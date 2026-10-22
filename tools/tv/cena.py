# Cena de teste da camera de TV: campo em perspectiva (vista da lateral, do alto da
# arquibancada) com 22 bonecos de boneco.py. So para mostrar os estilos ao Leo.
# Uso: python3 cena.py pasta_de_saida
import math, os, random, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import boneco as b

W, H = 1280, 720
CAM = np.array([62.0, 26.0, -55.0])
ALVO = np.array([62.0, 0.0, 25.0])
FOCO = 2350.0

def base_camera(cam, alvo):
    f = alvo - cam; f /= np.linalg.norm(f)
    r = np.cross([0, 1.0, 0], f); r /= np.linalg.norm(r)     # x da tela = x do mundo
    u = np.cross(f, r)
    return r, u, f

def projeta(p, cam=None, alvo=None):
    cam = CAM if cam is None else cam
    alvo = ALVO if alvo is None else alvo
    r, u, f = base_camera(cam, alvo)
    d = np.asarray(p, float) - cam
    z = d @ f
    return np.stack([W / 2 + FOCO * (d @ r) / z, H / 2 - FOCO * (d @ u) / z, z], -1)

def gramado():
    r, u, f = base_camera(CAM, ALVO)
    ys, xs = np.mgrid[0:H, 0:W]
    dx = (xs - W / 2) / FOCO; dy = -(ys - H / 2) / FOCO
    ray = f[None, None, :] + dx[..., None] * r + dy[..., None] * u
    t = -CAM[1] / np.minimum(ray[..., 1], -1e-6)
    p = CAM + ray * t[..., None]
    chao = ray[..., 1] < -1e-4
    faixa = (np.floor(p[..., 0] / 5.25) % 2) == 0
    a = np.array([0.13, 0.42, 0.19]); c = np.array([0.11, 0.37, 0.165])
    cor = np.where(faixa[..., None], a, c)
    # vinheta e textura leve
    ruido = np.random.default_rng(3).normal(0, 0.012, (H, W, 1))
    dist = np.clip((t - 40) / 120, 0, 1)[..., None]
    cor = cor * (1 - 0.25 * dist) + ruido
    fora = (p[..., 2] < -4) | (p[..., 2] > 72) | (p[..., 0] < -6) | (p[..., 0] > 111)
    cor[fora] *= 0.75
    img = np.where(chao[..., None], cor, 0)
    return np.clip(img, 0, 1), chao, p

def linhas(d, esc):
    def poli(pts, w=2.2):
        pr = [projeta([x, 0, z]) for x, z in pts]
        pr = [(q[0] * esc, q[1] * esc) for q in pr if q[2] > 1]
        if len(pr) > 1: d.line(pr, fill=(235, 240, 230, 210), width=int(w * esc), joint="curve")
    poli([(0, 0), (105, 0), (105, 68), (0, 68), (0, 0)])
    poli([(52.5, 0), (52.5, 68)])
    poli([(52.5 + 9.15 * math.cos(a), 34 + 9.15 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 80)])
    for x0, s in ((0, 1), (105, -1)):
        poli([(x0, 13.85), (x0 + s * 16.5, 13.85), (x0 + s * 16.5, 54.15), (x0, 54.15)])
        poli([(x0, 24.85), (x0 + s * 5.5, 24.85), (x0 + s * 5.5, 43.15), (x0, 43.15)])

