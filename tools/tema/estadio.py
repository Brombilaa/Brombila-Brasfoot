# Fundo da janela Estadio (aicons/estadio.jpg, 598x328) no estilo "Noite de estreia".
# O jogo desenha os numeros e os campos por cima nas mesmas posicoes do original
# (valores em x=165, precos em x=460, linhas em y=122/150/177/204), entao aqui ficam
# so a foto escurecida, dois cartoes de vidro, os titulos e os nomes das linhas.
# Uso: python3 estadio.py <repo> <saida.jpg>
import sys, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

repo, saida = sys.argv[1], sys.argv[2]
K = 3
W, H = 598, 328
fontes = os.path.join(repo, 'mod/assets/mod/fontes')
def f(nome, px): return ImageFont.truetype(os.path.join(fontes, nome), int(px * K))

# foto do estadio: recorte da arquibancada, escurecida e aquecida
foto = Image.open(os.path.join(repo, 'mod/assets/mod/estadio.jpg')).convert('RGB')
fw, fh = foto.size
rec = foto.crop((int(fw * 0.12), int(fh * 0.10), int(fw * 0.12) + int(fh * 0.62 * W / H), int(fh * 0.72)))
img = rec.resize((W * K, H * K), Image.LANCZOS)
img = ImageEnhance.Color(img).enhance(0.55)
img = ImageEnhance.Brightness(img).enhance(0.8)
img = img.filter(ImageFilter.GaussianBlur(1.2 * K))

# veu escuro: mais forte embaixo e nas laterais
veu = Image.new('RGBA', img.size, (0, 0, 0, 0))
d = ImageDraw.Draw(veu)
for y in range(H * K):
    t = y / (H * K)
    d.line([(0, y), (W * K, y)], fill=(6, 9, 8, int(70 + 140 * t)))
img = Image.alpha_composite(img.convert('RGBA'), veu)

# luz de refletor no alto
luz = Image.new('RGBA', img.size, (0, 0, 0, 0))
ImageDraw.Draw(luz).ellipse([W * K * 0.15, -H * K * 0.6, W * K * 0.85, H * K * 0.35], fill=(242, 193, 78, 40))
img = Image.alpha_composite(img, luz.filter(ImageFilter.GaussianBlur(40 * K)))

def cartao(x0, y0, x1, y1):
    global img
    s = Image.new('RGBA', img.size, (0, 0, 0, 0))
    ImageDraw.Draw(s).rounded_rectangle([x0 * K, (y0 + 6) * K, x1 * K, (y1 + 6) * K], radius=16 * K, fill=(0, 0, 0, 120))
    img = Image.alpha_composite(img, s.filter(ImageFilter.GaussianBlur(10 * K)))
    c = Image.new('RGBA', img.size, (0, 0, 0, 0))
    dc = ImageDraw.Draw(c)
    dc.rounded_rectangle([x0 * K, y0 * K, x1 * K, y1 * K], radius=14 * K, fill=(18, 22, 20, 205),
                         outline=(244, 241, 234, 34), width=K)
    dc.line([((x0 + 16) * K, y0 * K + 1), ((x1 - 16) * K, y0 * K + 1)], fill=(242, 193, 78, 150), width=2 * K)
    img = Image.alpha_composite(img, c)

cartao(24, 62, 286, 238)
cartao(312, 62, 574, 258)
ov = Image.new('RGBA', img.size, (0, 0, 0, 0))  # textos e filetes numa camada (mistura certa)
d = ImageDraw.Draw(ov)

def texto(x, y, s, fonte, cor, ancora='la', espaco=0):
    if espaco:
        # letras espacadas
        tot = sum(d.textlength(ch, font=fonte) for ch in s) + espaco * K * (len(s) - 1)
        xx = x * K - (tot if ancora[0] == 'r' else 0)
        for ch in s:
            d.text((xx, y * K), ch, font=fonte, fill=cor, anchor='l' + ancora[1])
            xx += d.textlength(ch, font=fonte) + espaco * K
    else:
        d.text((x * K, y * K), s, font=fonte, fill=cor, anchor=ancora)

OURO, CREME, FRACO = (242, 193, 78), (244, 241, 234), (244, 241, 234, 165)
tit = f('SairaExtraCondensed-ExtraBold.ttf', 21)
rot = f('Manrope-Bold.ttf', 9)
lin = f('Manrope-Medium.ttf', 12.5)
texto(42, 84, 'CAPACIDADE ATUAL', tit, CREME, 'ls')
texto(42, 100, 'LUGARES POR SETOR', rot, OURO, 'ls', espaco=1.6)
texto(330, 84, 'PREÇO DOS INGRESSOS', tit, CREME, 'ls')
texto(330, 100, 'VALOR POR SETOR', rot, OURO, 'ls', espaco=1.6)

# linhas: rotulo a direita alinhado antes do valor, com filete entre elas
for (xr, ys, x0, x1) in [(155, (122, 150, 177, 204), 40, 270), (450, (122, 150, 180, 210), 328, 558)]:
    for i, (y, nome) in enumerate(zip(ys, ('Geral', 'Arquibancada', 'Cadeira', 'Camarote'))):
        texto(xr, y + 9.5, nome, lin, FRACO, 'rm')
        if i < 3:
            yy = (y + ys[i + 1]) / 2 + 9.5
            d.line([(x0 * K, yy * K), (x1 * K, yy * K)], fill=(244, 241, 234, 18), width=K)

texto(444, 286, 'Preços sugeridos:', lin, FRACO, 'rm')

img = Image.alpha_composite(img, ov)
img = img.convert('RGB').resize((W, H), Image.LANCZOS)
img.save(saida, quality=93, optimize=True)
print('ok', saida)
