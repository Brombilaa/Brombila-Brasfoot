# Tira o carimbo "CAMPEAO!" do fundo da janela "E campeao!" (aicons/campeao.png):
# o titulo passa a ser escrito pelo mod na fonte do menu. O resto (confetes, taca e
# podio) fica igual.
# Uso: python3 tools/tema/campeao.py <campeao.png original> mod/assets/aicons/campeao.png
import sys
import numpy as np
from PIL import Image, ImageFilter

im = Image.open(sys.argv[1]).convert("RGBA")
a = np.array(im).astype(float)
h, w = a.shape[:2]
x0, x1, y0, y1 = 52, 252, 22, 80
# fundo quase preto do proprio quadro, com borda suave
fundo = np.array([5, 2, 1, 255], float)
yy, xx = np.mgrid[0:h, 0:w]
dx = np.maximum(np.maximum(x0 - xx, xx - x1), 0)
dy = np.maximum(np.maximum(y0 - yy, yy - y1), 0)
d = np.hypot(dx, dy)
peso = np.clip(1 - d / 10.0, 0, 1)[..., None]
a = a * (1 - peso) + fundo * peso
Image.fromarray(a.clip(0, 255).astype(np.uint8)).save(sys.argv[2])
