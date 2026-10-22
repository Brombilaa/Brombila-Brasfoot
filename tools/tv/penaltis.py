# Poses da disputa de penaltis (batedor e goleiro) para a camera atras do batedor.
# Cada animacao e uma lista de poses; sprites.py gera o conjunto "pen" com elas.
#   python3 penaltis.py previa saida.png [animacao ...]   folha de conferencia
import math, sys
import numpy as np
from PIL import Image
import boneco2 as b

def mistura(a, c, t):
    o = {}
    for k in set(a) | set(c):
        va, vc = a.get(k, c.get(k)), c.get(k, a.get(k))
        if isinstance(va, (tuple, list)):
            o[k] = tuple(x + (y - x) * t for x, y in zip(va, vc))
        elif isinstance(va, bool) or va is None:
            o[k] = va if t < 0.5 else vc
        else:
            o[k] = va + (vc - va) * t
    return o

def base(**k):
    p = dict(quadril=(0.06, -0.04), joelho=(0.08, 0.1), ombro=(0.08, -0.06), cotovelo=(0.25, 0.3), tronco=0.03, pulo=0.0, abre=0.22)
    p.update(k); return p

# ---------------------------------------------------------------- batedor (chuta de direita: perna 0)
def corrida(n=12):
    return [b.pose(i * 2 * math.pi / n, True) for i in range(n)]

def chute(n=10):
    """quadros-chave da cobranca, interpolados em n quadros (o contato e o quadro 5)"""
    k = chave_chute()
    tempos = [0, 0.6, 1.2, 1.8, 2.4, 3.0, 3.5, 4.0, 4.5, 5.0]
    out = []
    for t in tempos[:n]:
        i = min(int(t), len(k) - 2)
        out.append(mistura(k[i], k[i + 1], t - i))
    return out

def chave_chute():
    k = []
    # 0 ultima passada: perna de apoio (1) a frente, direita atras
    k.append(base(quadril=(-0.55, 0.55), joelho=(0.9, 0.25), ombro=(0.6, -0.5), cotovelo=(0.6, 0.6), tronco=0.15, abre=(0.6, 0.5), apoia=True))
    # 1 apoio plantado, direita armando atras, bracos abrindo
    k.append(base(quadril=(-0.95, 0.35), joelho=(1.55, 0.35), ombro=(0.3, -0.3), cotovelo=(0.4, 0.4), tronco=0.05, abre=(1.4, 1.6), apoia=True, torce=0.25))
    # 2 descendo a perna
    k.append(base(quadril=(-0.2, 0.3), joelho=(1.3, 0.35), ombro=(0.1, 0.2), cotovelo=(0.3, 0.3), tronco=0.0, abre=(1.5, 1.6), apoia=True, torce=0.1))
    # 3 contato
    k.append(base(quadril=(0.55, 0.25), joelho=(0.35, 0.3), ombro=(-0.2, 0.4), cotovelo=(0.3, 0.3), tronco=-0.05, abre=(1.4, 1.5), apoia=True, pe=(-0.5, 0), torce=-0.1))
    # 4 continuacao alta
    k.append(base(quadril=(1.25, 0.15), joelho=(0.15, 0.2), ombro=(-0.4, 0.5), cotovelo=(0.3, 0.4), tronco=-0.15, abre=(1.2, 1.4), apoia=True, pe=(-0.6, 0), torce=-0.3))
    # 5 aterrissa
    k.append(base(quadril=(0.6, -0.2), joelho=(0.5, 0.3), ombro=(-0.1, 0.3), cotovelo=(0.4, 0.4), tronco=0.05, abre=(0.8, 0.8), apoia=True, torce=-0.2))
    return k

def festa():
    # comemora: punhos para o alto (de frente para a camera)
    k = []
    for i in range(4):
        s = math.sin(i * math.pi / 2)
        k.append(base(quadril=(0.25 * s, -0.25 * s), joelho=(0.3, 0.3), ombro=(-0.15, -0.15), cotovelo=(0.25 + 0.3 * s, 0.25 - 0.3 * s), tronco=-0.1, abre=(4.9 + 0.3 * s, 4.9 - 0.3 * s), abre2=(5.1, 5.1), apoia=True, pulo=0.05 * abs(s)))
    return k

def lamenta():
    # maos na cabeca
    return [base(quadril=(0.05, 0.0), joelho=(0.1, 0.1), ombro=(2.6, 2.6), cotovelo=(2.2, 2.2), tronco=0.25, abre=(1.5, 1.5), apoia=True),
            base(quadril=(0.15, -0.05), joelho=(0.25, 0.1), ombro=(2.5, 2.5), cotovelo=(2.3, 2.3), tronco=0.45, abre=(1.4, 1.4), apoia=True)]

