# Renderizador 3D minimo (numpy) para os bonecos da camera de TV.
# Monta um jogador com primitivas (elipsoides e capsulas), posa o esqueleto (parado ou
# correndo) e rasteriza com z-buffer e supersampling. Tres estilos de acabamento:
#   lowpoly  - poucas faces, sombreamento chapado (facetado)
#   toon     - liso, luz em 3 faixas e contorno escuro, proporcoes mais "cabecudas"
#   realista - liso, luz suave com oclusao e brilho, proporcoes reais
# Uso: python3 boneco.py pasta_de_saida   (gera os prints de comparacao)
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# ------------------------------------------------------------------ geometria

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])

def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])

def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])

class Malha:
    def __init__(self):
        self.v, self.n, self.f, self.m = [], [], [], []
        self.cont = 0

    def junta(self, v, n, f, mat):
        self.v.append(v); self.n.append(n); self.f.append(f + self.cont)
        self.m.append(np.full(len(f), mat))
        self.cont += len(v)

    def arrays(self):
        return np.vstack(self.v), np.vstack(self.n), np.vstack(self.f), np.concatenate(self.m)

def elipsoide(malha, c, r, mat, nu=16, nv=12, R=None, vmax=math.pi):
    R = np.eye(3) if R is None else R
    us = np.linspace(0, 2 * math.pi, nu + 1)[:-1]
    vs = np.linspace(0, vmax, nv + 1)
    vert, nor = [], []
    for v in vs:
        for u in us:
            d = np.array([math.sin(v) * math.cos(u), math.cos(v), math.sin(v) * math.sin(u)])
            p = d * r
            nn = d / np.array(r)
            vert.append(R @ p + c); nor.append(R @ (nn / np.linalg.norm(nn)))
    f = []
    for j in range(nv):
        for i in range(nu):
            a = j * nu + i; b = j * nu + (i + 1) % nu
            f.append([a, a + nu, b]); f.append([b, a + nu, b + nu])
    malha.junta(np.array(vert), np.array(nor), np.array(f), mat)

def capsula(malha, p0, p1, r0, r1, mat, nu=14, nv=6, mat2=None, corte=1.0, achata=1.0):
    """tronco de cone de p0 (raio r0) a p1 (r1) com tampas esfericas; mat2 a partir de 'corte'"""
    p0, p1 = np.array(p0, float), np.array(p1, float)
    eixo = p1 - p0; L = np.linalg.norm(eixo); z = eixo / L
    x = np.cross(z, [0, 0, 1.0])
    if np.linalg.norm(x) < 1e-6: x = np.cross(z, [1.0, 0, 0])
    x /= np.linalg.norm(x); y = np.cross(z, x)
    us = np.linspace(0, 2 * math.pi, nu + 1)[:-1]
    aneis = []
    # tampa de baixo, corpo, tampa de cima
    for k in range(3, 0, -1):
        a = math.pi / 2 * k / 3
        aneis.append((-r0 * math.sin(a), r0 * math.cos(a), -math.sin(a), math.cos(a)))
    for k in range(nv + 1):
        t = k / nv
        aneis.append((t * L, r0 + (r1 - r0) * t, 0.0, 1.0))
    for k in range(1, 4):
        a = math.pi / 2 * k / 3
        aneis.append((L + r1 * math.sin(a), r1 * math.cos(a), math.sin(a), math.cos(a)))
    vert, nor = [], []
    for (h, r, nz, nr) in aneis:
        for u in us:
            d = x * math.cos(u) + y * math.sin(u) * achata
            vert.append(p0 + z * h + d * r)
            nn = (x * math.cos(u) + y * math.sin(u) / achata) * nr + z * nz
            nor.append(nn / np.linalg.norm(nn))
    nr_ = len(aneis)
    f, mats = [], []
    for j in range(nr_ - 1):
        hmed = (aneis[j][0] + aneis[j + 1][0]) / 2
        for i in range(nu):
            a = j * nu + i; b = j * nu + (i + 1) % nu
            f.append([a, a + nu, b]); f.append([b, a + nu, b + nu])
            mm = mat2 if (mat2 is not None and hmed > corte * L) else mat
            mats += [mm, mm]
    # junta com material por face
    v, n, ff = np.array(vert), np.array(nor), np.array(f)
    malha.v.append(v); malha.n.append(n); malha.f.append(ff + malha.cont); malha.m.append(np.array(mats))
    malha.cont += len(v)

# ------------------------------------------------------------------ jogador

