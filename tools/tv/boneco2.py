# Boneco "robusto": jogador esculpido com campos de distancia (SDF) e renderizado por
# ray marching em numpy. Musculos sao elipsoides fundidos com uniao suave, a roupa e uma
# casca por cima do corpo (camisa, calcao) e a luz tem sombra suave e oclusao, para dar a
# cara de estudio de render 3D. Camera ortografica: serve para a TV e para a isometrica.
#   from boneco2 import Jogador, render
import math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ------------------------------------------------------------------ algebra

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])

def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])

def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])

def hexrgb(h):
    h = h.lstrip("#"); return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float) / 255

def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b + (a - b) * h - k * h * (1 - h)

def smax(a, b, k):
    return -smin(-a, -b, k)

# ------------------------------------------------------------------ primitivas (p: (N,3))

def elip(p, c, r, R=None):
    q = p - c
    if R is not None: q = q @ R          # R: local->mundo, entao mundo->local = q @ R
    r = np.asarray(r, float)
    k0 = np.linalg.norm(q / r, axis=1)
    k1 = np.linalg.norm(q / (r * r), axis=1)
    return k0 * (k0 - 1) / np.maximum(k1, 1e-9)

def cone(p, a, b, r1, r2):
    """cone arredondado (iq): esfera r1 em a, r2 em b"""
    a = np.asarray(a, float); b = np.asarray(b, float)
    ba = b - a; l2 = ba @ ba; rr = r1 - r2; a2 = l2 - rr * rr; il2 = 1.0 / l2
    pa = p - a
    y = pa @ ba
    z = y - l2
    xv = pa * l2 - np.outer(y, ba)
    x2 = np.einsum("ij,ij->i", xv, xv)
    y2 = y * y * l2; z2 = z * z * l2
    k = np.sign(rr) * rr * rr * x2
    d = np.where(np.sign(z) * a2 * z2 > k, np.sqrt(x2 + z2) * il2 - r2,
        np.where(np.sign(y) * a2 * y2 < k, np.sqrt(x2 + y2) * il2 - r1,
                 (np.sqrt(np.maximum(x2 * a2 * il2, 0)) + y * rr) * il2 - r1))
    return d

def caixa(p, c, b, r, R=None):
    q = p - c
    if R is not None: q = q @ R
    q = np.abs(q) - (np.asarray(b, float) - r)
    return np.linalg.norm(np.maximum(q, 0), axis=1) + np.minimum(np.max(q, axis=1), 0) - r

def plano(p, c, n):
    n = np.asarray(n, float); n = n / np.linalg.norm(n)
    return (p - c) @ n

# ------------------------------------------------------------------ jogador

PELE, CABELO, CAMISA, CALCAO, MEIA, CHUTEIRA, SOLA, GOLA, OLHO = range(9)

def pose(fase=0.0, correndo=False):
    if not correndo:
        return dict(quadril=(0.06, -0.04), joelho=(0.08, 0.1), ombro=(0.08, -0.06), cotovelo=(0.25, 0.3), tronco=0.03, pulo=0.0, abre=0.22)
    s = math.sin(fase)
    return dict(quadril=(0.62 * s, -0.62 * s),
                joelho=(0.3 + 0.95 * max(0.0, -math.sin(fase - 0.6)), 0.3 + 0.95 * max(0.0, math.sin(fase - 0.6))),
                ombro=(-0.75 * s, 0.75 * s), cotovelo=(1.35, 1.35), tronco=0.2, pulo=0.035 * abs(math.cos(fase)), abre=0.12)

