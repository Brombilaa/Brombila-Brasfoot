# Folhas do boneco robusto (boneco2.py) para a pagina de escolha:
#   retrato.jpg    um jogador grande, luz de estudio (como a imagem de referencia)
#   uniformes.jpg  cinco uniformes, peles e cabelos diferentes
#   giro.jpg       frente, 3/4, lado e costas com o numero
#   corrida/       quadros da corrida (vira mp4 com ffmpeg)
# Uso: python3 folhas2.py pasta_de_saida [retrato|uniformes|giro|corrida ...]
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw
import boneco2 as b

def fundo(w, h):
    """degrade verde escuro de estudio, mais claro atras do jogador"""
    ys, xs = np.mgrid[0:h, 0:w]
    r = np.hypot((xs - w / 2) / (w * 0.6), (ys - h * 0.45) / (h * 0.7))
    k = np.clip(1 - r, 0, 1)[..., None]
    c = np.array([0.05, 0.09, 0.07]) + k * np.array([0.08, 0.17, 0.11])
    im = Image.fromarray((np.clip(c, 0, 1) * 255).astype(np.uint8)).convert("RGBA")
    return im

UNIFORMES = [
    b.Uniforme("#c8102e", "#141414", "#f4f4f2", "#141414", "faixas", pele="#8d5a3b", cabelo="#1c1410", corte="black", numero=9),
    b.Uniforme("#f4f4f2", "#111111", "#111111", "#f4f4f2", "listras", pele="#f1c7a1", cabelo="#6b4a2a", corte="curto", numero=10),
    b.Uniforme("#0f7a3c", "#f4f4f2", "#f4f4f2", "#0f7a3c", "liso", pele="#c98e64", cabelo="#0e0b09", corte="topete", numero=7),
    b.Uniforme("#f4f4f2", "#141414", "#141414", "#141414", "faixa", pele="#5b3a26", cabelo="#2a1a12", corte="raspado", numero=5),
    b.Uniforme("#f2c14e", "#1a1a1a", "#1a1a1a", "#f2c14e", "liso", pele="#e0ac84", cabelo="#a07a4a", corte="curto", numero=1),
]

def retrato(saida):
    u = b.Uniforme("#c8102e", "#141414", "#f4f4f2", "#141414", "faixas", pele="#c98e64", cabelo="#1c1410", corte="curto", numero=9)
    s = b.render(b.Jogador(u, b.pose()), 0.45, 0.1, 760, aa=2)
    im = fundo(s.size[0] + 200, s.size[1])
    im.alpha_composite(s, (100, 0))
    im.convert("RGB").save(saida, quality=90)

def uniformes(saida):
    spr = [b.render(b.Jogador(u, b.pose()), 0.5, 0.18, 380, aa=2) for u in UNIFORMES]
    passo = spr[0].size[0] - 70
    im = fundo(passo * (len(spr) - 1) + spr[0].size[0] + 40, spr[0].size[1])
    for i, s in enumerate(spr): im.alpha_composite(s, (20 + i * passo, 0))
    im.convert("RGB").save(saida, quality=90)

def giro(saida):
    u = UNIFORMES[1]
    spr = [b.render(b.Jogador(u, b.pose()), a, 0.15, 400, aa=2) for a in (0.0, 0.8, math.pi / 2, math.pi)]
    passo = spr[0].size[0] - 40
    im = fundo(passo * 3 + spr[0].size[0] + 40, spr[0].size[1])
    for i, s in enumerate(spr): im.alpha_composite(s, (20 + i * passo, 0))
    im.convert("RGB").save(saida, quality=90)

def corrida(pasta, n=16, parte=0, partes=1):
    os.makedirs(pasta, exist_ok=True)
    u = UNIFORMES[0]
    for i in range(parte, n, partes):
        f = 2 * math.pi * i / n
        a = b.render(b.Jogador(u, b.pose(f, True)), math.pi / 2, 0.15, 380, aa=2)
        c = b.render(b.Jogador(u, b.pose(f + 1.0, True)), 2.5, 0.3, 380, aa=2)
        im = fundo(a.size[0] + c.size[0], a.size[1])
        im.alpha_composite(a, (0, 0)); im.alpha_composite(c, (a.size[0], 0))
        im.convert("RGB").save(os.path.join(pasta, "q%03d.png" % i))
        print("quadro", i, flush=True)

if __name__ == "__main__":
    pasta = sys.argv[1]
    for o in sys.argv[2:] or ["retrato", "uniformes", "giro", "corrida"]:
        if o.startswith("corrida"):
            k = o.split(":")[1:]
            corrida(os.path.join(pasta, "corrida"), parte=int(k[0]) if k else 0, partes=int(k[1]) if k else 1)
        else: globals()[o](os.path.join(pasta, o + ".jpg"))
        print("ok", o, flush=True)