PELE, CABELO, CAMISA, CALCAO, MEIA, CHUTEIRA, GOLA, OLHO = range(8)

ESTILOS = {
    "lowpoly":  dict(nu=7, nv=2, sx=8, sy=6, cabeca=1.05, perna=1.0, ombro=1.0, liso=False),
    "toon":     dict(nu=18, nv=6, sx=20, sy=14, cabeca=1.45, perna=0.86, ombro=1.08, liso=True),
    "realista": dict(nu=20, nv=8, sx=22, sy=16, cabeca=1.0, perna=1.0, ombro=1.0, liso=True),
}

def pose(fase, correndo):
    if not correndo:
        return dict(quadril=(0.05, -0.05), joelho=(0.1, 0.1), ombro=(0.05, -0.05), cotovelo=(0.35, 0.35), tronco=0.02, pulo=0.0)
    s = math.sin(fase)
    q = (0.65 * s, -0.65 * s)
    j = (0.3 + 0.85 * max(0.0, -math.sin(fase - 0.6)), 0.3 + 0.85 * max(0.0, math.sin(fase - 0.6)))
    o = (-0.7 * s, 0.7 * s)
    return dict(quadril=q, joelho=j, ombro=o, cotovelo=(1.3, 1.3), tronco=0.18, pulo=0.04 * abs(math.cos(fase)))

