# Tacas proprias do Brombila Brasfoot, renderizadas do zero (ray marching em numpy).
# Seis modelos por tipo de competicao, em ouro, sobre uma base escura com filete dourado e
# uma placa lisa na frente, onde o jogo escreve o nome da competicao.
#
# Uso: python3 tools/tacas/tacas.py <pasta de saida> [modelo ...]
# Gera <modelo>.png (fundo transparente) e <modelo>.txt com o retangulo da placa (x y w h).
import os
import sys
import numpy as np
from PIL import Image

W, H, AA = 440, 640, 2          # tamanho final e supersampling
ESCALA = 1.0 / 2.9               # unidades do mundo por pixel final * W
INCL = np.radians(9)             # camera um pouco acima
LARG = 2.3                       # largura da cena em unidades


def norm(v):
    return v / np.maximum(np.linalg.norm(v, axis=-1, keepdims=True), 1e-9)


# ------------------------------------------------------------------ formas

def suaviza(perfil, n=12):
    """Catmull-Rom pelos pontos do perfil: curva lisa, sem facetas."""
    pts = np.array(perfil, float)
    pts = np.vstack([pts[0], pts, pts[-1]])
    out = []
    for i in range(1, len(pts) - 2):
        p0, p1, p2, p3 = pts[i - 1], pts[i], pts[i + 1], pts[i + 2]
        for t in np.linspace(0, 1, n, endpoint=False):
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(pts[-2])
    out = np.array(out)
    # garante y crescente para o interp
    keep = np.concatenate([[True], np.diff(out[:, 0]) > 1e-6])
    return out[keep]


def torno(p, perfil, y0, y1):
    """Solido de revolucao em torno do eixo y. perfil: lista (y, raio)."""
    c = suaviza(perfil)
    ys, rs = c[:, 0], np.maximum(c[:, 1], 0.0)
    y = p[..., 1]
    rho = np.hypot(p[..., 0], p[..., 2])
    r = np.interp(y, ys, rs)
    dr = np.gradient(rs, ys)
    inc = np.interp(y, ys, dr)
    d = (rho - r) / np.sqrt(1 + inc * inc)
    # onde o perfil e ingreme a conta acima encolhe demais e da acerto falso longe da peca;
    # rho - raio maximo e sempre um limite valido
    d = np.maximum(d, rho - rs.max())
    d = np.maximum(d, np.maximum(y0 - y, y - y1))
    return d


def oco(p, perfil, yfundo, ytopo, parede=0.035):
    """Taca aberta: tira o miolo de cima ate yfundo."""
    dentro = [(a, max(b - parede, 0.0)) for a, b in perfil]
    return torno(p, dentro, yfundo, ytopo + 0.2)


def toro_v(p, c, R, r, lado):
    """Alca: meio anel vertical no plano xy, do lado +1 (direita) ou -1."""
    q = p - np.array(c)
    a = np.hypot(q[..., 0], q[..., 1]) - R
    d = np.hypot(a, q[..., 2]) - r
    return np.maximum(d, -lado * p[..., 0])


def toro_h(p, y, R, r):
    q = p.copy()
    a = np.hypot(q[..., 0], q[..., 2]) - R
    return np.hypot(a, q[..., 1] - y) - r


def esfera(p, c, r):
    return np.linalg.norm(p - np.array(c), axis=-1) - r


def caixa(p, c, b, raio=0.0):
    q = np.abs(p - np.array(c)) - (np.array(b) - raio)
    return np.linalg.norm(np.maximum(q, 0), axis=-1) + np.minimum(np.max(q, axis=-1), 0) - raio


def cilindro(p, y0, y1, r):
    rho = np.hypot(p[..., 0], p[..., 2])
    return np.maximum(rho - r, np.maximum(y0 - p[..., 1], p[..., 1] - y1))


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


# ------------------------------------------------------------------ base comum

BASE_TOPO = -1.05


def base(p):
    """Pedestal escuro em dois degraus (material 1) e filetes dourados (material 0)."""
    preto = caixa(p, (0, -1.32, 0), (0.62, 0.27, 0.46), 0.03)
    preto = np.minimum(preto, caixa(p, (0, -1.0 - 0.06, 0), (0.48, 0.07, 0.36), 0.02))
    filete = caixa(p, (0, -1.59, 0), (0.66, 0.03, 0.50), 0.015)
    filete = np.minimum(filete, caixa(p, (0, -1.05 + 0.065, 0), (0.50, 0.018, 0.38), 0.01))
    # placa na frente (ouro escovado, lisa)
    placa = caixa(p, (0, -1.33, -0.465), (0.46, 0.13, 0.012), 0.01)
    return preto, np.minimum(filete, placa)