class Jogador:
    """esqueleto posado + uniforme; sdf(p) e material(p) em coordenadas do boneco (y para cima, rosto para +z)"""

    def __init__(self, uni, p=None, corte="curto"):
        self.u = uni
        self.corte = getattr(uni, "corte", corte)
        p = p or pose()
        coxa, canela = 0.43, 0.43
        self.yq = yq = 0.075 + coxa + canela + p["pulo"]
        T = self.T = rot_x(-p["tronco"])
        self.pelve = np.array([0, yq, 0])
        self.peito = self.pelve + T @ np.array([0, 0.40, 0.0])
        self.pescoco = self.pelve + T @ np.array([0, 0.58, 0.0])
        self.ch = 1.13
        self.cabeca = self.pescoco + np.array([0, 0.16 * self.ch, 0.025])
        self.pernas = []
        for lado, sx in ((0, -1), (1, 1)):
            q = self.pelve + T @ np.array([sx * 0.1, -0.03, 0])
            Rq = rot_z(-sx * 0.03) @ rot_x(-p["quadril"][lado])
            joelho = q + Rq @ np.array([0, -coxa, 0])
            Rj = rot_x(-p["quadril"][lado] + p["joelho"][lado])
            tornozelo = joelho + Rj @ np.array([0, -canela, 0])
            Rp = rot_x(-p["quadril"][lado] + p["joelho"][lado] * 0.7)
            self.pernas.append((q, joelho, tornozelo, Rq, Rj, Rp))
        self.bracos = []
        for lado, sx in ((0, -1), (1, 1)):
            o = self.peito + T @ np.array([sx * 0.235, 0.05, -0.01])
            Ro = rot_z(sx * p["abre"] * 0.6) @ rot_x(-p["ombro"][lado])
            cot = o + Ro @ np.array([0, -0.29, 0])
            Rc = rot_z(sx * p["abre"] * 0.25) @ rot_x(-p["ombro"][lado] - p["cotovelo"][lado])
            pulso = cot + Rc @ np.array([0, -0.255, 0])
            self.bracos.append((o, cot, pulso, Ro, Rc, sx))
        # caixa envolvente
        self.min = np.array([-0.62, -0.02, -0.6]); self.max = np.array([0.62, yq + 0.95, 0.6])

    # ---- partes
    def tronco(self, p):
        T = self.T
        pv, pc = self.pelve, self.peito
        peito = elip(p, pc + T @ [0, -0.01, 0.015], (0.215, 0.165, 0.14), T)
        barriga = elip(p, pv + T @ [0, 0.2, 0.01], (0.165, 0.17, 0.115), T)
        dorsal = elip(p, pc + T @ [0, -0.07, -0.03], (0.2, 0.14, 0.12), T)
        quadril = caixa(p, pv + T @ [0, 0.02, 0], (0.165, 0.1, 0.11), 0.08, T)
        d = smin(smin(peito, barriga, 0.08), smin(dorsal, quadril, 0.08), 0.06)
        trap = smin(cone(p, self.peito + T @ [-0.15, 0.06, -0.02], self.pescoco + T @ [0, -0.01, -0.02], 0.07, 0.06),
                    cone(p, self.peito + T @ [0.15, 0.06, -0.02], self.pescoco + T @ [0, -0.01, -0.02], 0.07, 0.06), 0.04)
        pesc = cone(p, self.pescoco - T @ [0, 0.06, 0], self.cabeca - [0, 0.08 * self.ch, 0.02], 0.068, 0.06)
        sem = smin(d, trap, 0.06)
        self._sem_pescoco = sem
        return smin(sem, pesc, 0.05)

    def braco(self, p, i):
        o, cot, pulso, Ro, Rc, sx = self.bracos[i]
        delt = elip(p, o + Ro @ [-sx * 0.018, -0.05, 0], (0.066, 0.09, 0.076), Ro)
        sup = cone(p, o, cot, 0.066, 0.052)
        bic = elip(p, o + Ro @ [0, -0.15, 0.02], (0.062, 0.095, 0.064), Ro)
        ant = cone(p, cot, pulso, 0.05, 0.036)
        musc = elip(p, cot + Rc @ [0, -0.07, 0.005], (0.052, 0.085, 0.05), Rc)
        mao = caixa(p, pulso + Rc @ [0, -0.065, 0.008], (0.03, 0.06, 0.045), 0.026, Rc)
        ded = caixa(p, pulso + Rc @ [0, -0.125, 0.018], (0.026, 0.04, 0.04), 0.022, Rc @ rot_x(-0.35))
        pol = cone(p, pulso + Rc @ [-sx * 0.012, -0.03, 0.04], pulso + Rc @ [-sx * 0.01, -0.085, 0.06], 0.017, 0.014)
        d = smin(smin(delt, sup, 0.04), bic, 0.03)
        self._sup[i] = d
        d = smin(d, smin(ant, musc, 0.03), 0.025)
        dm = smin(smin(mao, ded, 0.02), pol, 0.012)
        return smin(d, dm, 0.02), dm

    def perna(self, p, i):
        q, joelho, tornozelo, Rq, Rj, Rp = self.pernas[i]
        coxa = cone(p, q, joelho, 0.112, 0.07)
        quad = elip(p, q + Rq @ [0, -0.18, 0.025], (0.1, 0.18, 0.095), Rq)
        joe = elip(p, joelho + Rj @ [0, 0, 0.02], (0.055, 0.06, 0.05), Rj)
        can = cone(p, joelho, tornozelo, 0.068, 0.048)
        pant = elip(p, joelho + Rj @ [0, -0.13, -0.025], (0.078, 0.125, 0.074), Rj)
        alto = smin(smin(coxa, quad, 0.05), joe, 0.04)
        baixo = smin(can, pant, 0.05)
        return alto, baixo

    def bota(self, p, i):
        q, joelho, tornozelo, Rq, Rj, Rp = self.pernas[i]
        c = tornozelo + Rp @ np.array([0, -0.035, 0.045])
        cano = cone(p, tornozelo + Rp @ [0, 0.02, 0], tornozelo + Rp @ [0, -0.03, 0.01], 0.05, 0.055)
        corpo = caixa(p, c, (0.068, 0.05, 0.14), 0.045, Rp)
        bico = elip(p, c + Rp @ [0, -0.005, 0.095], (0.068, 0.046, 0.078), Rp)
        d = smin(smin(corpo, bico, 0.03), cano, 0.03)
        sola = caixa(p, c + Rp @ [0, -0.047, 0.01], (0.072, 0.014, 0.162), 0.01, Rp)
        return min_(d, sola), sola

    def cabeca_(self, p):
        c = self.cabeca
        cra = elip(p, c + [0, 0.015, -0.008], (0.1, 0.11, 0.11))
        mand = elip(p, c + [0, -0.055, 0.025], (0.082, 0.07, 0.085))
        d = smin(cra, mand, 0.05)
        nariz = elip(p, c + [0, -0.012, 0.107], (0.016, 0.03, 0.022))
        orelhas = np.minimum(elip(p, c + [-0.1, -0.005, -0.005], (0.018, 0.032, 0.024)), elip(p, c + [0.1, -0.005, -0.005], (0.018, 0.032, 0.024)))
        sob = np.minimum(caixa(p, c + [-0.04, 0.056, 0.094], (0.03, 0.0075, 0.01), 0.006, rot_z(0.06)),
                         caixa(p, c + [0.04, 0.056, 0.094], (0.03, 0.0075, 0.01), 0.006, rot_z(-0.06)))
        return smin(smin(d, nariz, 0.012), orelhas, 0.01), sob

    def cabelo(self, p):
        c = self.cabeca
        rosto = lambda d, y=0.045, z=0.072: smax(d, plano(p, c + [0, y, z], [0, -0.4, 1]), 0.015)
        nuca = lambda d, y=-0.02: smax(d, plano(p, c + [0, y, 0], [0, -1, 0.45]), 0.015)
        if self.corte == "raspado":
            return nuca(rosto(elip(p, c + [0, 0.018, -0.01], (0.105, 0.117, 0.116)), 0.05, 0.065), 0.0)
        if self.corte == "black":
            return nuca(rosto(elip(p, c + [0, 0.065, -0.025], (0.142, 0.13, 0.148)), 0.05, 0.085), -0.03)
        d = nuca(rosto(elip(p, c + [0, 0.028, -0.01], (0.114, 0.122, 0.126))))
        for x, r in ((-0.05, 0.25), (0.0, 0.0), (0.05, -0.25)):
            d = smin(d, caixa(p, c + [x, 0.112, 0.035], (0.042, 0.028, 0.06), 0.022, rot_x(-0.3) @ rot_z(r)), 0.02)
        if self.corte == "topete":
            d = smin(d, caixa(p, c + [0.0, 0.14, 0.075], (0.05, 0.035, 0.05), 0.028, rot_x(0.5)), 0.03)
        return d

    # ---- roupa
    def camisa(self, p, tronco, bracos_sup):
        casca = tronco - 0.013
        for d in bracos_sup: casca = smin(casca, d - 0.024, 0.03)
        T = self.T
        bainha = plano(p, self.pelve + T @ [0, -0.045, 0], T @ [0, -1, 0])
        gola = elip(p, self.pescoco + T @ [0, 0.035, 0.035], (0.078, 0.07, 0.09), T)
        cintura = caixa(p, self.pelve + T @ [0, 0.02, 0], (0.184, 0.07, 0.132), 0.06, T)
        casca = smin(casca, cintura, 0.05)
        casca = smax(casca, bainha, 0.01)
        casca = smax(casca, -gola, 0.01)
        return casca

    def mangas(self, p):
        out = []
        for i, (o, cot, pulso, Ro, Rc, sx) in enumerate(self.bracos):
            m = self._sup[i] - 0.016
            corte = plano(p, o + Ro @ [0, -0.16, 0], Ro @ [0, -1, 0])
            out.append(smax(m, corte, 0.01))
        return out

    def calcao(self, p):
        T = self.T
        c = caixa(p, self.pelve + T @ [0, -0.02, 0], (0.178, 0.1, 0.128), 0.075, T)
        for q, joelho, tornozelo, Rq, Rj, Rp in self.pernas:
            fim = q + Rq @ np.array([0, -0.21, 0])
            perna = cone(p, q + Rq @ [0, 0.03, 0], fim, 0.125, 0.122)
            perna = smax(perna, plano(p, fim, Rq @ [0, -1, 0.15]), 0.008)
            c = smin(c, perna, 0.04)
        topo = plano(p, self.pelve + T @ [0, 0.0, 0], T @ [0, 1, 0])
        return smax(c, topo, 0.01)

    # ---- sdf completa
    def partes(self, p):
        tr = self.tronco(p)
        self._sup = [None, None]
        b0, m0 = self.braco(p, 0); b1, m1 = self.braco(p, 1)
        a0, c0 = self.perna(p, 0); a1, c1 = self.perna(p, 1)
        bo0, s0 = self.bota(p, 0); bo1, s1 = self.bota(p, 1)
        ch = self.ch; pc = self.cabeca + (p - self.cabeca) / ch
        cab, sob = self.cabeca_(pc); cab = cab * ch; sob = sob * ch
        corpo = smin(tr, smin(b0, b1, 0.0001), 0.04)
        corpo = smin(corpo, smin(a0, a1, 0.0001), 0.06)
        corpo = np.minimum(corpo, np.minimum(c0, c1))
        corpo = smin(corpo, smin(c0, a0, 0.04), 0.0001)
        corpo = smin(corpo, smin(c1, a1, 0.04), 0.0001)
        corpo = smin(corpo, cab, 0.04)
        mangas = self.mangas(p)
        cam = self.camisa(p, self._sem_pescoco, [])
        cam = smin(cam, np.minimum(mangas[0], mangas[1]), 0.04)
        cal = self.calcao(p)
        cabelo = self.cabelo(pc) * ch
        botas = np.minimum(bo0, bo1)
        return dict(corpo=corpo, cam=cam, cal=cal, cab=cabelo, botas=botas, sola=np.minimum(s0, s1),
                    meia=np.minimum(c0, c1), sob=sob)

    def sdf(self, p):
        d = self.partes(p)
        return np.minimum.reduce([d["corpo"], d["cam"], d["cal"], d["cab"], d["botas"], d["sob"]])

    def material(self, p):
        d = self.partes(p)
        m = np.full(len(p), PELE)
        # ordem: quem estiver mais perto da superficie ganha (roupa por cima do corpo)
        base = d["corpo"]
        m[(d["meia"] <= base + 0.004) & (p[:, 1] < self.yq - 0.43 - 0.05)] = MEIA
        for chave, mat in (("cal", CALCAO), ("cam", CAMISA), ("botas", CHUTEIRA), ("sola", SOLA), ("cab", CABELO), ("sob", CABELO)):
            atual = np.minimum(base, 9)
            m[d[chave] <= atual + 0.003] = mat
            base = np.minimum(base, d[chave])
        return m

