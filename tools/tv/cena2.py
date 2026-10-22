# Cenas com os bonecos robustos (boneco2.py) em duas cameras:
#   iso - ortografica, do alto e girada (campo em losango), como jogo isometrico
#   tv  - perspectiva, da lateral, do alto da arquibancada (como cena.py)
# Os bonecos sao desenhados uma vez por (time, direcao, passo da corrida) e reaproveitados,
# do mesmo jeito que o jogo faria no inicio de cada partida.
# Uso: python3 cena2.py pasta_de_saida [iso|tv|video-iso ...]
import math, os, random, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import boneco2 as b

W, H = 1280, 720
FONTES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "mod", "assets", "mod", "fontes")
UP = np.array([0, 1.0, 0])

class Camera:
    """orto=True: escala em px/m; orto=False: foco em px. alvo no centro da tela."""
    def __init__(self, alvo, az, el, dist=60.0, escala=30.0, foco=2350.0, orto=True):
        self.alvo = np.asarray(alvo, float); self.orto = orto
        self.escala, self.foco = escala, foco
        h = np.array([math.sin(az), 0, -math.cos(az)])          # do alvo para a camera (az=0: lateral de baixo)
        self.pos = self.alvo + dist * (math.cos(el) * h + math.sin(el) * UP)
        f = self.alvo - self.pos; self.f = f / np.linalg.norm(f)
        r = np.cross(UP, self.f); self.r = r / np.linalg.norm(r)
        self.u = np.cross(self.f, self.r)
        self.fh = -h                                              # frente no chao
        self.el = el

    def projeta(self, p):
        d = np.asarray(p, float) - self.pos
        z = d @ self.f
        k = self.escala if self.orto else self.foco / z
        return np.stack([W / 2 + k * (d @ self.r), H / 2 - k * (d @ self.u), z], -1)

    def chao(self, w=W, h=H, ss=1):
        ys, xs = np.mgrid[0:h * ss, 0:w * ss]
        a = (xs / ss - w / 2) ; bb = -(ys / ss - h / 2)
        if self.orto:
            o = self.pos + (a[..., None] * self.r + bb[..., None] * self.u) / self.escala
            ray = np.broadcast_to(self.f, o.shape)
        else:
            o = np.broadcast_to(self.pos, a.shape + (3,))
            ray = self.f + (a[..., None] * self.r + bb[..., None] * self.u) / self.foco
        t = -o[..., 1] / np.minimum(ray[..., 1], -1e-6)
        return o + ray * t[..., None], ray[..., 1] < -1e-4

    def px_por_m(self, p):
        return self.escala if self.orto else self.foco / self.projeta(p)[2]

    def inclinacao(self, p):
        if self.orto: return self.el
        d = self.pos - np.asarray(p, float)
        return math.atan2(d[1], math.hypot(d[0], d[2]))

    def giro(self, dx, dz):
        v = np.array([dx, 0, dz])
        return math.atan2(v @ self.r, v @ -self.fh)

# ------------------------------------------------------------------ campo

def gramado(cam):
    p, ok = cam.chao()
    x, z = p[..., 0], p[..., 2]
    faixa = (np.floor(x / 5.25) % 2) == 0
    a = np.array([0.15, 0.45, 0.2]); c = np.array([0.125, 0.395, 0.17])
    cor = np.where(faixa[..., None], a, c)
    rng = np.random.default_rng(3)
    cor = cor + rng.normal(0, 0.01, cor.shape[:2] + (1,))
    # luz dos refletores: mais claro no centro, cai para as bordas
    luz = 1.05 - 0.22 * np.clip(np.hypot((x - 52.5) / 70, (z - 34) / 50), 0, 1.2)
    cor = cor * luz[..., None]
    fora = (z < -3) | (z > 71) | (x < -4) | (x > 109)
    cor[fora] = cor[fora] * 0.72
    pista = (z < -7.5) | (z > 75.5) | (x < -8.5) | (x > 113.5)
    cor[pista] = np.array([0.07, 0.08, 0.075])
    return np.clip(np.where(ok[..., None], cor, 0.04), 0, 1)

