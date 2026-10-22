# Folhas de comparacao dos estilos para a pagina de escolha:
#   uniformes-<estilo>.jpg  cinco uniformes lado a lado (3/4 de frente)
#   giro-<estilo>.jpg       o mesmo jogador de frente, de lado, de costas e de 3/4
#   corrida-<estilo>/       quadros da corrida (vira mp4 com ffmpeg)
#   perto-<estilo>.jpg      camera de replay, mais perto da bola
# Uso: python3 folhas.py pasta_de_saida
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw
import boneco as b
import cena

FUNDO = (18, 22, 20, 255)

def fundo(w, h):
    im = Image.new("RGBA", (w, h), FUNDO)
    # gramado escuro embaixo, como um estudio
    d = ImageDraw.Draw(im)
    for y in range(h):
        k = max(0, (y - h * 0.55) / (h * 0.45))
        d.line([(0, y), (w, y)], fill=(int(18 + 10 * k), int(22 + 50 * k), int(20 + 18 * k), 255))
    return im

UNIFORMES = [
    ("Listras", b.Uniforme("#f4f4f2", "#111111", "#111111", "#f4f4f2", "listras"), 10),
    ("Rubro-negro", b.Uniforme("#c8102e", "#141414", "#f4f4f2", "#141414", "faixas", pele="#8d5a3b", corte="black"), 9),
    ("Liso", b.Uniforme("#0f7a3c", "#f4f4f2", "#f4f4f2", "#0f7a3c", "liso", pele="#f1c7a1", cabelo="#6b4a2a", corte="topete"), 7),
    ("Faixa", b.Uniforme("#f4f4f2", "#141414", "#141414", "#141414", "faixa", pele="#5b3a26", cabelo="#2a1a12", corte="raspado"), 5),
    ("Goleiro", b.Uniforme("#f2c14e", "#1a1a1a", "#1a1a1a", "#f2c14e", "liso", pele="#e0ac84", cabelo="#a07a4a"), 1),
]

def uniformes(estilo, saida):
    alt = 300
    spr = [b.render(estilo, u.com_numero(n), 0.55, 0.3, alt) for _, u, n in UNIFORMES]
    w = sum(s.size[0] for s in spr) - 60 * (len(spr) - 1) + 40
    im = fundo(w, spr[0].size[1] + 20)
    x = 20
    for s in spr:
        im.alpha_composite(s, (x, 10)); x += s.size[0] - 60
    im.convert("RGB").save(saida, quality=88)

def giro(estilo, saida):
    u = UNIFORMES[0][1].com_numero(10)
    angulos = [0.0, 0.8, math.pi / 2, math.pi]
    spr = [b.render(estilo, u, a, 0.25, 340) for a in angulos]
    w = sum(s.size[0] for s in spr) - 40 * 3 + 40
    im = fundo(w, spr[0].size[1] + 20)
    x = 20
    for s in spr:
        im.alpha_composite(s, (x, 10)); x += s.size[0] - 40
    im.convert("RGB").save(saida, quality=88)

def corrida(estilo, pasta, n=16):
    os.makedirs(pasta, exist_ok=True)
    u = UNIFORMES[1][1].com_numero(9)
    for i in range(n):
        fase = 2 * math.pi * i / n
        a = b.render(estilo, u, math.pi / 2, 0.22, 300, fase=fase, correndo=True)
        c = b.render(estilo, u, 2.4, 0.3, 300, fase=fase + 1.0, correndo=True)
        im = fundo(a.size[0] + c.size[0] + 20, a.size[1] + 20)
        im.alpha_composite(a, (10, 10)); im.alpha_composite(c, (a.size[0] + 10, 10))
        im.convert("RGB").save(os.path.join(pasta, "q%03d.png" % i))

def perto(estilo, saida):
    cena.CAM = np.array([70.0, 9.0, -14.0]); cena.ALVO = np.array([67.0, 0.0, 30.0]); cena.FOCO = 2500.0
    cena.cena(estilo, saida)

if __name__ == "__main__":
    pasta = sys.argv[1]
    for e in sys.argv[2:] or ["lowpoly", "toon", "realista"]:
        uniformes(e, os.path.join(pasta, "uniformes-%s.jpg" % e))
        giro(e, os.path.join(pasta, "giro-%s.jpg" % e))
        corrida(e, os.path.join(pasta, "corrida-%s" % e))
        print("ok", e)
