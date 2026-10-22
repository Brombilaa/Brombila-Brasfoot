# Estrelas de potencial (CPE) dos juniores: aiconsj/e1.png ... e5.png (100x16).
# O original tinha fundo branco; aqui: fundo transparente, estrelas douradas cheias,
# meia estrela e as que faltam so com contorno apagado. Desenhado em 8x e reduzido.
# Uso: python3 estrelas.py <pasta de saida>
import sys, os, math
from PIL import Image, ImageDraw

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
K = 8
W, H = 100 * K, 16 * K
OURO = (242, 193, 78, 255)
APAGADO = (244, 241, 234, 70)

def estrela(cx, cy, r):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.45
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts

for nome, n in [('e1', 1), ('e15', 1.5), ('e2', 2), ('e25', 2.5), ('e3', 3), ('e35', 3.5),
                ('e4', 4), ('e45', 4.5), ('e5', 5)]:
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    for i in range(5):
        cx, cy, r = (10 + i * 19.5) * K, 8.4 * K, 7.4 * K
        p = estrela(cx, cy, r)
        camada = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(camada)
        if i + 1 <= n:
            d.polygon(p, fill=OURO)
        else:
            d.line(p + [p[0]], fill=APAGADO, width=int(1.1 * K), joint='curve')
            if i + 0.5 <= n:  # meia estrela: metade esquerda cheia
                m = Image.new('L', (W, H), 0)
                ImageDraw.Draw(m).rectangle([0, 0, cx, H], fill=255)
                cheia = Image.new('RGBA', (W, H), (0, 0, 0, 0))
                ImageDraw.Draw(cheia).polygon(p, fill=OURO)
                camada.paste(cheia, (0, 0), Image.composite(cheia, Image.new('RGBA', (W, H)), m).getchannel('A'))
        im = Image.alpha_composite(im, camada)
    im.resize((100, 16), Image.LANCZOS).save(os.path.join(out, nome + '.png'), optimize=True)
    print('ok', nome)
