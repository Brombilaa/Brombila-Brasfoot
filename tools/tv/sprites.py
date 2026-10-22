# Folhas de bonecos para o jogo (mod.Bonecos le em mod/assets/mod/bonecos/<conjunto>.bin).
# Cada quadro guarda, por pixel, o que e preciso para pintar o uniforme de qualquer time
# na hora: luz (difusa+ceu com sombra), brilho somado, material e a coordenada na imagem
# da camisa do time (as camisas do jogo, 50x50, viram textura no peito, nas costas e nas mangas).
#   conjuntos: iso (camera isometrica, inclinacao 36 graus) e tv (replay, 11 graus)
#   4 cortes de cabelo x 8 direcoes x 9 poses (0 parado, 1..8 passos da corrida)
# Uso: python3 sprites.py parte <conjunto> <corte> <pasta>   (gera uma parte, pode rodar em paralelo)
#      python3 sprites.py junta <conjunto> <pasta> <saida.bin>
import math, os, struct, sys, zlib
import numpy as np
import boneco2 as b

CONJUNTOS = {"iso": (math.radians(36), 84), "tv": (0.2, 150)}
CORTES = ["curto", "black", "raspado", "topete"]
DIRS, POSES = 8, 9
# materiais no arquivo (mod.Bonecos usa os mesmos numeros)
PELE, CABELO, CAMISA, CALCAO, MEIA, CHUTEIRA, SOLA, OLHO, BOCA, FAIXA = range(10)
COSTAS = 16
VAZIO = 255

def quadro(corte, giro, pose, incl, alt):
    u = b.Uniforme("#808080", "#404040", "#808080", "#808080", corte=corte)
    j = b.Jogador(u, pose)
    W, H, escala, idx, p, mat, luz, soma = b.ilumina(j, giro, incl, alt, 1)
    M = np.full(W * H, VAZIO, np.uint8); L = np.zeros(W * H, np.uint8); A = np.zeros(W * H, np.uint8)
    U = np.zeros(W * H, np.uint8); V = np.zeros(W * H, np.uint8)
    if len(idx):
        m = np.array(mat)
        out = np.full(len(idx), PELE, np.uint8)
        for de, para in ((b.CABELO, CABELO), (b.CALCAO, CALCAO), (b.MEIA, MEIA), (b.CHUTEIRA, CHUTEIRA), (b.SOLA, SOLA)):
            out[m == de] = para
        # rosto: olhos e boca (mesma conta de Uniforme.cores)
        pe = np.where(m == b.PELE)[0]
        if len(pe):
            q = (p[pe] - j.cabeca) / j.ch
            frente = q[:, 2] > 0.06
            for sx in (-1, 1):
                olho = frente & (((q[:, 0] - sx * 0.037) / 0.013) ** 2 + ((q[:, 1] - 0.013) / 0.019) ** 2 < 1)
                out[pe[olho]] = OLHO
            boca = frente & (np.abs(q[:, 1] + 0.062) < 0.004) & (np.abs(q[:, 0]) < 0.022)
            out[pe[boca]] = BOCA
        me = np.where(m == b.MEIA)[0]
        if len(me):
            y = p[me, 1]; topo = j.yq - 0.43 - 0.05
            out[me[(y > topo - 0.05) & (y < topo - 0.02)]] = FAIXA
        # camisa: coordenada na imagem da camisa do time
        ca = np.where(m == b.CAMISA)[0]
        if len(ca):
            pc = p[ca]
            j.partes(pc)                     # prepara j._sup (bracos) para as mangas
            mangas = j.mangas(pc)
            tronco = j.camisa(pc, j._sem_pescoco, [])
            uu = np.zeros(len(ca)); vv = np.zeros(len(ca)); costas = np.zeros(len(ca), bool)
            q = (pc - j.pelve) @ j.T
            frente = q[:, 2] >= -0.02
            uu[:] = 0.5 + np.clip(q[:, 0] / 0.22, -1.1, 1.1) * 0.23 * np.where(frente, 1, -1)
            vv[:] = 0.08 + (0.58 - q[:, 1]) / 0.625 * 0.86
            costas[:] = ~frente
            for i, (o, cot, pulso, Ro, Rc, sx) in enumerate(j.bracos):
                nesta = (mangas[i] < tronco) & (mangas[i] <= mangas[1 - i])
                if not nesta.any(): continue
                d = pc[nesta] - o
                t = np.clip(d @ (Ro @ [0, -1.0, 0]) / 0.16, 0, 1)
                s = np.clip(d @ (Ro @ [0, 0, 1.0]) / 0.08, -1, 1)
                a0 = np.array([0.5 + sx * 0.23, 0.17]); a1 = np.array([0.5 + sx * 0.33, 0.40])
                ax = a1 - a0; per = np.array([-ax[1], ax[0]]) / np.linalg.norm(ax) * 0.06
                img = a0[None] + t[:, None] * ax[None] + s[:, None] * per[None] * (-sx)
                uu[nesta] = img[:, 0]; vv[nesta] = img[:, 1]; costas[nesta] = False
            out[ca] = CAMISA + costas * COSTAS
            U[idx[ca]] = np.clip(uu * 255, 0, 255).astype(np.uint8)
            V[idx[ca]] = np.clip(vv * 255, 0, 255).astype(np.uint8)
        M[idx] = out
        L[idx] = np.clip(luz * 100, 0, 255).astype(np.uint8)
        A[idx] = np.clip(soma * 255, 0, 255).astype(np.uint8)
    planos = np.stack([L, A, M, U, V]).reshape(5, H, W)
    return W, H, planos

def parte(nome, corte, pasta):
    incl, alt = CONJUNTOS[nome]
    os.makedirs(pasta, exist_ok=True)
    quadros = []
    for d in range(DIRS):
        for k in range(POSES):
            pose = b.pose() if k == 0 else b.pose((k - 1) * 2 * math.pi / 8, True)
            W, H, pl = quadro(CORTES[corte], d * 2 * math.pi / DIRS, pose, incl, alt)
            quadros.append(pl)
            print(nome, corte, d, k, flush=True)
    np.save(os.path.join(pasta, "%s-%d.npy" % (nome, corte)), np.stack(quadros))

def junta(nome, pasta, saida):
    incl, alt = CONJUNTOS[nome]
    corpo = bytearray()
    H = W = 0
    for c in range(len(CORTES)):
        qs = np.load(os.path.join(pasta, "%s-%d.npy" % (nome, c)))
        for pl in qs:
            _, H, W = pl.shape
            cheio = np.where(pl[2] != VAZIO)
            if len(cheio[0]) == 0:
                corpo += struct.pack(">hhhh", 0, 0, 0, 0); continue
            y0, y1 = cheio[0].min(), cheio[0].max() + 1; x0, x1 = cheio[1].min(), cheio[1].max() + 1
            corpo += struct.pack(">hhhh", x0, y0, x1 - x0, y1 - y0)
            corpo += pl[:, y0:y1, x0:x1].astype(np.uint8).tobytes()
    escala = alt / 1.95
    cab = b"BON1" + struct.pack(">iiiiiff", len(CORTES), DIRS, POSES, W, H, escala, H * 0.86)
    with open(saida, "wb") as f:
        f.write(cab + zlib.compress(bytes(corpo), 9))
    print("ok", saida, os.path.getsize(saida))

if __name__ == "__main__":
    if sys.argv[1] == "parte": parte(sys.argv[2], int(sys.argv[3]), sys.argv[4])
    else: junta(sys.argv[2], sys.argv[3], sys.argv[4])