# ---------------------------------------------------------------- goleiro (de frente para a camera)
# base aberta, joelhos dobrados e tronco curvado para a frente, bracos abertos na altura da cintura
PRONTO = base(quadril=(0.95, 0.95), joelho=(1.55, 1.55), ombro=(0.75, 0.75), cotovelo=(0.7, 0.7), tronco=0.55, abre=(1.05, 1.05), lado=(0.36, 0.36), apoia=True)

def pronto():
    k = []
    for i in range(4):
        s = math.sin(i * math.pi / 2)
        p = dict(PRONTO); p["quadril"] = (0.95 + 0.07 * s, 0.95 + 0.07 * s); p["joelho"] = (1.55 + 0.12 * s, 1.55 + 0.12 * s)
        p["abre"] = (1.05 + 0.1 * s, 1.05 + 0.1 * s)
        k.append(p)
    return k

def voo(lado):
    """lado +1: o goleiro se joga para a direita da tela (esquerda dele, x+ do boneco quando olha a camera)"""
    s = -lado
    k = []
    # impulso: quadril abaixa e o corpo inclina
    k.append(mistura(PRONTO, base(quadril=(0.7, 0.4), joelho=(1.2, 0.7), ombro=(0.6, 0.6), cotovelo=(0.4, 0.4), tronco=0.3, abre=(1.6, 1.6), lado=(0.25, 0.25), apoia=True, rolo=0.0), 0.7))
    for i, r in enumerate((0.35, 0.8, 1.2, 1.4, 1.5)):
        # bracos estendidos acima da cabeca na direcao do voo, pernas juntas atras
        k.append(base(quadril=(0.15, 0.35 - 0.05 * i), joelho=(0.2, 0.6 - 0.05 * i), ombro=(0.25, 0.25), cotovelo=(0.15, 0.15),
                      tronco=0.05, abre=(5.0, 5.0) if i >= 1 else (2.6, 2.6), abre2=(5.2, 5.2) if i >= 1 else (2.4, 2.4), lado=(0.05, 0.12), rolo=s * r))
    # no chao, deitado
    k.append(base(quadril=(0.35, 0.6), joelho=(0.5, 0.9), ombro=(0.5, 0.5), cotovelo=(0.6, 0.6), tronco=0.2, abre=(3.2, 3.2), rolo=s * 1.55))
    return k

def defende_meio():
    # fica no meio e encaixa a bola no peito
    return [PRONTO,
            base(quadril=(0.7, 0.7), joelho=(1.1, 1.1), ombro=(1.2, 1.2), cotovelo=(0.9, 0.9), tronco=0.35, abre=(0.5, 0.5), lado=(0.3, 0.3), apoia=True),
            base(quadril=(0.8, 0.8), joelho=(1.3, 1.3), ombro=(1.0, 1.0), cotovelo=(1.6, 1.6), tronco=0.45, abre=(0.35, 0.35), lado=(0.3, 0.3), apoia=True)]

ANIMS = [
    # nome, poses, giro (0 = de frente para a camera), goleiro?
    # giros medidos na camera da cena (mod.Penaltis): a corrida vai na diagonal ate a bola
    ("corrida", corrida(), math.pi - 0.66, False),
    ("chute", chute(), math.pi - 0.3, False),
    ("festa", festa(), -0.8, False),
    ("lamenta", lamenta(), math.pi - 0.3, False),
    ("pronto", pronto(), 0.0, True),
    ("voo_dir", voo(+1), 0.0, True),
    ("voo_esq", voo(-1), 0.0, True),
    ("meio", defende_meio(), 0.0, True),
    ("goleiro_festa", festa(), 0.0, True),
]
INCL = 0.12
LARGURA = 2.6

def previa(saida, nomes):
    uni = b.Uniforme("#c8102e", "#141414", "#f4f4f2", "#141414", "faixas", pele="#c98e64", cabelo="#1c1410", corte="curto", numero=9)
    gol = b.Uniforme("#2b2e2c", "#f2c14e", "#2b2e2c", "#2b2e2c", "liso", pele="#e0ac84", cabelo="#3b2a1c", corte="raspado", numero=1)
    linhas = []
    for nome, poses, giro, goleiro in ANIMS:
        if nomes and nome not in nomes: continue
        ims = [b.render(b.Jogador(gol if goleiro else uni, p), giro, INCL, 150, aa=1, largura=LARGURA if goleiro else 1.3) for p in poses]
        W = sum(i.size[0] for i in ims); H = max(i.size[1] for i in ims)
        lin = Image.new("RGBA", (W, H), (30, 70, 40, 255)); x = 0
        for i in ims: lin.alpha_composite(i, (x, 0)); x += i.size[0]
        linhas.append(lin); print(nome, flush=True)
    W = max(l.size[0] for l in linhas); H = sum(l.size[1] for l in linhas)
    im = Image.new("RGB", (W, H), (20, 20, 20)); y = 0
    for l in linhas: im.paste(l, (0, y)); y += l.size[1]
    im.save(saida)

if __name__ == "__main__":
    if sys.argv[1] == "previa": previa(sys.argv[2], sys.argv[3:])
