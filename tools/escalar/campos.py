# Gera os gramados da tela Escalar time (aicons/campo*.png, 434x650) do zero, em 4x,
# com a mesma geometria de linhas do jogo original:
#   campo2 = Excelente, campo = Muito boa, campo3 = Ruim, campo4 = Precaria.
# Grama com textura de folhas em varias escalas, listras de corte, luz de refletor,
# vinheta, linhas creme nitidas e, nos estados ruins, falhas de terra e grama seca.
# Uso: python3 campos.py <pasta de saida>
import sys, os
import numpy as np
from PIL import Image

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
W0, H0, K = 434, 650, 4
W, H = W0 * K, H0 * K
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
X, Y = xx / K, yy / K  # coordenadas em pixels do tamanho final


def ruido(sw, sh, semente):
    """Ruido suave: grade aleatoria sw x sh ampliada com bicubico, valores 0..1."""
    r = np.random.default_rng(semente).random((sh, sw)).astype(np.float32)
    im = Image.fromarray((r * 255).astype(np.uint8)).resize((W, H), Image.BICUBIC)
    return np.asarray(im).astype(np.float32) / 255


def fbm(base_w, base_h, oitavas, semente, ganho=0.5):
    soma, amp, tot = 0, 1.0, 0
    for o in range(oitavas):
        soma = soma + amp * ruido(base_w * 2 ** o, base_h * 2 ** o, semente + o)
        tot += amp
        amp *= ganho
    return soma / tot