def min_(a, b):
    return np.minimum(a, b)

# ------------------------------------------------------------------ uniforme

class Uniforme:
    def __init__(self, camisa, detalhe, calcao, meia, padrao="liso", pele="#b97a52", cabelo="#1e1611",
                 chuteira="#161616", sola="#2a2a2a", corte="curto", numero=None):
        self.camisa, self.detalhe = hexrgb(camisa), hexrgb(detalhe)
        self.calcao, self.meia = hexrgb(calcao), hexrgb(meia)
        self.padrao, self.pele, self.cabelo = padrao, hexrgb(pele), hexrgb(cabelo)
        self.chuteira, self.sola = hexrgb(chuteira), hexrgb(sola)
        self.corte = corte
        self.numero = numero
        self._mascara = None

    def mascara(self):
        if self._mascara is None and self.numero is not None:
            im = Image.new("L", (64, 64), 0); d = ImageDraw.Draw(im)
            try:
                f = ImageFont.truetype(os.path.join(os.path.dirname(__file__), "..", "..", "mod", "assets", "mod", "fontes", "SairaExtraCondensed-ExtraBold.ttf"), 62)
            except Exception:
                f = ImageFont.load_default()
            t = str(self.numero); bb = d.textbbox((0, 0), t, font=f)
            d.text(((64 - (bb[2] - bb[0])) / 2 - bb[0], (64 - (bb[3] - bb[1])) / 2 - bb[1]), t, fill=255, font=f)
            self._mascara = np.array(im, float) / 255
        return self._mascara

    def cores(self, j, mat, p):
        c = np.zeros((len(p), 3))
        for k, v in ((PELE, self.pele), (CABELO, self.cabelo), (CALCAO, self.calcao), (MEIA, self.meia),
                     (CHUTEIRA, self.chuteira), (SOLA, self.sola)):
            c[mat == k] = v
        cam = mat == CAMISA
        if cam.any():
            q = (p[cam] - j.pelve) @ j.T          # coordenadas do tronco
            x, y = q[:, 0], q[:, 1]
            if self.padrao == "listras":
                d = (np.floor((x + 0.6) / 0.08) % 2) == 1
            elif self.padrao == "faixas":
                d = (np.floor((y + 0.6) / 0.095) % 2) == 1
            elif self.padrao == "faixa":
                d = np.abs((y - 0.3) + x * 0.95) < 0.065
            else:
                d = np.zeros(len(x), bool)
            cc = np.where(d[:, None], self.detalhe, self.camisa)
            # gola e barra das mangas no detalhe
            gola = np.linalg.norm(p[cam] - j.pescoco, axis=1) < 0.105
            cc[gola] = self.detalhe if self.padrao == "liso" else self.camisa * 0.0 + self.detalhe
            mk = self.mascara()
            if mk is not None:
                u_ = (0.12 - x) / 0.24; v_ = (0.5 - y) / 0.25
                den = (q[:, 2] < -0.05) & (u_ >= 0) & (u_ < 1) & (v_ >= 0) & (v_ < 1)
                if den.any():
                    if self.padrao != "liso": cc[den] = self.camisa
                    iu = np.clip((u_[den] * 64).astype(int), 0, 63); iv = np.clip((v_[den] * 64).astype(int), 0, 63)
                    on = mk[iv, iu] > 0.5
                    claro = self.camisa.mean() > 0.55
                    cc[np.where(den)[0][on]] = [0.08, 0.08, 0.08] if claro else [0.97, 0.97, 0.95]
            c[cam] = cc
        # meia: faixa no alto em detalhe
        me = mat == MEIA
        if me.any():
            y = p[me, 1]
            topo = j.yq - 0.43 - 0.05
            c[me] = np.where(((y > topo - 0.05) & (y < topo - 0.02))[:, None], self.detalhe, self.meia)
        # rosto: olhos e boca pintados
        pe = mat == PELE
        if pe.any():
            q = (p[pe] - j.cabeca) / j.ch
            frente = q[:, 2] > 0.06
            for sx in (-1, 1):
                olho = frente & (((q[:, 0] - sx * 0.037) / 0.013) ** 2 + ((q[:, 1] - 0.013) / 0.019) ** 2 < 1)
                c[np.where(pe)[0][olho]] = [0.06, 0.05, 0.05]
            boca = frente & (np.abs(q[:, 1] + 0.062) < 0.004) & (np.abs(q[:, 0]) < 0.022)
            c[np.where(pe)[0][boca]] = self.pele * 0.55
        return c