def jogador(estilo, p, corte="curto"):
    e = ESTILOS[estilo]
    nu, nv = e["nu"], e["nv"]
    m = Malha()
    ch = e["cabeca"]; pe = e["perna"]
    coxa, canela = 0.44 * pe, 0.44 * pe
    yq = 0.06 + coxa + canela + 0.04 + p["pulo"]   # altura do quadril
    T = rot_x(-p["tronco"])                         # inclinacao do tronco para frente
    base = np.array([0, yq, 0])
    pelve = base
    peito = base + T @ np.array([0, 0.42, 0])
    pescoco = base + T @ np.array([0, 0.56, 0])
    cabeca = pescoco + np.array([0, 0.13 * ch + 0.02, 0.01])
    # tronco
    capsula(m, pelve + T @ [0, 0.06, 0], peito, 0.17, 0.205 * e["ombro"], CAMISA, nu, nv, achata=0.62)
    capsula(m, pelve - [0, 0.04, 0], pelve + T @ [0, 0.1, 0], 0.175, 0.172, CALCAO, nu, 2, achata=0.7)
    capsula(m, peito, pescoco, 0.06, 0.055, PELE, max(6, nu // 2), 2)
    elipsoide(m, pescoco - T @ [0, 0.015, 0], (0.075, 0.03, 0.065), GOLA, max(6, nu // 2), max(3, nv), R=T)
    # cabeca e cabelo
    elipsoide(m, cabeca, (0.105 * ch, 0.125 * ch, 0.115 * ch), PELE, e["sx"], e["sy"])
    if corte == "black":
        elipsoide(m, cabeca + np.array([0, 0.045 * ch, -0.02 * ch]), (0.135 * ch, 0.125 * ch, 0.14 * ch), CABELO, e["sx"], max(3, e["sy"] // 2 + 1),
                  R=rot_x(-0.5), vmax=0.62 * math.pi)
    elif corte == "raspado":
        elipsoide(m, cabeca + np.array([0, 0.004 * ch, -0.004 * ch]), (0.108 * ch, 0.128 * ch, 0.118 * ch), CABELO, e["sx"], max(3, e["sy"] // 2),
                  R=rot_x(-0.62), vmax=0.45 * math.pi)
    elif corte == "topete":
        elipsoide(m, cabeca + np.array([0, 0.02 * ch, -0.008 * ch]), (0.114 * ch, 0.135 * ch, 0.124 * ch), CABELO, e["sx"], max(3, e["sy"] // 2),
                  R=rot_x(-0.55), vmax=0.5 * math.pi)
        elipsoide(m, cabeca + np.array([0, 0.11 * ch, 0.05 * ch]), (0.07 * ch, 0.04 * ch, 0.06 * ch), CABELO, max(6, e["sx"] // 2), max(3, e["sy"] // 2), R=rot_x(0.4))
    else:
        elipsoide(m, cabeca + np.array([0, 0.016 * ch, -0.008 * ch]), (0.116 * ch, 0.136 * ch, 0.126 * ch), CABELO, e["sx"], max(3, e["sy"] // 2),
                  R=rot_x(-0.55), vmax=0.52 * math.pi)
    # rosto: nariz, orelhas e olhos (olhos maiores no toon)
    elipsoide(m, cabeca + np.array([0, -0.01 * ch, 0.108 * ch]), (0.018 * ch, 0.028 * ch, 0.022 * ch), PELE, 6, 4)
    for sx_ in (-1, 1):
        elipsoide(m, cabeca + np.array([sx_ * 0.103 * ch, -0.005 * ch, -0.005 * ch]), (0.018 * ch, 0.032 * ch, 0.022 * ch), PELE, 6, 4)
        ro = 0.017 if estilo == "toon" else 0.011
        elipsoide(m, cabeca + np.array([sx_ * 0.04 * ch, 0.018 * ch, 0.098 * ch]), (ro * ch, ro * 1.3 * ch, 0.008 * ch), OLHO, 6, 4)
    # pernas
    for lado, sx in ((0, -1), (1, 1)):
        q = pelve + np.array([sx * 0.095, -0.02, 0])
        Rq = rot_x(-p["quadril"][lado])
        joelho = q + Rq @ np.array([0, -coxa, 0])
        Rj = rot_x(-p["quadril"][lado] + p["joelho"][lado])
        tornozelo = joelho + Rj @ np.array([0, -canela, 0])
        capsula(m, q, joelho, 0.098, 0.068, CALCAO, nu, nv, mat2=PELE, corte=0.42)
        capsula(m, joelho, tornozelo, 0.066, 0.042, MEIA, nu, nv)
        Rp = rot_x(-p["quadril"][lado] + p["joelho"][lado] * 0.6)
        elipsoide(m, tornozelo + Rp @ np.array([0, -0.035, 0.07]), (0.05, 0.04, 0.12), CHUTEIRA, max(6, nu // 2 + 2), max(3, nv), R=Rp)
    # bracos
    for lado, sx in ((0, -1), (1, 1)):
        o = peito + np.array([sx * 0.225 * e["ombro"], -0.035, 0])
        Ro = rot_z(sx * 0.07) @ rot_x(-p["ombro"][lado])
        cot = o + Ro @ np.array([0, -0.29, 0])
        Rc = rot_z(sx * 0.07) @ rot_x(-p["ombro"][lado] - p["cotovelo"][lado])
        pulso = cot + Rc @ np.array([0, -0.26, 0])
        capsula(m, o, cot, 0.07, 0.05, CAMISA, nu, nv, mat2=PELE, corte=0.5)
        capsula(m, cot, pulso, 0.045, 0.036, PELE, nu, nv)
        elipsoide(m, pulso + Rc @ np.array([0, -0.04, 0]), (0.04, 0.05, 0.03), PELE, max(6, nu // 2), max(3, nv), R=Rc)
    return m.arrays(), yq

# ------------------------------------------------------------------ uniformes

def hexrgb(h):
    h = h.lstrip("#"); return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float) / 255

class Uniforme:
    def __init__(self, camisa, detalhe, calcao, meia, padrao="liso", pele="#c98e64", cabelo="#1c1410", chuteira="#151515", corte="curto"):
        self.corte = corte
        self.camisa, self.detalhe = hexrgb(camisa), hexrgb(detalhe)
        self.calcao, self.meia = hexrgb(calcao), hexrgb(meia)
        self.padrao = padrao
        self.pele, self.cabelo, self.chuteira = hexrgb(pele), hexrgb(cabelo), hexrgb(chuteira)
        self.numero = None
        self.yq = 1.0

    def com_numero(self, n, cor=None):
        self.numero = n
        claro = self.camisa.mean() > 0.55
        self.cor_numero = hexrgb(cor) if cor else (np.array([0.08, 0.08, 0.08]) if claro else np.array([0.97, 0.97, 0.95]))
        im = Image.new("L", (64, 64), 0)
        d = ImageDraw.Draw(im)
        try:
            fonte = ImageFont.truetype(os.path.join(os.path.dirname(__file__), "..", "..", "mod", "assets", "mod", "fontes", "SairaExtraCondensed-ExtraBold.ttf"), 62)
        except Exception:
            fonte = ImageFont.load_default()
        t = str(n)
        bb = d.textbbox((0, 0), t, font=fonte)
        d.text(((64 - (bb[2] - bb[0])) / 2 - bb[0], (64 - (bb[3] - bb[1])) / 2 - bb[1]), t, fill=255, font=fonte)
        self.mascara = np.array(im, float) / 255
        return self

    def cor(self, mat, pos):
        """cor base por pixel; pos = posicao no corpo (antes da pose da camera)"""
        n = len(mat)
        c = np.zeros((n, 3))
        for k, v in ((PELE, self.pele), (CABELO, self.cabelo), (CALCAO, self.calcao), (MEIA, self.meia), (CHUTEIRA, self.chuteira), (GOLA, self.detalhe), (OLHO, np.array([0.06, 0.05, 0.05]))):
            c[mat == k] = v
        cam = mat == CAMISA
        if cam.any():
            x, y = pos[cam, 0], pos[cam, 1]
            if self.padrao == "listras":
                d = (np.floor((x + 0.5) / 0.075) % 2) == 1
            elif self.padrao == "faixas":
                d = (np.floor((y - 0.9) / 0.085) % 2) == 1
            elif self.padrao == "faixa":   # faixa diagonal
                d = np.abs((y - 1.25) + x * 0.9) < 0.07
            else:
                d = np.zeros(cam.sum(), bool)
            cc = np.where(d[:, None], self.detalhe, self.camisa)
            # numero nas costas
            if self.numero is not None:
                z = pos[cam, 2]
                u_ = (0.12 - x) / 0.24; v_ = (self.yq + 0.5 - y) / 0.27
                dentro = (z < -0.02) & (u_ >= 0) & (u_ < 1) & (v_ >= 0) & (v_ < 1)
                if dentro.any() and self.padrao != "liso":
                    cc[np.where(dentro)[0]] = self.camisa     # placa lisa atras do numero
                if dentro.any():
                    mk = self.mascara
                    iu = np.clip((u_[dentro] * mk.shape[1]).astype(int), 0, mk.shape[1] - 1)
                    iv = np.clip((v_[dentro] * mk.shape[0]).astype(int), 0, mk.shape[0] - 1)
                    on = mk[iv, iu] > 0.5
                    idx = np.where(dentro)[0][on]
                    cc[idx] = self.cor_numero
            c[cam] = cc
        # meia com faixa no alto
        me = mat == MEIA
        if me.any():
            y = pos[me, 1]
            c[me] = np.where(((y > 0.36) & (y < 0.4))[:, None], self.detalhe, self.meia)
        return c

# ------------------------------------------------------------------ render

LUZ = np.array([-0.45, 0.85, 0.55]); LUZ /= np.linalg.norm(LUZ)
LUZ2 = np.array([0.7, 0.3, -0.6]); LUZ2 /= np.linalg.norm(LUZ2)     # contraluz dos refletores

def render(estilo, uni, giro, inclinacao, altura_px, fase=0.0, correndo=False, ss=3, sombra=True):
    (v, n, f, mats), yq = jogador(estilo, pose(fase, correndo), getattr(uni, "corte", "curto"))
    uni.yq = yq
    Rw = rot_y(giro)
    vw = v @ Rw.T; nw = n @ Rw.T                     # mundo (luz fixa no mundo)
    Rc = rot_x(inclinacao)                           # camera olhando de cima
    vc = vw @ Rc.T
    escala = altura_px * ss / 1.95
    W = int(altura_px * 0.9 * ss); H = int(altura_px * 1.25 * ss)
    sx = W / 2 + vc[:, 0] * escala
    sy = H * 0.86 - vc[:, 1] * escala
    sz = vc[:, 2]
    zbuf = np.full((H, W), -1e9)
    cor = np.zeros((H, W, 3)); alfa = np.zeros((H, W)); idm = np.full((H, W), -1)
    liso = ESTILOS[estilo]["liso"]
    for t, (a, b, c) in enumerate(f):
        xs = np.array([sx[a], sx[b], sx[c]]); ys = np.array([sy[a], sy[b], sy[c]])
        x0, x1 = max(int(xs.min()), 0), min(int(xs.max()) + 1, W - 1)
        y0, y1 = max(int(ys.min()), 0), min(int(ys.max()) + 1, H - 1)
        if x1 < x0 or y1 < y0: continue
        den = (ys[1] - ys[2]) * (xs[0] - xs[2]) + (xs[2] - xs[1]) * (ys[0] - ys[2])
        if abs(den) < 1e-9: continue
        X, Y = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        w0 = ((ys[1] - ys[2]) * (X - xs[2]) + (xs[2] - xs[1]) * (Y - ys[2])) / den
        w1 = ((ys[2] - ys[0]) * (X - xs[2]) + (xs[0] - xs[2]) * (Y - ys[2])) / den
        w2 = 1 - w0 - w1
        dentro = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
        if not dentro.any(): continue
        z = w0 * sz[a] + w1 * sz[b] + w2 * sz[c]
        zb = zbuf[y0:y1 + 1, x0:x1 + 1]
        ok = dentro & (z > zb)
        if not ok.any(): continue
        if liso:
            nn = w0[ok][:, None] * nw[a] + w1[ok][:, None] * nw[b] + w2[ok][:, None] * nw[c]
            nn /= np.linalg.norm(nn, axis=1)[:, None]
        else:
            fn = np.cross(vw[b] - vw[a], vw[c] - vw[a]); fn /= (np.linalg.norm(fn) + 1e-12)
            if fn @ (nw[a] + nw[b] + nw[c]) < 0: fn = -fn
            nn = np.repeat(fn[None, :], ok.sum(), axis=0)
        pos = w0[ok][:, None] * v[a] + w1[ok][:, None] * v[b] + w2[ok][:, None] * v[c]
        base = uni.cor(np.full(ok.sum(), mats[t]), pos)
        cl = luz(estilo, nn, pos, mats[t])
        sub = cor[y0:y1 + 1, x0:x1 + 1]
        sub[ok] = np.clip(base * cl[:, :1] + cl[:, 1:2] * np.array([1.0, 0.93, 0.78]), 0, 1)
        zb[ok] = z[ok]
        alfa[y0:y1 + 1, x0:x1 + 1][ok] = 1
        idm[y0:y1 + 1, x0:x1 + 1][ok] = mats[t]
    if estilo == "toon":
        cor, alfa = contorno(cor, alfa, idm, zbuf, ss)
    img = np.dstack([cor, alfa])
    im = Image.fromarray((img * 255).astype(np.uint8), "RGBA")
    if sombra:
        s = Image.new("RGBA", im.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(s)
        cx, cy = W / 2, H * 0.86
        rx, ry = 0.38 * escala, 0.38 * escala * math.sin(inclinacao) + 2
        d.ellipse([cx - rx + 0.25 * escala, cy - ry, cx + rx + 0.25 * escala, cy + ry], fill=(0, 0, 0, 110))
        s = s.filter(ImageFilter.GaussianBlur(4 * ss))
        s.alpha_composite(im); im = s
    return im.resize((W // ss, H // ss), Image.LANCZOS)

def luz(estilo, nn, pos, mat):
    d = np.clip(nn @ LUZ, 0, 1)
    rim = np.clip(nn @ LUZ2, 0, 1)
    if estilo == "toon":
        k = np.where(d > 0.6, 1.0, np.where(d > 0.2, 0.78, 0.55))
        r = np.where(rim > 0.55, 0.12, 0.0)
        return np.stack([k, r], 1)
    if estilo == "lowpoly":
        k = 0.42 + 0.68 * d
        return np.stack([k, 0.18 * rim ** 2], 1)
    # realista: luz de ceu (hemisferio) + difusa + brilho + oclusao perto do chao/axilas
    ceu = 0.5 + 0.5 * nn[:, 1]
    ao = np.clip(0.55 + 0.45 * np.clip(pos[:, 1] / 0.6, 0, 1), 0, 1)
    h = nn @ (LUZ + np.array([0, 0, 1.0])); h = np.clip(h / np.linalg.norm(LUZ + np.array([0, 0, 1.0])), 0, 1)
    brilho = (0.25 if mat in (CAMISA, CALCAO, CHUTEIRA) else 0.08) * h ** 24
    k = (0.28 * ceu + 0.8 * d) * ao
    return np.stack([k, brilho + 0.22 * rim ** 3], 1)

def contorno(cor, alfa, idm, zbuf, ss):
    a = alfa > 0
    borda = np.zeros_like(a)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        for k in range(1, ss + 1):
            sh = np.roll(np.roll(a, dy * k, 0), dx * k, 1)
            borda |= sh & ~a
    # dobras internas: salto grande de profundidade
    z = np.where(a, zbuf, -1e9)
    dz = np.maximum(np.abs(z - np.roll(z, 1, 0)), np.abs(z - np.roll(z, 1, 1)))
    interna = a & (dz > 0.06) & (dz < 1e8)
    cor = cor.copy(); al = alfa.copy()
    cor[borda] = [0.08, 0.07, 0.06]; al[borda] = 1
    cor[interna] = cor[interna] * 0.35
    return cor, al
