"""Gera os fundos img/f01.jpg (estadio) e img/f02.jpg (textura) no tema dourado.

Uso: python3 fundos.py <pasta img do Brasfoot original> <pasta img de saida>
"""
import sys

from PIL import Image, ImageEnhance, ImageOps

orig, saida = sys.argv[1], sys.argv[2]

# f01: foto do estadio, quase sem cor e escurecida, com um toque quente
f = Image.open(orig + "/f01.jpg").convert("RGB")
f = ImageEnhance.Color(f).enhance(0.35)
f = ImageEnhance.Brightness(f).enhance(0.55)
calor = Image.new("RGB", f.size, (40, 30, 12))
f = Image.blend(f, calor, 0.08)
f.save(saida + "/f01.jpg", quality=90)

# f02: textura "lona" em cinza quase preto levemente quente
t = Image.open(orig + "/f02.jpg").convert("L")
t = ImageOps.autocontrast(t)
t = ImageOps.colorize(t, black=(8, 9, 8), white=(46, 42, 34))
t.save(saida + "/f02.jpg", quality=90)
