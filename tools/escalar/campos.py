# Gera os campos "noite de estreia" (aicons/campo*.png) a partir dos originais do jogo.
# Uso: python3 campos.py <pasta com os campo*.png originais> <pasta de saida>
# - tira as linhas, escurece e esfria a grama (gramado de jogo noturno),
# - mantem as listras de corte e as falhas de grama (Ruim/Precaria),
# - luz de refletor vinda dos cantos e vinheta nas bordas,
# - linhas redesenhadas em creme suave a partir da mascara do campo2 (Excelente).
import sys, os
import numpy as np
from PIL import Image, ImageFilter

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)

def arr(p):
    return np.asarray(Image.open(os.path.join(src, p)).convert('RGBA')).astype(np.float32) / 255.0

def lum(a):
    return 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]

# mascara das linhas: pixels bem mais claros que a vizinhanca no campo2
ref = arr('campo2.png')
L = lum(ref)
viz = np.asarray(Image.fromarray((L * 255).astype(np.uint8)).filter(ImageFilter.MedianFilter(7))).astype(np.float32) / 255
linha = np.clip((L - viz - 0.10) / 0.25, 0, 1)
H, W = L.shape
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)

def refletor(cx, cy, r):
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / r
    return np.clip(1 - d, 0, 1) ** 2

luz = 0.55 * refletor(-30, -40, 520) + 0.55 * refletor(W + 30, -40, 520) \
    + 0.35 * refletor(-30, H + 40, 480) + 0.35 * refletor(W + 30, H + 40, 480)
nx, ny = (xx / W - 0.5) * 2, (yy / H - 0.5) * 2
vinheta = np.clip(1 - 0.38 * (nx ** 2 * 0.9 + ny ** 2 * 0.6), 0, 1)

for nome in ('campo2.png', 'campo.png', 'campo3.png', 'campo4.png'):
    a = arr(nome)
    rgb = a[..., :3]
    # tira as linhas: troca pelos vizinhos (mediana)
    im = Image.fromarray((rgb * 255).astype(np.uint8))
    fundo = np.asarray(im.filter(ImageFilter.MedianFilter(9))).astype(np.float32) / 255
    m = np.clip(linha * 1.6, 0, 1)[..., None]
    m = np.asarray(Image.fromarray((m[..., 0] * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3))).astype(np.float32)[..., None] / 255
    rgb = rgb * (1 - m) + fundo * m
    # grama de noite: menos amarelo, mais escura, contraste das listras um pouco maior
    l = lum(rgb)[..., None]
    rgb = l + (rgb - l) * 0.95
    rgb = rgb * np.array([0.70, 0.86, 0.70], np.float32)
    media = rgb.mean(axis=(0, 1), keepdims=True)
    rgb = media + (rgb - media) * 1.6
    rgb = rgb * (0.66 + 0.34 * luz[..., None]) * vinheta[..., None]
    rgb += np.array([1.0, 0.85, 0.55], np.float32) * (0.05 * luz[..., None])  # brilho quente
    # linhas creme
    cor = np.array([0.95, 0.93, 0.86], np.float32)
    al = (np.clip(linha * 1.15, 0, 1) * (0.62 + 0.25 * luz))[..., None]
    rgb = rgb * (1 - al) + cor * al
    rgb = np.clip(rgb, 0, 1)
    res = np.dstack([rgb, a[..., 3:4]])
    Image.fromarray((res * 255).round().astype(np.uint8), 'RGBA').save(os.path.join(out, nome), optimize=True)
    print('ok', nome)