# ------------------------------------------------------------------ render

LUZ = np.array([-0.5, 0.8, 0.6]); LUZ /= np.linalg.norm(LUZ)
CONTRA = np.array([0.6, 0.35, -0.7]); CONTRA /= np.linalg.norm(CONTRA)

def render(j, giro, inclinacao, altura_px, aa=2, sombra=True, contorno=0.0):
    """retorna RGBA; o pe do jogador fica em (largura/2, 86% da altura)"""
    M = rot_x(inclinacao) @ rot_y(giro)                    # boneco -> camera
    escala = altura_px * aa / 1.95
    W = int(altura_px * 0.95 * aa); H = int(altura_px * 1.22 * aa)
    xs = (np.arange(W) + 0.5 - W / 2) / escala
    ys = (H * 0.86 - (np.arange(H) + 0.5)) / escala
    X, Y = np.meshgrid(xs, ys)
    oc = np.stack([X.ravel(), Y.ravel(), np.full(X.size, 3.0)], 1)
    dc = np.array([0, 0, -1.0])
    o = oc @ M; d = dc @ M                                   # camera -> boneco
    # caixa envolvente
    inv = 1.0 / np.where(np.abs(d) < 1e-9, 1e-9, d)
    t0 = (j.min - o) * inv; t1 = (j.max - o) * inv
    tmin = np.max(np.minimum(t0, t1), 1); tmax = np.min(np.maximum(t0, t1), 1)
    vivo = np.where(tmax > np.maximum(tmin, 0))[0]
    t = np.maximum(tmin[vivo], 0)
    acerto = np.zeros(len(o), bool); tt = np.zeros(len(o)); dmin = np.full(len(o), 9.0)
    eps = 0.6 / escala
    ativos = vivo.copy()
    for it in range(110):
        if len(ativos) == 0: break
        p = o[ativos] + np.outer(t, d)
        dist = j.sdf(p)
        dmin[ativos] = np.minimum(dmin[ativos], dist)
        hit = dist < eps
        acerto[ativos[hit]] = True; tt[ativos[hit]] = t[hit]
        t = t + np.maximum(dist * 0.85, eps * 0.5)
        seg = (~hit) & (t < tmax[ativos])
        ativos = ativos[seg]; t = t[seg]
    img = np.zeros((len(o), 4))
    idx = np.where(acerto)[0]
    if len(idx):
        p = o[idx] + np.outer(tt[idx], d)
        h = 0.0015
        n = np.stack([j.sdf(p + [h, 0, 0]) - j.sdf(p - [h, 0, 0]),
                      j.sdf(p + [0, h, 0]) - j.sdf(p - [0, h, 0]),
                      j.sdf(p + [0, 0, h]) - j.sdf(p - [0, 0, h])], 1)
        n /= np.linalg.norm(n, axis=1)[:, None] + 1e-12
        mat = j.material(p)
        base = j.u.cores(j, mat, p)
        # luz no mundo: gira a luz para o espaco do boneco
        Lw = rot_y(giro).T @ LUZ; Cw = rot_y(giro).T @ CONTRA
        dif = np.clip(n @ Lw * 0.85 + 0.15, 0, 1)            # luz "enrolada", mais macia
        # sombra suave
        sh = np.ones(len(p)); ts = np.full(len(p), 0.02)
        for _ in range(28):
            q = p + n * 0.004 + np.outer(ts, Lw)
            ds = j.sdf(q)
            sh = np.minimum(sh, np.clip(10 * ds / ts, 0, 1))
            ts += np.clip(ds, 0.01, 0.08)
        sh = np.clip(sh, 0, 1) ** 1.5
        # oclusao
        ao = np.ones(len(p))
        for k, dk in enumerate((0.015, 0.035, 0.06, 0.1)):
            ao -= (dk - j.sdf(p + n * dk)) * (1.6 / (k + 1))
        ao = np.clip(ao, 0.35, 1)
        ceu = 0.55 + 0.45 * n[:, 1]
        rim = np.clip(n @ Cw, 0, 1) ** 2.5
        luz = (0.42 * ceu * ao + 0.95 * dif * sh * (0.6 + 0.4 * ao))[:, None]
        cor = base * luz + rim[:, None] * 0.22 * np.array([1.0, 0.95, 0.85])
        # brilho leve no tecido sintetico e na chuteira
        hv = Lw + np.array([0, 0, 1.0]) @ M; hv /= np.linalg.norm(hv)
        spec = np.clip(n @ hv, 0, 1) ** 40
        brilha = np.isin(mat, [CAMISA, CALCAO, CHUTEIRA]) * 0.18 + (mat == PELE) * 0.06
        cor += (spec * brilha)[:, None] * sh[:, None]
        img[idx, :3] = np.clip(cor, 0, 1) ** (1 / 1.1)
        img[idx, 3] = 1
    # borda suave pelo dmin dos raios que passaram perto
    borda = (~acerto) & (dmin < eps * 2.5)
    img[borda, 3] = np.clip(1 - (dmin[borda] - eps) / (eps * 1.5), 0, 1) * 0.0
    img = img.reshape(H, W, 4)
    if contorno > 0:
        a = img[..., 3] > 0
        k = max(1, int(contorno * aa))
        borda = np.zeros_like(a)
        for dy in range(-k, k + 1):
            for dx in range(-k, k + 1):
                if dx * dx + dy * dy <= k * k:
                    borda |= np.roll(np.roll(a, dy, 0), dx, 1)
        borda &= ~a
        img[borda] = [0.06, 0.05, 0.05, 1]
    im = Image.fromarray((img * 255).astype(np.uint8), "RGBA")
    if sombra:
        s = Image.new("RGBA", im.size, (0, 0, 0, 0)); ds = ImageDraw.Draw(s)
        cx, cy = W / 2, H * 0.86
        rx = 0.42 * escala; ry = rx * max(0.15, math.sin(inclinacao)) + 2
        ds.ellipse([cx - rx + 0.12 * escala, cy - ry, cx + rx + 0.12 * escala, cy + ry], fill=(0, 0, 0, 120))
        s = s.filter(ImageFilter.GaussianBlur(5 * aa)); s.alpha_composite(im); im = s
    return im.resize((W // aa, H // aa), Image.LANCZOS)