PLACA = ((-0.46, -1.20), (0.46, -1.46), -0.478)  # cantos (x, y) e z da frente da placa


# ------------------------------------------------------------------ modelos

def liga(p):
    """Liga nacional: taca larga de duas alcas em pe alto."""
    perfil = [(-1.05, 0.30), (-0.98, 0.30), (-0.92, 0.12), (-0.55, 0.075), (-0.42, 0.13), (-0.36, 0.09),
              (-0.20, 0.16), (0.05, 0.36), (0.35, 0.50), (0.55, 0.55), (0.62, 0.57)]
    d = torno(p, perfil, -1.05, 0.62)
    d = np.maximum(d, -oco(p, perfil, 0.25, 0.62))
    d = np.minimum(d, toro_h(p, 0.60, 0.565, 0.035))
    d = np.minimum(d, toro_h(p, -0.42, 0.13, 0.03))
    for s in (1, -1):
        d = np.minimum(d, toro_v(p, (s * 0.46, 0.18, 0), 0.24, 0.045, s))
    return d


def copa(p):
    """Copa nacional: calice alto e fino com alcas compridas da boca ao pe."""
    perfil = [(-1.05, 0.26), (-0.98, 0.26), (-0.90, 0.10), (-0.30, 0.07), (-0.18, 0.12), (-0.05, 0.10),
              (0.20, 0.20), (0.55, 0.32), (0.85, 0.36), (0.92, 0.38)]
    d = torno(p, perfil, -1.05, 0.92)
    d = np.maximum(d, -oco(p, perfil, 0.55, 0.92))
    d = np.minimum(d, toro_h(p, 0.90, 0.375, 0.03))
    d = np.minimum(d, toro_h(p, -0.18, 0.12, 0.028))
    for s in (1, -1):
        # alca alongada: anel achatado na vertical
        q = p.copy()
        q[..., 1] = (q[..., 1] - 0.30) / 1.9
        h = toro_v(q, (s * 0.30, 0, 0), 0.24, 0.026, s) * 1.0
        d = np.minimum(d, h)
    return d


def supercopa(p):
    """Supercopa: fruteira robusta (pe em trompete, gomo no meio) com a bola sobre a boca."""
    perfil = [(-1.05, 0.30), (-0.98, 0.30), (-0.90, 0.17), (-0.62, 0.10), (-0.38, 0.10), (-0.24, 0.17),
              (-0.15, 0.13), (-0.02, 0.20), (0.14, 0.42), (0.27, 0.56), (0.32, 0.58)]
    d = torno(p, perfil, -1.05, 0.32)
    d = np.maximum(d, -oco(p, perfil, 0.14, 0.32, 0.035))
    d = np.minimum(d, toro_h(p, 0.31, 0.575, 0.035))
    d = np.minimum(d, toro_h(p, -0.24, 0.17, 0.035))
    d = np.minimum(d, toro_h(p, -0.62, 0.11, 0.025))
    d = np.minimum(d, esfera(p, (0, 0.50, 0), 0.25))
    # coroa de quatro garras segurando a bola
    for k in range(4):
        a = k * np.pi / 2 + np.pi / 4
        rx = p[..., 0] * np.cos(a) + p[..., 2] * np.sin(a)
        rz = -p[..., 0] * np.sin(a) + p[..., 2] * np.cos(a)
        g = np.hypot(np.hypot(rx, p[..., 1] - 0.50) - 0.27, rz) - 0.03
        g = np.maximum(g, np.maximum(-rx, p[..., 1] - 0.62))
        d = smin(d, g, 0.02)
    return d


def estadual(p):
    """Estadual e regionais: urna classica com tampa e pinaculo."""
    perfil = [(-1.05, 0.26), (-0.98, 0.26), (-0.92, 0.11), (-0.72, 0.08), (-0.62, 0.15), (-0.40, 0.33),
              (-0.10, 0.40), (0.15, 0.36), (0.28, 0.27), (0.34, 0.30), (0.40, 0.30), (0.46, 0.22),
              (0.55, 0.12), (0.62, 0.05), (0.66, 0.05)]
    d = torno(p, perfil, -1.05, 0.66)
    d = np.minimum(d, esfera(p, (0, 0.74, 0), 0.09))
    d = np.minimum(d, toro_h(p, 0.34, 0.30, 0.03))
    d = np.minimum(d, toro_h(p, -0.62, 0.15, 0.025))
    for s in (1, -1):
        d = np.minimum(d, toro_v(p, (s * 0.36, -0.10, 0), 0.20, 0.04, s))
    return d


