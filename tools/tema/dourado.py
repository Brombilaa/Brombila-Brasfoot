"""Recolore as imagens do tema de verde neon para o dourado "Noite de estreia".

Uso: python3 dourado.py arquivo.png [...]   (sobrescreve cada arquivo)

Pixels verdes (matiz 70-170 graus) viram ambar/dourado (#F2C14E) mantendo o
brilho e a transparencia; verdes muito escuros (fundos) viram quase preto
neutro. As outras cores (vermelho, prata, amarelo, cores de clubes) ficam.
"""
import colorsys
import sys

from PIL import Image

OURO_H = 41 / 360.0  # matiz do #F2C14E


def recolore(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    if s < 0.12 or not (70 / 360.0 <= h <= 170 / 360.0):
        return r, g, b
    if v < 0.35:
        # fundo verde escuro -> escuro neutro com um toque quente
        s2 = min(s, 0.25) * 0.5
        v2 = v * 0.85
    else:
        s2 = min(1.0, s * 0.9)
        v2 = min(1.0, v * 0.95)
    r2, g2, b2 = colorsys.hsv_to_rgb(OURO_H, s2, v2)
    return round(r2 * 255), round(g2 * 255), round(b2 * 255)


def processa(caminho):
    img = Image.open(caminho)
    modo = img.mode
    rgba = img.convert("RGBA")
    px = rgba.load()
    cache = {}
    for y in range(rgba.height):
        for x in range(rgba.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            k = (r, g, b)
            if k not in cache:
                cache[k] = recolore(r, g, b)
            px[x, y] = cache[k] + (a,)
    if modo in ("RGB", "L"):
        rgba = rgba.convert("RGB")
    if caminho.lower().endswith((".jpg", ".jpeg")):
        rgba.convert("RGB").save(caminho, quality=90)
    else:
        rgba.save(caminho)


if __name__ == "__main__":
    for c in sys.argv[1:]:
        processa(c)