def linhas(cam, d, esc):
    def poli(pts, w=0.12):
        pr = [cam.projeta([x, 0, z]) for x, z in pts]
        if any(q[2] < 1 for q in pr): return
        lw = max(1, int(w * cam.px_por_m([52, 0, 34]) * esc))
        d.line([(q[0] * esc, q[1] * esc) for q in pr], fill=(236, 240, 232, 215), width=lw, joint="curve")
    poli([(0, 0), (105, 0), (105, 68), (0, 68), (0, 0)])
    poli([(52.5, 0), (52.5, 68)])
    poli([(52.5 + 9.15 * math.cos(a), 34 + 9.15 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 90)])
    for x0, s in ((0, 1), (105, -1)):
        poli([(x0, 13.85), (x0 + s * 16.5, 13.85), (x0 + s * 16.5, 54.15), (x0, 54.15)])
        poli([(x0, 24.85), (x0 + s * 5.5, 24.85), (x0 + s * 5.5, 43.15), (x0, 43.15)])
        ang = math.acos(5.5 / 9.15)
        poli([(x0 + s * (11 + 9.15 * math.cos(a)), 34 + 9.15 * math.sin(a)) for a in np.linspace(-ang, ang, 30)] if s > 0 else
             [(x0 - 11 - 9.15 * math.cos(a), 34 + 9.15 * math.sin(a)) for a in np.linspace(-ang, ang, 30)])

def de_frente(cam, x, z):
    """a frente da placa/arquibancada olha para o centro do campo; so desenha se a camera a ve de frente"""
    n = np.array([52.5 - x, 0, 34 - z]); n /= np.linalg.norm(n)
    return n @ -cam.f > 0.05

def placas(cam, im):
    """placas de publicidade em pe atras das linhas laterais e de fundo"""
    d = ImageDraw.Draw(im)
    fonte = ImageFont.truetype(os.path.join(FONTES, "SairaExtraCondensed-ExtraBold.ttf"), 40)
    textos = ["BROMBILA BRASFOOT", "SÉRIE A 2023", "BROMBILA BRASFOOT", "CAMPEONATO BRASILEIRO"]
    segs = []
    for x in range(-2, 106, 12):
        segs.append(((x, 72.5), (x + 12, 72.5)))
        segs.append(((x, -4.5), (x + 12, -4.5)))
    for z in range(-2, 70, 12):
        segs.append(((-6.0, z), (-6.0, z + 12))); segs.append(((111.0, z), (111.0, z + 12)))
    quads = []
    for k, ((x0, z0), (x1, z1)) in enumerate(segs):
        if cam.projeta([x0, 0, z0])[0] > cam.projeta([x1, 0, z1])[0]: x0, z0, x1, z1 = x1, z1, x0, z0   # texto sempre legivel
        q = [cam.projeta([x0, 0, z0]), cam.projeta([x1, 0, z1]), cam.projeta([x1, 0.9, z1]), cam.projeta([x0, 0.9, z0])]
        if min(v[2] for v in q) < 1 or not de_frente(cam, (x0 + x1) / 2, (z0 + z1) / 2): continue
        quads.append((-(q[0][2] + q[1][2]), q, textos[k % 4]))
    for _, q, t in sorted(quads, key=lambda a: a[0]):
        pts = [(v[0], v[1]) for v in q]
        if max(v[0] for v in pts) < -50 or min(v[0] for v in pts) > W + 50: continue
        # textura com o texto, deformada no quadrilatero
        tw, th = 420, 32
        tx = Image.new("RGBA", (tw, th), (14, 16, 15, 255)); dt = ImageDraw.Draw(tx)
        bb = dt.textbbox((0, 0), t, font=fonte)
        dt.text(((tw - bb[2] + bb[0]) / 2 - bb[0], (th - bb[3] + bb[1]) / 2 - bb[1]), t, fill=(242, 193, 78), font=fonte)
        dt.line([(0, 0), (tw, 0)], fill=(242, 193, 78, 160), width=2)
        quad = [pts[3], pts[0], pts[1], pts[2]]     # PIL QUAD: sup-esq, inf-esq, inf-dir, sup-dir (origem->destino invertido)
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        x0, y0 = int(min(xs)), int(min(ys)); w = int(max(xs)) - x0 + 1; h = int(max(ys)) - y0 + 1
        if w < 2 or h < 2: continue
        # coeficientes do mapeamento destino->origem
        dst = [(p[0] - x0, p[1] - y0) for p in quad]
        src = [(0, 0), (0, th), (tw, th), (tw, 0)]
        A = []; B = []
        for (X, Y), (u, v) in zip(dst, src):
            A.append([X, Y, 1, 0, 0, 0, -u * X, -u * Y]); B.append(u)
            A.append([0, 0, 0, X, Y, 1, -v * X, -v * Y]); B.append(v)
        co = np.linalg.solve(np.array(A, float), np.array(B, float))
        pl = tx.transform((w, h), Image.PERSPECTIVE, tuple(co), Image.BICUBIC)
        m = Image.new("L", (w, h), 0); ImageDraw.Draw(m).polygon(dst, fill=255)
        pl.putalpha(m)
        im.alpha_composite(pl, (x0, y0))