def continental(p):
    """Continentais de clubes: taca alta com alcas grandes do pescoco ate a base do corpo."""
    perfil = [(-1.05, 0.30), (-0.98, 0.30), (-0.92, 0.13), (-0.70, 0.09), (-0.50, 0.12), (-0.30, 0.10),
              (-0.05, 0.18), (0.30, 0.34), (0.62, 0.43), (0.88, 0.47), (0.95, 0.49)]
    d = torno(p, perfil, -1.05, 0.95)
    d = np.maximum(d, -oco(p, perfil, 0.55, 0.95))
    d = np.minimum(d, toro_h(p, 0.93, 0.485, 0.035))
    d = np.minimum(d, toro_h(p, -0.30, 0.11, 0.03))
    for s in (1, -1):
        q = p.copy()
        q[..., 1] = (q[..., 1] - 0.28) / 1.45
        d = np.minimum(d, toro_v(q, (s * 0.40, 0, 0), 0.36, 0.05, s))
    return d


def selecoes(p):
    """Selecoes e mundiais: globo seguro por quatro laminas que sobem de uma haste."""
    perfil = [(-1.05, 0.28), (-0.98, 0.28), (-0.90, 0.12), (-0.40, 0.10), (-0.10, 0.17), (0.12, 0.22)]
    d = torno(p, perfil, -1.05, 0.12)
    c = np.array((0, 0.50, 0))
    g = esfera(p, c, 0.40)
    # sulcos do globo (meridianos e paralelos)
    q = p - c
    lon = np.arctan2(q[..., 2], q[..., 0])
    lat = np.arcsin(np.clip(q[..., 1] / np.maximum(np.linalg.norm(q, axis=-1), 1e-6), -1, 1))
    sulco = np.minimum(np.abs(np.sin(lon * 4)), np.abs(np.sin(lat * 5)))
    g = g + 0.012 * (sulco < 0.10)
    d = np.minimum(d, g)
    for k in range(4):
        a = k * np.pi / 2 + np.pi / 4
        # lamina curva: anel fino inclinado que abraca o globo
        rx = p[..., 0] * np.cos(a) + p[..., 2] * np.sin(a)
        rz = -p[..., 0] * np.sin(a) + p[..., 2] * np.cos(a)
        qq = np.stack([rx, p[..., 1], rz], axis=-1)
        anel = np.hypot(np.hypot(qq[..., 0], qq[..., 1] - 0.42) - 0.47, qq[..., 2]) - 0.035
        anel = np.maximum(anel, qq[..., 1] - 0.62)
        anel = np.maximum(anel, -qq[..., 0])
        d = smin(d, anel, 0.03)
    return d


MODELOS = {"liga": liga, "copa": copa, "supercopa": supercopa, "estadual": estadual,
           "continental": continental, "selecoes": selecoes}


# ------------------------------------------------------------------ render

# quanto cada modelo cresce a partir do topo da base, para encher o quadro
TAMANHO = {"liga": 1.25, "copa": 1.12, "supercopa": 1.22, "estadual": 1.25, "continental": 1.10, "selecoes": 1.18}


def cena(p, modelo):
    k = TAMANHO[modelo]
    q = p / k
    q[..., 1] = (p[..., 1] - BASE_TOPO) / k + BASE_TOPO
    t = MODELOS[modelo](q) * k
    preto, filete = base(p)
    ouro = np.minimum(t, filete)
    d = np.minimum(ouro, preto)
    mat = (preto < ouro).astype(np.int8)  # 1 = base escura
    return d, mat


def normal(p, modelo):
    e = 0.0015
    n = np.zeros_like(p)
    for i in range(3):
        o = np.zeros(3)
        o[i] = e
        n[..., i] = cena(p + o, modelo)[0] - cena(p - o, modelo)[0]
    return norm(n)


def ambiente(r):
    """Estudio: fundo escuro, duas softboxes, luz quente de baixo."""
    y = r[..., 1]
    x = r[..., 0]
    z = r[..., 2]
    c = 0.02 + 0.06 * np.clip(y, 0, 1)
    caixa1 = np.exp(-((x + 0.55) ** 2 / 0.05 + (y - 0.35) ** 2 / 0.12)) * 2.2 * (z < 0.3)
    caixa2 = np.exp(-((x - 0.65) ** 2 / 0.03 + (y - 0.15) ** 2 / 0.25)) * 1.6 * (z < 0.4)
    topo = np.exp(-((y - 0.95) ** 2) / 0.02) * 1.2
    chao = 0.25 * np.clip(-y, 0, 1)
    return c + caixa1 + caixa2 + topo + chao