def suave(a, b, t):
    t = np.clip((t - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


# --------------------------------------------------------------- textura comum
folhas = ruido(W // 3, H // 9, 11)            # folhas: riscos finos no sentido do corte
folhas2 = ruido(W // 2, H // 2, 12)           # granulado
tufos = fbm(22, 33, 4, 20)                    # tufos medios
manchas = fbm(4, 6, 3, 40)                    # variacao grande
grama = 0.42 * folhas + 0.18 * folhas2 + 0.25 * tufos + 0.15 * manchas

# listras de corte (17 faixas na largura do campo, borda levemente ondulada)
ond = (fbm(3, 40, 2, 60) - 0.5) * 3.0
faixa = np.floor((Y - 20 + ond) / (605 / 16)).astype(int)
listra = (faixa % 2).astype(np.float32)

# luz de refletores nos quatro cantos + vinheta
def refletor(cx, cy, r):
    d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2) / r
    return np.clip(1 - d, 0, 1) ** 2

luz = 0.6 * refletor(-30, -40, 540) + 0.6 * refletor(W0 + 30, -40, 540) \
    + 0.4 * refletor(-30, H0 + 40, 500) + 0.4 * refletor(W0 + 30, H0 + 40, 500)
nx, ny = (X / W0 - 0.5) * 2, (Y / H0 - 0.5) * 2
vinheta = np.clip(1 - 0.34 * (nx ** 2 * 0.9 + ny ** 2 * 0.55), 0, 1)

# --------------------------------------------------------------- linhas
LW = 1.25  # meia espessura em px finais


def seg_h(y, x0, x1):
    d = np.where((X >= x0) & (X <= x1), np.abs(Y - y), 1e9)
    return d

def seg_v(x, y0, y1):
    return np.where((Y >= y0) & (Y <= y1), np.abs(X - x), 1e9)

def circ(cx, cy, r, cond=None):
    d = np.abs(np.sqrt((X - cx) ** 2 + (Y - cy) ** 2) - r)
    return d if cond is None else np.where(cond, d, 1e9)

def ponto(cx, cy, r):
    return np.maximum(np.sqrt((X - cx) ** 2 + (Y - cy) ** 2) - r + LW, 0)

x0, x1, y0, y1 = 18, 414, 20.5, 625.5
cx, cy = 216, 322.5
d = np.minimum.reduce([
    seg_h(y0, x0, x1), seg_h(y1, x0, x1), seg_v(x0, y0, y1), seg_v(x1, y0, y1),
    seg_h(cy, x0, x1), circ(cx, cy, 60), ponto(cx, cy, 2.6),
    # grande area e pequena area (cima)
    seg_v(71, y0, 129.5), seg_v(360.5, y0, 129.5), seg_h(129.5, 71, 360.5),
    seg_v(150.5, y0, 56.5), seg_v(281, y0, 56.5), seg_h(56.5, 150.5, 281),
    ponto(cx, 93.5, 1.9), circ(cx, 93.5, 60.5, Y > 129.5),
    # baixo
    seg_v(71, 516, y1), seg_v(360.5, 516, y1), seg_h(516, 71, 360.5),
    seg_v(150.5, 588.5, y1), seg_v(281, 588.5, y1), seg_h(588.5, 150.5, 281),
    ponto(cx, 551.5, 1.9), circ(cx, 551.5, 60.5, Y < 516),
    # escanteios
    circ(x0, y0, 6, (X >= x0) & (Y >= y0)), circ(x1, y0, 6, (X <= x1) & (Y >= y0)),
    circ(x0, y1, 6, (X >= x0) & (Y <= y1)), circ(x1, y1, 6, (X <= x1) & (Y <= y1)),
])
linha = 1 - suave(LW - 0.45, LW + 0.45, d)
linha_brilho = 1 - suave(LW, LW + 3.5, d)  # leve halo de tinta

# --------------------------------------------------------------- estados
# base escura (cor da grama) e clara (topo das folhas), por estado
ESTADOS = {
    'campo2.png': dict(escuro=(0.035, 0.11, 0.045), claro=(0.17, 0.40, 0.13), listra=0.22, seca=0.0, terra=0.0),
    'campo.png':  dict(escuro=(0.04, 0.105, 0.045), claro=(0.18, 0.34, 0.13), listra=0.17, seca=0.12, terra=0.0),
    'campo3.png': dict(escuro=(0.05, 0.10, 0.045), claro=(0.21, 0.32, 0.13), listra=0.13, seca=0.35, terra=0.55),
    'campo4.png': dict(escuro=(0.06, 0.095, 0.045), claro=(0.25, 0.30, 0.13), listra=0.09, seca=0.65, terra=1.0),
}
seco_cor = np.array([0.30, 0.27, 0.13], np.float32)
terra_cor = np.array([0.23, 0.17, 0.10], np.float32)

# onde o campo mais gasta: areas, meio-campo e (no precario) corredores
def blob(cx, cy, rx, ry):
    return np.exp(-(((X - cx) / rx) ** 2 + ((Y - cy) / ry) ** 2))

gasto = np.maximum.reduce([blob(216, 70, 95, 45), blob(216, 575, 95, 45), blob(216, 322, 85, 55)])
gasto_largo = np.maximum.reduce([gasto * 1.15, 0.55 * blob(216, 200, 60, 70), 0.55 * blob(216, 445, 60, 70),
                                 0.45 * blob(60, 322, 40, 140), 0.45 * blob(372, 322, 40, 140)])
borda = fbm(10, 15, 4, 80)

for nome, e in ESTADOS.items():
    esc, cla = np.array(e['escuro'], np.float32), np.array(e['claro'], np.float32)
    t = np.clip(grama * (1 + e['listra'] * (listra - 0.5) * 0.8), 0, 1)
    t = np.clip((t - 0.22) / 0.62, 0, 1) ** 1.1
    rgb = esc + (cla - esc) * t[..., None]
    rgb *= (1 + e['listra'] * 0.9 * (listra[..., None] - 0.5))
    # grama seca (amarelada) em manchas
    if e['seca'] > 0:
        s = e['seca'] * suave(0.45, 0.75, manchas * 0.6 + borda * 0.4 + 0.25 * gasto_largo)
        seca = seco_cor * (0.55 + 0.7 * t[..., None])
        rgb = rgb * (1 - s[..., None]) + seca * s[..., None]
    # falhas de terra
    if e['terra'] > 0:
        area = gasto if e['terra'] < 1 else gasto_largo
        v = area * e['terra'] * 0.8 + borda * 0.35 + tufos * 0.25 + folhas2 * 0.12 - 0.12
        tm = suave(0.50, 0.58, v)
        # tufos de grama que sobraram dentro da falha
        tm *= 1 - 0.85 * suave(0.55, 0.75, folhas * 0.7 + tufos * 0.5 - 0.1)
        terra = terra_cor * (0.6 + 0.5 * folhas2[..., None] + 0.4 * tufos[..., None])
        rgb = rgb * (1 - tm[..., None]) + terra * tm[..., None]
    # luz da noite
    rgb = rgb * (0.92 + 0.55 * luz[..., None]) * vinheta[..., None]
    rgb += np.array([1.0, 0.86, 0.55], np.float32) * (0.035 * luz[..., None])
    # linhas
    cor = np.array([0.93, 0.92, 0.85], np.float32) * (0.88 + 0.12 * folhas2[..., None])
    al = linha * (0.80 + 0.15 * luz)
    rgb = rgb + np.array([0.9, 0.9, 0.8], np.float32) * 0.05 * linha_brilho[..., None]
    rgb = rgb * (1 - al[..., None]) + cor * al[..., None]
    rgb = np.clip(rgb, 0, 1)
    im = Image.fromarray((rgb * 255).round().astype(np.uint8), 'RGB').resize((W0, H0), Image.LANCZOS)
    im.convert('RGBA').save(os.path.join(out, nome), optimize=True)
    print('ok', nome)