def torcida(cam, im):
    """arquibancada atras das placas: degraus escuros com torcedores pontilhados"""
    rng = np.random.default_rng(7)
    camada = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(camada)
    cores = [(230, 230, 220), (200, 30, 46), (240, 240, 240), (40, 40, 48), (242, 193, 78), (20, 20, 20)]
    for lado in ("fundo", "frente", "esq", "dir"):
        meio = {"fundo": (52, 80), "frente": (52, -12), "esq": (-14, 34), "dir": (119, 34)}[lado]
        if not de_frente(cam, *meio): continue
        for deg in range(14):
            s = 9 + deg * 0.9; y = 0.5 + deg * 0.55
            if lado == "fundo": a, bq = (-12, 76 + s - 9), (117, 76 + s - 9)
            elif lado == "frente": a, bq = (-12, -8 - s + 9), (117, -8 - s + 9)
            elif lado == "esq": a, bq = (-10 - s + 9, -8), (-10 - s + 9, 76)
            else: a, bq = (115 + s - 9, -8), (115 + s - 9, 76)
            n = 260
            for k in range(n):
                t0, t1 = k / n, (k + 1) / n
                p0 = np.array([a[0] + (bq[0] - a[0]) * t0, y, a[1] + (bq[1] - a[1]) * t0])
                p1 = np.array([a[0] + (bq[0] - a[0]) * t1, y, a[1] + (bq[1] - a[1]) * t1])
                q = [cam.projeta(p0), cam.projeta(p1), cam.projeta(p1 + [0, 0.55, 0]), cam.projeta(p0 + [0, 0.55, 0])]
                if min(v[2] for v in q) < 1: continue
                g = 20 + deg
                d.polygon([(v[0], v[1]) for v in q], fill=(g, g + 3, g + 2, 255))
                if rng.random() < 0.8:
                    c = cores[rng.integers(len(cores))]
                    m = cam.projeta((p0 + p1) / 2 + [0, 0.45, 0])
                    rr = max(0.6, 0.12 * cam.px_por_m((p0 + p1) / 2))
                    d.ellipse([m[0] - rr, m[1] - rr, m[0] + rr, m[1] + rr], fill=c + (int(150 + 60 * rng.random()),))
    im.alpha_composite(camada)

def fundo(cam):
    img = gramado(cam)
    im = Image.fromarray((img * 255).astype(np.uint8)).convert("RGBA")
    torcida(cam, im)
    esc = 2
    c = Image.new("RGBA", (W * esc, H * esc), (0, 0, 0, 0))
    linhas(cam, ImageDraw.Draw(c), esc)
    im.alpha_composite(c.resize((W, H), Image.LANCZOS))
    placas(cam, im)
    for x0, sg in ((0.0, -1), (105.0, 1)): gol(cam, im, x0, sg)
    br = Image.new("RGBA", (W, H), (0, 0, 0, 0)); dg = ImageDraw.Draw(br)
    for x in (140, 1140):
        dg.ellipse([x - 200, -200, x + 200, 110], fill=(255, 245, 210, 34))
    im.alpha_composite(br.filter(ImageFilter.GaussianBlur(70)))
    return im

# ------------------------------------------------------------------ times