def arquibancada(img):
    # metade de cima: torcida escura com pontinhos e o brilho dos refletores
    rng = np.random.default_rng(7)
    topo = int(H * 0.0)
    q = projeta([52, 0, 76])[1]
    hz = int(max(40, q))
    a = np.zeros((hz, W, 3))
    grad = np.linspace(0.05, 0.11, hz)[:, None, None]
    a[:] = grad * np.array([0.9, 0.95, 1.0])
    pts = rng.random((hz * W // 9, 2))
    cores = rng.choice([[0.9, 0.9, 0.85], [0.85, 0.2, 0.2], [0.95, 0.95, 0.95], [0.2, 0.2, 0.25], [0.95, 0.75, 0.2]], len(pts))
    yy = (pts[:, 0] * (hz - 30)).astype(int); xx = (pts[:, 1] * W).astype(int)
    a[yy, xx] = a[yy, xx] * 0.3 + np.array(cores) * 0.35
    img[:hz] = a
    # placas de publicidade
    img[hz - 26:hz - 6] = np.array([0.08, 0.09, 0.09])
    return img, hz

def cena(estilo, saida, hud=True):
    img, chao, p = gramado()
    img, hz = arquibancada(img)
    im = Image.fromarray((img * 255).astype(np.uint8)).convert("RGBA")
    # placas com texto dourado
    d = ImageDraw.Draw(im)
    fonte = ImageFont.truetype(os.path.join(os.path.dirname(__file__), "..", "..", "mod", "assets", "mod", "fontes", "SairaExtraCondensed-ExtraBold.ttf"), 15)
    for i, x in enumerate(range(10, W, 240)):
        d.text((x, hz - 25), ["BROMBILA BRASFOOT", "SÉRIE D 2023", "BROMBILA BRASFOOT", "CAMPEONATO BRASILEIRO"][i % 4], fill=(242, 193, 78), font=fonte)
    # linhas em 2x
    esc = 2
    camada = Image.new("RGBA", (W * esc, H * esc), (0, 0, 0, 0))
    linhas(ImageDraw.Draw(camada), esc)
    camada = camada.resize((W, H), Image.LANCZOS)
    im.alpha_composite(camada)
    # luz dos refletores
    brilho = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dg = ImageDraw.Draw(brilho)
    for x in (120, 1160):
        dg.ellipse([x - 160, -160, x + 160, 120], fill=(255, 245, 210, 40))
    im.alpha_composite(brilho.filter(ImageFilter.GaussianBlur(60)))

    casa = b.Uniforme("#f4f4f2", "#111111", "#111111", "#f4f4f2", "listras")
    fora = b.Uniforme("#c8102e", "#f4f4f2", "#f4f4f2", "#c8102e", "liso")
    gol_c = b.Uniforme("#2b2e2c", "#f2c14e", "#2b2e2c", "#2b2e2c", "liso")
    gol_f = b.Uniforme("#f2c14e", "#1a1a1a", "#1a1a1a", "#f2c14e", "liso")
    peles = ["#f1c7a1", "#c98e64", "#8d5a3b", "#5b3a26", "#e0ac84"]
    cabelos = ["#1c1410", "#3b2a1c", "#6b4a2a", "#0e0b09", "#a07a4a"]
    rng = random.Random(11)
    # momento: casa atacando para a direita, bola no meio-campo ofensivo
    bola = np.array([66.0, 0.11, 30.0])
    casa_pos = [(8, 34), (38, 12), (36, 27), (37, 41), (40, 58), (55, 22), (52, 38), (60, 50), (68, 14), (66, 31), (74, 44)]
    fora_pos = [(99, 34), (80, 16), (82, 29), (81, 41), (78, 55), (72, 24), (71, 37), (66, 47), (60, 30), (58, 44), (54, 15)]
    jogadores = []
    for i, (x, z) in enumerate(casa_pos):
        u = (gol_c if i == 0 else casa)
        ang = math.atan2(1.0, -0.25 * rng.uniform(-1, 1)) if i else math.atan2(0.4, 0.0)
        jogadores.append((x, z, u, i + 1, ang, i not in (0, 1)))
    for i, (x, z) in enumerate(fora_pos):
        u = (gol_f if i == 0 else fora)
        alvo = math.atan2(bola[0] - x, -(bola[2] - z))
        jogadores.append((x, z, u, i + 1, alvo, i not in (0,)))
    # ordem: de tras para frente
    jogadores.sort(key=lambda j: -j[1])
    for k, (x, z, u, num, giro, corre) in enumerate(jogadores):
        pe = projeta([x, 0, z]); cab = projeta([x, 1.85, z])
        alt = pe[1] - cab[1]
        if pe[2] < 1: continue
        incl = math.atan2(CAM[1], pe[2])
        uu = b.Uniforme.__new__(b.Uniforme); uu.__dict__.update(u.__dict__)
        uu.pele = b.hexrgb(peles[(num * 7 + k) % 5]); uu.cabelo = b.hexrgb(cabelos[(num * 3 + k) % 5])
        uu.corte = ["curto", "black", "raspado", "topete", "curto"][(num * 5 + k) % 5]
        uu.com_numero(num)
        spr = b.render(estilo, uu, giro, incl, alt, fase=rng.uniform(0, 6.28), correndo=corre, ss=4)
        sw, sh = spr.size
        im.alpha_composite(spr, (int(pe[0] - sw / 2), int(pe[1] - sh * 0.86)))
    # bola
    pb = projeta(bola); rb = max(3, FOCO * 0.22 / pb[2] / 2)
    sb = Image.new("RGBA", (W, H), (0, 0, 0, 0)); ds = ImageDraw.Draw(sb)
    ds.ellipse([pb[0] - rb * 1.3 + 2, pb[1] + rb * 0.4 - rb * 0.4, pb[0] + rb * 1.3 + 2, pb[1] + rb * 0.4 + rb * 0.4], fill=(0, 0, 0, 90))
    im.alpha_composite(sb.filter(ImageFilter.GaussianBlur(1.5)))
    d = ImageDraw.Draw(im)
    d.ellipse([pb[0] - rb, pb[1] - rb * 2, pb[0] + rb, pb[1]], fill=(250, 250, 248), outline=(40, 40, 40))
    if hud:
        placar(im)
    im.convert("RGB").save(saida, quality=88)

def placar(im):
    d = ImageDraw.Draw(im)
    ft = ImageFont.truetype(os.path.join(os.path.dirname(__file__), "..", "..", "mod", "assets", "mod", "fontes", "Manrope-Bold.ttf"), 17)
    fn = ImageFont.truetype(os.path.join(os.path.dirname(__file__), "..", "..", "mod", "assets", "mod", "fontes", "SairaExtraCondensed-ExtraBold.ttf"), 26)
    x, y = 28, 24
    d.rounded_rectangle([x, y, x + 300, y + 40], 20, fill=(10, 12, 11, 225), outline=(242, 193, 78, 150))
    d.rectangle([x + 18, y + 12, x + 24, y + 28], fill=(244, 244, 242)); d.rectangle([x + 24, y + 12, x + 30, y + 28], fill=(17, 17, 17))
    d.text((x + 40, y + 9), "ABC", fill=(241, 238, 231), font=ft)
    d.text((x + 100, y + 4), "1  x  0", fill=(242, 193, 78), font=fn)
    d.rectangle([x + 172, y + 12, x + 184, y + 28], fill=(200, 16, 46))
    d.text((x + 192, y + 9), "SOU", fill=(241, 238, 231), font=ft)
    d.text((x + 248, y + 9), "63'", fill=(158, 155, 146), font=ft)

if __name__ == "__main__":
    pasta = sys.argv[1]
    for e in sys.argv[2:] or ["lowpoly", "toon", "realista"]:
        cena(e, os.path.join(pasta, "cena-%s.jpg" % e))
        print("ok", e)
