# Abas da janela Tabelas (aiconsb/b0..b4.png e b0s..b4s.png, 79x45):
# fundo transparente, texto em Manrope, a selecionada em creme com sublinhado dourado.
# Uso: python3 abas.py <repo> <pasta-saida>
import sys, os
from PIL import Image, ImageDraw, ImageFont

repo, saida = sys.argv[1], sys.argv[2]
K = 4
W, H = 79, 45
fonte = ImageFont.truetype(os.path.join(repo, 'mod/assets/mod/fontes/Manrope-SemiBold.ttf'), int(12.5 * K))
NOMES = ['nacional', 'internacional', 'estadual', 'seleções', 'regional']
for i, nome in enumerate(NOMES):
    for sel in (False, True):
        img = Image.new('RGBA', (W * K, H * K), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cor = (244, 241, 234, 255) if sel else (244, 241, 234, 150)
        tam = fonte
        while d.textlength(nome, font=tam) > (W - 6) * K:
            tam = ImageFont.truetype(tam.path, tam.size - K // 2)
        d.text((W * K / 2, 21 * K), nome, font=tam, fill=cor, anchor='mm')
        if sel:
            lw = min(d.textlength(nome, font=tam) / K, W - 12)
            x0 = (W - lw) / 2
            d.rounded_rectangle([x0 * K, 33 * K, (x0 + lw) * K, 35.5 * K], radius=K * 1.2, fill=(242, 193, 78, 255))
        img = img.resize((W, H), Image.LANCZOS)
        img.save(os.path.join(saida, 'b%d%s.png' % (i, 's' if sel else '')))
print('ok')