PELES = ["#f1c7a1", "#c98e64", "#8d5a3b", "#5b3a26", "#e0ac84", "#b97a52"]
CABELOS = ["#1c1410", "#3b2a1c", "#6b4a2a", "#0e0b09", "#a07a4a", "#1e1611"]
CORTES = ["curto", "black", "raspado", "topete", "curto", "raspado"]

def elenco(camisa, detalhe, calcao, meia, padrao, gol, semente):
    rng = random.Random(semente); out = []
    for n in range(1, 12):
        k = rng.randrange(6)
        cores = gol if n == 1 else (camisa, detalhe, calcao, meia, padrao)
        out.append(b.Uniforme(*cores, pele=PELES[k], cabelo=CABELOS[rng.randrange(6)], corte=CORTES[rng.randrange(6)], numero=n))
    return out

CASA = elenco("#c8102e", "#141414", "#f4f4f2", "#141414", "faixas", ("#2b2e2c", "#f2c14e", "#2b2e2c", "#2b2e2c", "liso"), 4)
FORA = elenco("#f4f4f2", "#141414", "#141414", "#f4f4f2", "listras", ("#f2c14e", "#1a1a1a", "#1a1a1a", "#f2c14e", "liso"), 9)

# momento: casa atacando para a direita, bola no meio-campo ofensivo
BOLA = np.array([66.0, 0.11, 30.0])
CASA_POS = [(8, 34), (38, 12), (36, 27), (37, 41), (40, 58), (55, 22), (52, 38), (60, 50), (68, 14), (65.2, 30.6), (74, 44)]
FORA_POS = [(99, 34), (80, 16), (82, 29), (81, 41), (78, 55), (72, 24), (71, 37), (66, 47), (60, 30), (58, 44), (54, 15)]

# lance de area: casa cruzando da esquerda, defesa do fora fechando
AREA_BOLA = np.array([90.5, 0.11, 22.0])
AREA_CASA = [(8, 34), (52, 20), (50, 34), (50, 48), (62, 58), (78, 30), (82, 46), (89.5, 21.5), (97, 30), (95, 39), (86, 36)]
AREA_FORA = [(103, 33), (96, 25), (98, 33), (98, 40), (93, 50), (91, 27), (90, 38), (86, 26), (80, 33), (76, 45), (72, 22)]
MOMENTO = "meio"

_cache = {}
def sprite(u, giro, fase, corre, incl, alt, chave=None):
    """giro e fase quantizados (16 direcoes, 8 passos) para reaproveitar"""
    gq = round(giro / (2 * math.pi / 16)) % 16; fq = round(fase / (2 * math.pi / 8)) % 8 if corre else 0
    k = (id(u), gq, fq, corre, round(incl, 2), int(alt)) if chave is None else chave
    if k not in _cache:
        j = b.Jogador(u, b.pose(fq * 2 * math.pi / 8, corre))
        _cache[k] = b.render(j, gq * 2 * math.pi / 16, incl, alt, aa=2)
    return _cache[k]

def jogadores(t=0.0):
    """posicoes e direcoes no instante t (segundos): todos correndo um pouco para a direita"""
    out = []
    rng = random.Random(11)
    bola0 = AREA_BOLA if MOMENTO == "area" else BOLA
    cp, fp = (AREA_CASA, AREA_FORA) if MOMENTO == "area" else (CASA_POS, FORA_POS)
    for time, pos, u in (("casa", cp, CASA), ("fora", fp, FORA)):
        for i, (x, z) in enumerate(pos):
            if time == "casa":
                vx, vz = (0.0, 0.3) if i == 0 else (4.2 + rng.uniform(-0.8, 0.8), rng.uniform(-1.2, 1.2))
            else:
                ax, az_ = bola0[0] - x, bola0[2] - z; L = math.hypot(ax, az_) + 1e-6
                vx, vz = (0.0, -0.3) if i == 0 else (3.0 * ax / L + 1.5, 3.0 * az_ / L)
            corre = i != 0
            out.append((x + vx * t, z + vz * t, vx, vz, u[i], corre, rng.uniform(0, 6.28) + t * 9.5))
    return out

