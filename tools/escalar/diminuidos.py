# Gera os gramados pequenos (aiconsj/campo_diminuido*.jpg, 340x566) do Time da Rodada,
# Time do Ano e detalhes da partida a partir dos gramados novos do Escalar time
# (aicons/campo*.png), encaixando as linhas do campo onde o jogo original as tem.
# Uso: python3 diminuidos.py <pasta aicons> <pasta de saida>
import sys, os
import numpy as np
from PIL import Image

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
# linhas externas: no gramado novo (434x650) e no pequeno original (340x566)
NX0, NY0, NX1, NY1 = 18, 20, 414, 625
PX0, PY0, PX1, PY1 = 12, 18, 328, 545
sx, sy = (PX1 - PX0) / (NX1 - NX0), (PY1 - PY0) / (NY1 - NY0)
M = 12  # o gramado novo tem pouca margem em cima: espelha a borda
caixa = (M + NX0 - PX0 / sx, M + NY0 - PY0 / sy, M + NX1 + (340 - PX1) / sx, M + NY1 + (566 - PY1) / sy)
for novo, velho in [("campo2", "campo_diminuido2"), ("campo", "campo_diminuido"),
                    ("campo3", "campo_diminuido3"), ("campo4", "campo_diminuido4")]:
    im = Image.open(os.path.join(src, novo + ".png")).convert("RGB")
    im = Image.fromarray(np.pad(np.asarray(im), ((M, M), (M, M), (0, 0)), mode="reflect"))
    im = im.resize((340, 566), Image.LANCZOS, box=caixa)
    im.save(os.path.join(out, velho + ".jpg"), quality=92)
