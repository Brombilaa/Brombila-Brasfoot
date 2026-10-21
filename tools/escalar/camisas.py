# Camisas genericas da tela Escalar time (50x50, desenhadas em 8x e reduzidas):
#  camisav.png - posicao vazia: silhueta de vidro com contorno creme e um "+";
#  camisag.png - goleiro: grafite com gola e punhos dourados e borda clara (aparece no campo escuro).
# Uso: python3 camisas.py <pasta de saida>
import sys, os
from PIL import Image, ImageDraw

out = sys.argv[1]
K = 8
N = 50 * K
P = [(0.38, 0.12), (0.22, 0.16), (0.06, 0.37), (0.17, 0.46), (0.27, 0.39), (0.28, 0.88),
     (0.72, 0.88), (0.73, 0.39), (0.83, 0.46), (0.94, 0.37), (0.78, 0.16), (0.62, 0.12), (0.5, 0.21)]
pts = [(x * N, y * N) for x, y in P]

def contorno(d, cor, w):
    d.line(pts + [pts[0]], fill=cor, width=w, joint='curve')
    for p in pts:
        r = w / 2
        d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=cor)

def salva(im, nome):
    im.resize((50, 50), Image.LANCZOS).save(os.path.join(out, nome), optimize=True)
    print('ok', nome)

# vazia
im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.polygon(pts, fill=(244, 241, 234, 30))
contorno(d, (244, 241, 234, 150), int(1.3 * K))
c, t, l = N / 2, 0.9 * K, 5 * K
d.rounded_rectangle([c - l, N * 0.52 - t, c + l, N * 0.52 + t], radius=t, fill=(242, 193, 78, 230))
d.rounded_rectangle([c - t, N * 0.52 - l, c + t, N * 0.52 + l], radius=t, fill=(242, 193, 78, 230))
salva(im, 'camisav.png')

# goleiro
im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
corpo = Image.new('RGBA', (N, N), (0, 0, 0, 0))
g = ImageDraw.Draw(corpo)
for y in range(N):  # degrade vertical grafite
    k = y / N
    g.line([(0, y), (N, y)], fill=(int(70 - 30 * k), int(76 - 32 * k), int(82 - 34 * k), 255))
m = Image.new('L', (N, N), 0)
ImageDraw.Draw(m).polygon(pts, fill=255)
im.paste(corpo, (0, 0), m)
d = ImageDraw.Draw(im)
ouro = (242, 193, 78, 255)
d.line([pts[0], pts[12], pts[11]], fill=ouro, width=int(2.2 * K), joint='curve')   # gola em V
d.line([pts[2], pts[3]], fill=ouro, width=int(2.4 * K))                            # punho esq.
d.line([pts[8], pts[9]], fill=ouro, width=int(2.4 * K))                            # punho dir.
d.line([(0.33 * N, 0.5 * N), (0.67 * N, 0.5 * N)], fill=(242, 193, 78, 70), width=int(5 * K))
contorno(d, (244, 241, 234, 110), int(1.1 * K))
salva(im, 'camisag.png')