def pos_bola(t):
    if MOMENTO != "area": return BOLA + [4.6 * t, 0, 0.3 * t]
    # chute rasteiro no canto, para dentro da rede
    v = np.array([6.6, 0, 4.4])
    p = AREA_BOLA + v * min(t, 2.45)
    if t > 2.45: p = p + [0.6 * (1 - math.exp(-(t - 2.45) * 3)), 0, 0]
    return p

def desenha(cam, im, t=0.0, bola=None):
    js = jogadores(t)
    if bola is None: bola = pos_bola(t)
    js.sort(key=lambda j: -cam.projeta([j[0], 0, j[1]])[2])
    pb = cam.projeta(bola)
    feito_bola = False
    for x, z, vx, vz, u, corre, fase in js:
        pe = cam.projeta([x, 0, z])
        if not feito_bola and pe[2] < pb[2]:
            desenha_bola(cam, im, bola); feito_bola = True
        if pe[2] < 1 or pe[0] < -80 or pe[0] > W + 80 or pe[1] < -40 or pe[1] > H + 200: continue
        alt = 1.95 * cam.px_por_m([x, 0, z])
        s = sprite(u, cam.giro(vx, vz), fase, corre, cam.inclinacao([x, 0, z]), alt)
        sw, sh = s.size
        im.alpha_composite(s, (int(round(pe[0] - sw / 2)), int(round(pe[1] - sh * 0.86))))
    if not feito_bola: desenha_bola(cam, im, bola)

def gol(cam, im, x0, s):
    """trave, travessao e rede em linhas 3D (s = -1 gol da esquerda, +1 da direita)"""
    esc = 2
    c = Image.new("RGBA", (W * esc, H * esc), (0, 0, 0, 0)); d = ImageDraw.Draw(c)
    k = cam.px_por_m([x0, 0, 34]) * esc
    def seg(a, b_, cor, w):
        pa, pb = cam.projeta(a), cam.projeta(b_)
        if min(pa[2], pb[2]) < 1: return
        d.line([(pa[0] * esc, pa[1] * esc), (pb[0] * esc, pb[1] * esc)], fill=cor, width=max(1, int(w * k)))
    z0, z1, h, f = 34 - 3.66, 34 + 3.66, 2.44, x0 + s * 2.0
    rede = (235, 235, 230, 90)
    for z in np.linspace(z0, z1, 15):
        seg([f, 0, z], [f, h * 0.8, z], rede, 0.025); seg([f, h * 0.8, z], [x0, h, z], rede, 0.025)
    for y in np.linspace(0, h * 0.8, 9):
        seg([f, y, z0], [f, y, z1], rede, 0.025)
    for z in (z0, z1):
        for y in np.linspace(0, h, 9): seg([x0, y, z], [f, min(y, h * 0.8), z], rede, 0.025)
    for x in np.linspace(x0, f, 5): seg([x, h - (h * 0.2) * abs(x - x0) / 2.0, z0], [x, h - (h * 0.2) * abs(x - x0) / 2.0, z1], rede, 0.025)
    for z in (z0, z1): seg([x0, 0, z], [x0, h, z], (250, 250, 248, 255), 0.12)
    seg([x0, h, z0], [x0, h, z1], (250, 250, 248, 255), 0.12)
    im.alpha_composite(c.resize((W, H), Image.LANCZOS))

def desenha_bola(cam, im, bola):
    pb = cam.projeta(bola); pc = cam.projeta([bola[0], 0, bola[2]])
    rb = max(2.5, 0.12 * cam.px_por_m(bola))
    s = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(s)
    d.ellipse([pc[0] - rb * 1.2, pc[1] - rb * 0.45, pc[0] + rb * 1.2, pc[1] + rb * 0.45], fill=(0, 0, 0, 100))
    im.alpha_composite(s.filter(ImageFilter.GaussianBlur(1.2)))
    d = ImageDraw.Draw(im)
    cy = pb[1]
    d.ellipse([pb[0] - rb, cy - rb, pb[0] + rb, cy + rb], fill=(250, 250, 246), outline=(60, 60, 60))
    d.ellipse([pb[0] - rb * 0.35, cy - rb * 0.35, pb[0] + rb * 0.25, cy + rb * 0.25], fill=(40, 40, 40))