def render(modelo):
    w, h = W * AA, H * AA
    s = ESCALA / AA * (440 / W) * 2.9 / 2.9
    xs = (np.arange(w) - w / 2 + 0.5) * (LARG / w)
    ys = (h / 2 - np.arange(h) - 0.5) * (LARG / w) - 0.30
    X, Y = np.meshgrid(xs, ys)
    ci, si = np.cos(INCL), np.sin(INCL)
    # camera ortografica olhando para +z, inclinada para baixo
    dirc = np.array([0, -si, ci])
    up = np.array([0, ci, si])
    ori = (X[..., None] * np.array([1, 0, 0]) + Y[..., None] * up) - dirc * 4.0
    ori = ori.reshape(-1, 3)
    n = ori.shape[0]
    t = np.zeros(n)
    hit = np.zeros(n, bool)
    vivo = np.ones(n, bool)
    for _ in range(300):
        idx = np.nonzero(vivo)[0]
        if idx.size == 0:
            break
        p = ori[idx] + dirc * t[idx, None]
        d, _ = cena(p, modelo)
        bate = d < 0.0008
        hit[idx[bate]] = True
        vivo[idx[bate]] = False
        t[idx] += np.maximum(d * 0.8, 0.0006)
        vivo[idx[t[idx] > 8.0]] = False
    img = np.zeros((n, 4))
    idx = np.nonzero(hit)[0]
    p = ori[idx] + dirc * t[idx, None]
    _, mat = cena(p, modelo)
    nn = normal(p, modelo)
    v = -dirc
    r = 2 * np.sum(nn * v, -1, keepdims=True) * nn - v
    env = ambiente(r)
    fres = 0.25 + 0.75 * (1 - np.clip(np.sum(nn * v, -1), 0, 1)) ** 4
    luz = norm(np.array([-0.5, 0.7, -0.6]))
    dif = np.clip(np.sum(nn * luz, -1), 0, 1)
    # oclusao barata: distancia a cena um pouco fora da superficie
    ao = np.clip(cena(p + nn * 0.06, modelo)[0] / 0.06, 0, 1) ** 0.6
    ouro = np.array([1.0, 0.76, 0.33])
    cor_ouro = ouro[None] * (0.10 + 0.95 * env[:, None]) * (0.55 + 0.45 * ao[:, None]) + fres[:, None] * 0.25 * ouro[None]
    cor_ouro += np.clip(env - 1.2, 0, None)[:, None] * np.array([1.0, 0.95, 0.85]) * 0.5
    preto = np.array([0.055, 0.06, 0.058])
    cor_preto = preto[None] * (0.6 + 1.2 * dif[:, None]) * ao[:, None] + 0.10 * np.clip(env - 0.6, 0, None)[:, None] * np.array([1, 0.95, 0.85])
    cor = np.where(mat[:, None] == 1, cor_preto, cor_ouro)
    # tonemap suave
    cor = cor / (1 + 0.35 * cor)
    cor = np.clip(cor * 1.15, 0, 1) ** (1 / 1.25)
    img[idx, :3] = cor
    img[idx, 3] = 1
    img = img.reshape(h, w, 4)
    im = Image.fromarray((img * 255).astype(np.uint8), "RGBA").resize((W, H), Image.LANCZOS)
    # retangulo da placa na imagem final
    (x0, y0), (x1, y1), z = PLACA
    def proj(x, y):
        yy = y * ci + z * si  # coordenada "up" da camera
        px = (x / (LARG / w) + w / 2) / AA
        py = (h / 2 - (yy + 0.30) / (LARG / w)) / AA
        return px, py
    a, b = proj(x0, y0), proj(x1, y1)
    return im, (round(a[0]), round(a[1]), round(b[0] - a[0]), round(b[1] - a[1]))


if __name__ == "__main__":
    saida = sys.argv[1]
    os.makedirs(saida, exist_ok=True)
    nomes = sys.argv[2:] or list(MODELOS)
    for m in nomes:
        im, placa = render(m)
        im.save(os.path.join(saida, m + ".png"), optimize=True)
        with open(os.path.join(saida, m + ".txt"), "w") as f:
            f.write("%d %d %d %d\n" % placa)
        print(m, placa)