def placar(im, minuto="63'"):
    d = ImageDraw.Draw(im)
    ft = ImageFont.truetype(os.path.join(FONTES, "Manrope-Bold.ttf"), 17)
    fn = ImageFont.truetype(os.path.join(FONTES, "SairaExtraCondensed-ExtraBold.ttf"), 26)
    x, y = 28, 24
    d.rounded_rectangle([x, y, x + 300, y + 40], 20, fill=(10, 12, 11, 225), outline=(242, 193, 78, 150))
    d.rectangle([x + 18, y + 12, x + 30, y + 17], fill=(200, 16, 46)); d.rectangle([x + 18, y + 17, x + 30, y + 22], fill=(20, 20, 20)); d.rectangle([x + 18, y + 22, x + 30, y + 28], fill=(200, 16, 46))
    d.text((x + 40, y + 9), "RUB", fill=(241, 238, 231), font=ft)
    d.text((x + 100, y + 4), "1  x  0", fill=(242, 193, 78), font=fn)
    d.rectangle([x + 172, y + 12, x + 176, y + 28], fill=(244, 244, 242)); d.rectangle([x + 176, y + 12, x + 180, y + 28], fill=(20, 20, 20)); d.rectangle([x + 180, y + 12, x + 184, y + 28], fill=(244, 244, 242))
    d.text((x + 192, y + 9), "ALV", fill=(241, 238, 231), font=ft)
    d.text((x + 248, y + 9), minuto, fill=(158, 155, 146), font=ft)

# ------------------------------------------------------------------ cameras prontas

def cam_iso(alvo=(64.0, 0, 32.0), escala=26.0):
    return Camera(alvo, az=math.radians(-38), el=math.radians(36), dist=120, escala=escala, orto=True)

def cam_tv(alvo=(64.0, 0, 30.0)):
    return Camera(alvo, az=0.0, el=math.atan2(26, 80), dist=math.hypot(26, 80), foco=2350.0, orto=False)

def cam_perto(alvo=(66.0, 0, 30.0)):
    return Camera(alvo, az=math.radians(8), el=math.atan2(9, 44), dist=math.hypot(9, 44), foco=2500.0, orto=False)

def foto(cam, saida, t=0.0):
    im = fundo(cam); desenha(cam, im, t); placar(im)
    im.convert("RGB").save(saida, quality=88)

def video(cam_fn, pasta, n=48, fps=24):
    """camera acompanhando a bola (com atraso suave); o fundo e refeito a cada quadro porque a camera anda"""
    os.makedirs(pasta, exist_ok=True)
    alvo = None
    for i in range(n):
        t = i / fps
        bola = pos_bola(t)
        quer = np.array([min(bola[0], 96.0) - 1.0, 0, bola[2] + 4.0])
        alvo = quer if alvo is None else alvo + (quer - alvo) * 0.12
        cam = cam_fn(tuple(alvo))
        im = fundo(cam); desenha(cam, im, t, bola); placar(im)
        im.convert("RGB").save(os.path.join(pasta, "q%03d.png" % i))
        print("quadro", i, flush=True)

if __name__ == "__main__":
    pasta = sys.argv[1]
    for o in sys.argv[2:] or ["iso", "tv"]:
        if o == "iso": foto(cam_iso(), os.path.join(pasta, "cena-iso.jpg"))
        elif o == "iso-perto":
            MOMENTO = "area"
            foto(cam_iso(escala=34.0, alvo=(93.0, 0, 37.0)), os.path.join(pasta, "cena-iso-perto.jpg"))
            MOMENTO = "meio"
        elif o == "tv": foto(cam_tv(), os.path.join(pasta, "cena-tv.jpg"))
        elif o == "perto": foto(cam_perto(), os.path.join(pasta, "cena-perto.jpg"))
        elif o == "video-iso":
            MOMENTO = "area"
            video(lambda a: cam_iso(a, escala=34.0), os.path.join(pasta, "video-iso"), n=84)
            MOMENTO = "meio"
        elif o == "video-tv": video(cam_tv, os.path.join(pasta, "video-tv"))
        print("ok", o, flush=True)
