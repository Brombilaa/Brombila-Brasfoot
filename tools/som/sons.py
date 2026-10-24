"""Sons do jogo e da interface, sintetizados do zero com numpy/scipy (nada gravado de
terceiros, entao nao ha licenca a respeitar).

Partida (trocam os arquivos do jogo em sons/, mesmos nomes): intervalo, fimjogo, gol1,
goladv, penalty, expulsao, contusao.
Novos: apito de inicio, torcida ambiente (loop), "uuuh" de quase gol, sons dos penaltis,
sons da interface e fanfarra de campeao.

Saida: 44,1 kHz, estereo, 16 bits.
uso: python3 tools/som/sons.py [pasta_de_saida]   (padrao: mod/assets/mod/sons)
"""
import os
import sys
import wave

import numpy as np
from scipy import signal

SR = 44100
rng = np.random.default_rng(2026)


# ------------------------------------------------------------------ utilidades

def tempo(dur):
    return np.arange(int(SR * dur)) / SR


def rosa(n):
    f = np.fft.rfft(rng.standard_normal(n))
    k = np.arange(len(f)); k[0] = 1
    x = np.fft.irfft(f / np.sqrt(k), n)
    return x / (np.abs(x).max() + 1e-9)


def biquad(tipo, f, q=0.707, ganho_db=0.0):
    """Coeficientes RBJ."""
    w = 2 * np.pi * f / SR
    a = 10 ** (ganho_db / 40)
    al = np.sin(w) / (2 * q)
    c = np.cos(w)
    if tipo == 'pb':  # passa-baixa
        b = [(1 - c) / 2, 1 - c, (1 - c) / 2]; d = [1 + al, -2 * c, 1 - al]
    elif tipo == 'pa':  # passa-alta
        b = [(1 + c) / 2, -(1 + c), (1 + c) / 2]; d = [1 + al, -2 * c, 1 - al]
    elif tipo == 'bp':  # passa-banda (ganho 1 no pico)
        b = [al, 0, -al]; d = [1 + al, -2 * c, 1 - al]
    else:  # pico
        b = [1 + al * a, -2 * c, 1 - al * a]; d = [1 + al / a, -2 * c, 1 - al / a]
    return np.array(b) / d[0], np.array(d) / d[0]


def filtra(x, tipo, f, q=0.707, ganho_db=0.0):
    b, a = biquad(tipo, f, q, ganho_db)
    return signal.lfilter(b, a, x, axis=-1)


def banda(x, lo, hi):
    return filtra(filtra(x, 'pa', lo), 'pb', hi)


def envelope(n, ataque, queda, forma=1.0):
    """Sobe em 'ataque' s, cai nos ultimos 'queda' s."""
    e = np.ones(n)
    na, nq = max(1, int(ataque * SR)), max(1, int(queda * SR))
    e[:na] = np.linspace(0, 1, na) ** forma
    e[-nq:] *= np.linspace(1, 0, nq) ** forma
    return e


def curva(n, pontos):
    """Interpola [(t, v), ...] em n amostras."""
    ts, vs = zip(*pontos)
    return np.interp(np.arange(n) / SR, ts, vs)


def estereo(x, pan=0.0):
    ang = (pan + 1) * np.pi / 4
    return np.vstack([x * np.cos(ang), x * np.sin(ang)])


def reverb(x, seg=1.8, mix=0.3, brilho=5000):
    """Reverb de convolucao com ruido decaindo (L e R diferentes)."""
    if x.ndim == 1:
        x = estereo(x)
    n = int(seg * SR)
    t = np.arange(n) / SR
    out = np.empty((2, x.shape[1] + n - 1))
    for c in range(2):
        ir = rng.standard_normal(n) * np.exp(-6.9 * t / seg)
        ir = filtra(ir, 'pb', brilho)
        ir[: int(0.012 * SR)] = 0  # pre-delay
        ir /= np.sqrt((ir ** 2).sum())
        out[c] = signal.fftconvolve(x[c], ir)
    seco = np.zeros_like(out); seco[:, : x.shape[1]] = x
    return seco * (1 - mix) + out * mix


def junta(*partes):
    """Soma sinais estereo de tamanhos diferentes: (sinal, inicio_s, ganho)."""
    fim = max(int(i * SR) + s.shape[-1] for s, i, g in partes)
    out = np.zeros((2, fim))
    for s, i, g in partes:
        if s.ndim == 1:
            s = estereo(s)
        k = int(i * SR)
        out[:, k: k + s.shape[1]] += s * g
    return out


def normaliza(x, pico_db=-1.0, rms_db=None):
    if rms_db is not None:
        rms = np.sqrt((x ** 2).mean()) + 1e-12
        x = x * (10 ** (rms_db / 20) / rms)
    p = np.abs(x).max() + 1e-12
    lim = 10 ** (pico_db / 20)
    if p > lim:
        x = x * (lim / p)  # so baixa: limitar distorce o apito
    return x


def grava(nome, x, pasta):
    if x.ndim == 1:
        x = estereo(x)
    x = np.clip(x, -1, 1)
    # entra e sai sem estalo
    k = min(64, x.shape[1] // 4)
    x[:, :k] *= np.linspace(0, 1, k)
    x[:, -k:] *= np.linspace(1, 0, k)
    d = (x.T * 32767).astype('<i2')
    with wave.open(os.path.join(pasta, nome + '.wav'), 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(d.tobytes())


# ------------------------------------------------------------------ apito

def apito(dur, forca=1.0, f0=3150):
    """Apito de bolinha: tom alto com o trinado da bolinha (AM+FM irregular) e sopro."""
    t = tempo(dur)
    n = len(t)
    taxa = 34 + 6 * np.cumsum(rng.standard_normal(n)) / np.sqrt(SR * 40)
    fase_tr = 2 * np.pi * np.cumsum(taxa) / SR
    tr = 0.5 + 0.5 * np.sin(fase_tr)
    ataque = 260 * np.exp(-t / 0.035)  # o tom sobe um pouco no comeco do sopro
    f = f0 + ataque + 90 * tr + 25 * np.sin(2 * np.pi * 4.5 * t)
    fase = 2 * np.pi * np.cumsum(f) / SR
    tom = np.sin(fase) + 0.22 * np.sin(2 * fase) + 0.06 * np.sin(3 * fase)
    tom *= 0.62 + 0.38 * tr
    sopro = filtra(rng.standard_normal(n), 'bp', f0, 4) * 0.5 + filtra(rng.standard_normal(n), 'pa', 5000) * 0.05
    x = (tom + sopro * (0.4 + 0.6 * tr)) * envelope(n, 0.018, 0.05, 0.8) * forca
    return x


def apitos(sequencia, f0=3150):
    """[(inicio, duracao, forca), ...] -> estereo com eco de estadio."""
    partes = [(apito(d, g, f0), i, 1.0) for i, d, g in sequencia]
    x = junta(*partes)
    x = np.hstack([x, np.zeros((2, int(0.2 * SR)))])
    return reverb(x, seg=1.4, mix=0.22, brilho=7000)


# ------------------------------------------------------------------ torcida

VOGAIS = {  # formantes (Hz) e larguras relativas
    'a': [(730, 1.0), (1150, 0.6), (2500, 0.25)],
    'o': [(480, 1.0), (840, 0.6), (2500, 0.15)],
    'u': [(330, 1.0), (760, 0.35), (2400, 0.08)],
    'e': [(520, 1.0), (1750, 0.5), (2550, 0.25)],
}


def vozes(n, quantas, tom, vib=5.0, onda_tom=None, espalha=0.12, vida=None):
    """Soma de 'quantas' vozes (pulso glotal) com tom em Hz ~ tom (lista lo,hi)."""
    t = np.arange(n) / SR
    soma = np.zeros((2, n))
    for _ in range(quantas):
        f = rng.uniform(*tom)
        desv = 1 + espalha * rng.standard_normal() * 0.3
        fv = f * desv * (1 + 0.012 * np.sin(2 * np.pi * rng.uniform(vib * 0.6, vib * 1.4) * t + rng.uniform(0, 6.3)))
        if onda_tom is not None:
            fv = fv * onda_tom
        fv *= 1 + 0.02 * np.cumsum(rng.standard_normal(n)) / np.sqrt(SR * 5)
        fase = np.cumsum(fv) / SR
        serra = 2 * (fase % 1) - 1
        pulso = serra - np.roll(serra, 1) * 0.3
        amp = 1.0
        if vida is not None:
            amp = vida(t)
        pan = rng.uniform(-0.9, 0.9)
        soma += estereo(pulso * amp, pan) * rng.uniform(0.5, 1.0)
    return soma


def vogal(x, v):
    out = np.zeros_like(x)
    for f, g in VOGAIS[v]:
        out += filtra(x, 'bp', f, 5) * g
    return filtra(out, 'pb', 4200)


def vogal_mudando(x, pontos):
    """pontos: [(t, vogal), ...]; troca de vogal com fade."""
    filtradas = {v: vogal(x, v) for _, v in pontos}
    n = x.shape[-1]
    out = np.zeros_like(x)
    for i, (t0, v) in enumerate(pontos):
        t1 = pontos[i + 1][0] if i + 1 < len(pontos) else n / SR + 1
        tp = pontos[i - 1][0] if i > 0 else -1
        w = curva(n, [(tp, 0), (t0, 1), (t1 - 0.001, 1), (t1 + 0.12, 0)]) if i > 0 else curva(n, [(0, 1), (t1, 1), (t1 + 0.12, 0)])
        out += filtradas[v] * w
    return out


def burburinho(n, nivel=1.0):
    """Ruido de multidao conversando (rosa em banda de voz, mexendo devagar)."""
    out = np.zeros((2, n))
    for c in range(2):
        r = banda(rosa(n), 180, 3200)
        mod = 0.75 + 0.25 * banda(rng.standard_normal(n), 0.3, 3) * 30
        out[c] = r * np.clip(mod, 0.3, 1.3)
    return out * nivel


def palmas(n, densidade=40, nivel=1.0):
    out = np.zeros((2, n))
    k = int(densidade * n / SR)
    for _ in range(k):
        i = rng.integers(0, n - 2000)
        d = int(rng.uniform(0.004, 0.012) * SR)
        c = rng.standard_normal(d) * np.exp(-np.arange(d) / (d / 4))
        c = filtra(c, 'bp', rng.uniform(900, 2400), 1.2)
        out[:, i: i + d] += estereo(c, rng.uniform(-1, 1)) * rng.uniform(0.3, 1)
    return out * nivel


def surdo(n, bpm=120, nivel=1.0, inicio=0.0):
    out = np.zeros(n)
    passo = 60 / bpm
    t0 = inicio
    j = 0
    while t0 < n / SR - 0.4:
        d = int(0.35 * SR)
        tt = np.arange(d) / SR
        f = 58 + 40 * np.exp(-tt / 0.03)
        golpe = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / 0.12)
        golpe += filtra(rng.standard_normal(d), 'bp', 300, 2) * np.exp(-tt / 0.02) * 0.3
        g = 1.0 if j % 2 == 0 else 0.65
        i = int(t0 * SR)
        out[i: i + d] += golpe[: n - i] * g
        t0 += passo
        j += 1
    return estereo(out, 0.15) * nivel


# ------------------------------------------------------------------ sons da partida

def gol():
    """Explosao da torcida: grito subindo, "GOOOOL" sustentado, palmas, surdo."""
    dur = 4.6
    n = int(dur * SR)
    onda = curva(n, [(0, 0.85), (0.25, 1.05), (1.0, 1.12), (3.2, 1.08), (dur, 0.95)])
    vida = lambda t: np.interp(t, [0, 0.12, 0.45, 3.0, dur], [0, 0.6, 1.0, 0.85, 0.15])
    v = vozes(n, 70, (190, 330), vib=6, onda_tom=onda, vida=vida)
    v = vogal_mudando(v, [(0, 'a'), (0.5, 'o'), (3.2, 'o')])
    b = burburinho(n) * curva(n, [(0, 0.2), (0.2, 1.0), (dur, 0.4)])
    p = palmas(n, 260) * curva(n, [(0, 0), (0.6, 0.4), (2.0, 1.0), (dur, 0.6)])
    s = surdo(n, 128, inicio=0.9) * curva(n, [(0, 0), (1.0, 0.8), (dur, 0.8)])
    x = v * 1.0 + b * 0.35 + p * 0.25 + s * 0.35
    x = reverb(x, seg=2.2, mix=0.35)
    return normaliza(x, -1, rms_db=-13)


def lamento():
    """Gol do adversario: torcida da casa solta um "ooohh" caindo, e la longe a outra comemora."""
    dur = 3.0
    n = int(dur * SR)
    onda = curva(n, [(0, 1.15), (0.3, 1.05), (dur, 0.78)])
    vida = lambda t: np.interp(t, [0, 0.08, 0.5, 2.2, dur], [0, 0.9, 0.8, 0.3, 0])
    v = vogal(vozes(n, 55, (150, 240), vib=4, onda_tom=onda, vida=vida), 'o')
    longe = vogal(vozes(n, 25, (220, 320), vida=lambda t: np.interp(t, [0, 0.3, 2.4, dur], [0, 1, 0.7, 0])), 'a')
    longe = filtra(longe, 'pb', 1600) * 0.25
    b = burburinho(n) * curva(n, [(0, 0.6), (dur, 0.25)])
    x = reverb(v + longe + b * 0.3, seg=2.0, mix=0.35)
    return normaliza(x, -2, rms_db=-17)


def uuuh(dur=2.2):
    """Quase gol: "uuuuh" subindo e caindo."""
    n = int(dur * SR)
    onda = curva(n, [(0, 0.9), (0.35, 1.18), (dur, 0.85)])
    vida = lambda t: np.interp(t, [0, 0.1, 0.4, 1.4, dur], [0, 0.7, 1, 0.5, 0])
    v = vogal_mudando(vozes(n, 60, (170, 280), onda_tom=onda, vida=vida), [(0, 'u'), (0.5, 'o')])
    b = burburinho(n) * 0.25
    x = reverb(v + b, seg=2.0, mix=0.35)
    return normaliza(x, -2, rms_db=-16)


def vaia(dur=2.6):
    n = int(dur * SR)
    onda = curva(n, [(0, 1.0), (dur, 0.92)])
    vida = lambda t: np.interp(t, [0, 0.25, 1.8, dur], [0, 1, 0.9, 0])
    v = vogal(vozes(n, 60, (110, 190), vib=3, onda_tom=onda, vida=vida), 'u')
    return reverb(v + burburinho(n) * 0.2, seg=2.0, mix=0.3)


def penalti():
    """Apito forte e a torcida prendendo a respiracao."""
    a = apitos([(0, 0.55, 1.0)])
    n = int(2.4 * SR)
    v = vogal(vozes(n, 50, (180, 280), onda_tom=curva(n, [(0, 0.95), (2.4, 1.1)]),
                    vida=lambda t: np.interp(t, [0, 0.4, 1.6, 2.4], [0, 0.8, 0.6, 0])), 'o')
    v = reverb(v, seg=2.0, mix=0.35)
    return normaliza(junta((a, 0, 1.0), (v, 0.35, 0.55)), -1, rms_db=-15)


def expulsao():
    """Dois apitos secos e vaia."""
    a = apitos([(0, 0.16, 1.0), (0.24, 0.5, 1.0)])
    return normaliza(junta((a, 0, 1.0), (vaia(), 0.5, 0.5)), -1, rms_db=-15)


def contusao():
    """Apito curto e um murmurio preocupado."""
    a = apitos([(0, 0.3, 0.8)])
    n = int(2.0 * SR)
    v = vogal(vozes(n, 35, (160, 240), onda_tom=curva(n, [(0, 1.05), (2, 0.85)]),
                    vida=lambda t: np.interp(t, [0, 0.3, 1.2, 2.0], [0, 0.7, 0.4, 0])), 'o')
    m = reverb(v + burburinho(n) * 0.4, seg=2.0, mix=0.35)
    return normaliza(junta((a, 0, 1.0), (m, 0.25, 0.4)), -1, rms_db=-17)


def intervalo():
    """Fim do 1o tempo: dois apitos."""
    return normaliza(apitos([(0, 0.32, 1.0), (0.45, 0.85, 1.0)]), -1, rms_db=-15)


def fimjogo():
    """Fim de jogo: dois curtos e um longo, e a torcida reage."""
    a = apitos([(0, 0.2, 1.0), (0.3, 0.2, 1.0), (0.62, 1.2, 1.0)])
    n = int(2.6 * SR)
    m = burburinho(n) * curva(n, [(0, 0), (0.5, 1), (2.6, 0)])
    p = palmas(n, 120) * curva(n, [(0, 0), (0.6, 1), (2.6, 0)])
    m = reverb(m * 0.5 + p * 0.3, seg=2.0, mix=0.3)
    return normaliza(junta((a, 0, 1.0), (m, 1.2, 0.6)), -1, rms_db=-16)


def inicio():
    """Apito longo de inicio de tempo."""
    return normaliza(apitos([(0, 1.0, 1.0)]), -1, rms_db=-15)


def ambiente(dur=24.0):
    """Torcida durante a partida, em loop sem emenda: burburinho, palmas, surdo e cantos."""
    n = int(dur * SR)
    m = int(2.0 * SR)  # sobra para emendar
    nn = n + m
    b = burburinho(nn) * 0.6
    p = palmas(nn, 25) * 0.25
    s = surdo(nn, 112, nivel=0.25, inicio=0.2)
    s = filtra(s, 'pb', 900)
    # cantos curtos espalhados (grupos de vozes em "o"/"e")
    cantos = np.zeros((2, nn))
    t = 1.0
    while t < nn / SR - 3:
        d = rng.uniform(1.5, 2.8)
        k = int(d * SR)
        vida = lambda tt, d=d: np.interp(tt, [0, 0.25, d - 0.4, d], [0, 1, 1, 0]) * (0.75 + 0.25 * np.sin(2 * np.pi * 1.9 * tt))
        c = vogal(vozes(k, 30, (170, 260), vida=vida), rng.choice(['o', 'e', 'a']))
        i = int(t * SR)
        cantos[:, i: i + k] += c * rng.uniform(0.15, 0.3)
        t += d + rng.uniform(2.5, 5.0)
    x = b + p + s + cantos
    x = reverb(x, seg=2.4, mix=0.4)[:, :nn]
    # emenda: os ultimos 2 s entram por cima do comeco
    w = np.linspace(0, 1, m)
    x[:, :m] = x[:, :m] * w + x[:, n: n + m] * (1 - w)
    return normaliza(x[:, :n], -6, rms_db=-26)


# ------------------------------------------------------------------ penaltis

def chute():
    d = int(0.25 * SR)
    t = np.arange(d) / SR
    f = 70 + 120 * np.exp(-t / 0.012)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.05)
    x += filtra(rng.standard_normal(d), 'bp', 1800, 1.5) * np.exp(-t / 0.008) * 0.6
    return normaliza(reverb(x, seg=1.0, mix=0.15), -2, rms_db=-14)


def rede():
    d = int(0.7 * SR)
    t = np.arange(d) / SR
    x = banda(rng.standard_normal(d), 1500, 7000) * np.exp(-t / 0.12) * (1 - np.exp(-t / 0.01))
    x += filtra(rng.standard_normal(d), 'bp', 600, 2) * np.exp(-t / 0.08) * 0.4
    return normaliza(reverb(x, seg=0.8, mix=0.15), -3, rms_db=-20)


def trave():
    d = int(1.8 * SR)
    t = np.arange(d) / SR
    x = np.zeros(d)
    for f, a, tau in [(523, 1.0, 0.9), (1188, 0.6, 0.6), (1950, 0.45, 0.4), (2813, 0.3, 0.3), (4120, 0.15, 0.15)]:
        x += a * np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * np.exp(-t / tau)
    x *= 1 + 0.15 * np.sin(2 * np.pi * 7 * t)
    x += filtra(rng.standard_normal(d), 'pa', 2000) * np.exp(-t / 0.006) * 0.8
    x = reverb(x, seg=1.4, mix=0.25)
    return normaliza(junta((x, 0, 1.0), (uuuh(2.0), 0.15, 0.6)), -1, rms_db=-15)


def defesa():
    d = int(0.3 * SR)
    t = np.arange(d) / SR
    x = np.sin(2 * np.pi * np.cumsum(90 + 60 * np.exp(-t / 0.01)) / SR) * np.exp(-t / 0.04)
    x += filtra(rng.standard_normal(d), 'bp', 900, 1.0) * np.exp(-t / 0.015) * 0.8
    x = reverb(x, seg=0.8, mix=0.15)
    n = int(2.0 * SR)
    oh = vogal(vozes(n, 50, (170, 260), onda_tom=curva(n, [(0, 1.1), (2, 0.85)]),
                     vida=lambda t: np.interp(t, [0, 0.08, 0.6, 2.0], [0, 1, 0.6, 0])), 'o')
    oh = reverb(oh, seg=2.0, mix=0.35)
    return normaliza(junta((x, 0, 1.0), (oh, 0.05, 0.5)), -1, rms_db=-15)


# ------------------------------------------------------------------ interface

def clique():
    d = int(0.06 * SR)
    t = np.arange(d) / SR
    x = np.sin(2 * np.pi * 1350 * t) * np.exp(-t / 0.012) * 0.6
    x += np.sin(2 * np.pi * 2700 * t) * np.exp(-t / 0.006) * 0.25
    x += filtra(rng.standard_normal(d), 'pa', 3000) * np.exp(-t / 0.002) * 0.4
    return normaliza(x, -8)


def passa():
    d = int(0.035 * SR)
    t = np.arange(d) / SR
    x = np.sin(2 * np.pi * 2400 * t) * np.exp(-t / 0.006)
    return normaliza(x, -20)


def moedas():
    out = []
    for k, (i, f) in enumerate([(0.0, 2350), (0.07, 2780), (0.15, 3120)]):
        d = int(0.5 * SR)
        t = np.arange(d) / SR
        x = np.zeros(d)
        for r, a, tau in [(1, 1, 0.18), (2.76, 0.5, 0.09), (5.4, 0.25, 0.05), (8.93, 0.12, 0.03)]:
            x += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / tau)
        x += filtra(rng.standard_normal(d), 'pa', 4000) * np.exp(-t / 0.003) * 0.3
        out.append((estereo(x, (k - 1) * 0.3), i, 1.0 - 0.15 * k))
    return normaliza(reverb(junta(*out), seg=0.6, mix=0.12), -7)


def mensagem():
    out = []
    for i, f in [(0.0, 1318.5), (0.11, 1760.0)]:
        d = int(0.9 * SR)
        t = np.arange(d) / SR
        x = (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / 0.08)
             + 0.12 * np.sin(2 * np.pi * 3.01 * f * t) * np.exp(-t / 0.05))
        x *= np.exp(-t / 0.28) * (1 - np.exp(-t / 0.002))
        out.append((x, i, 1.0))
    return normaliza(reverb(junta(*out), seg=0.9, mix=0.18), -7)


def carimbo():
    d = int(0.45 * SR)
    t = np.arange(d) / SR
    x = np.sin(2 * np.pi * np.cumsum(55 + 110 * np.exp(-t / 0.02)) / SR) * np.exp(-t / 0.09)
    x += filtra(rng.standard_normal(d), 'bp', 2200, 0.8) * np.exp(-t / 0.012) * 0.7
    x += filtra(rng.standard_normal(d), 'bp', 450, 2) * np.exp(-t / 0.03) * 0.5
    return normaliza(reverb(x, seg=0.7, mix=0.12), -3, rms_db=-16)


def carta():
    d = int(0.22 * SR)
    t = np.arange(d) / SR
    e = np.interp(t, [0, 0.07, 0.12, 0.22], [0, 1, 0.35, 0])
    x = banda(rng.standard_normal(d), 2500, 9000) * e
    x += filtra(rng.standard_normal(d), 'bp', 1200, 2) * np.exp(-np.maximum(t - 0.1, 0) / 0.01) * (t > 0.1) * 0.6
    return normaliza(estereo(x, 0.1), -10)


def janela():
    d = int(0.32 * SR)
    t = np.arange(d) / SR
    fc = np.interp(t, [0, 0.32], [500, 2600])
    r = rng.standard_normal(d)
    x = np.zeros(d)
    # filtro andando em blocos de 5 ms
    bl = int(0.005 * SR)
    zi = None
    for i in range(0, d, bl):
        b, a = biquad('bp', fc[i], 1.6)
        if zi is None:
            zi = signal.lfilter_zi(b, a) * 0
        x[i: i + bl], zi = signal.lfilter(b, a, r[i: i + bl], zi=zi)
    x *= np.interp(t, [0, 0.12, 0.32], [0, 1, 0]) ** 1.5
    return normaliza(estereo(x, 0.0), -14)


def fanfarra():
    """E campeao: metais (serra com filtro abrindo), tímpano e a torcida."""
    notas = [(0.0, 0.16, [523.25]), (0.18, 0.16, [523.25]), (0.36, 0.16, [523.25]),
             (0.56, 0.5, [698.46, 880.0]), (1.1, 0.3, [783.99, 987.77]), (1.45, 1.9, [523.25, 659.25, 783.99, 1046.5])]
    partes = []
    for i, d, fs in notas:
        n = int((d + 0.4) * SR)
        t = np.arange(n) / SR
        x = np.zeros(n)
        for f in fs:
            for dt in (-0.004, 0.0, 0.005):
                fase = np.cumsum(f * (1 + dt) * (1 + 0.004 * np.sin(2 * np.pi * 5.5 * t))) / SR
                x += 2 * (fase % 1) - 1
        e = np.interp(t, [0, 0.03, d, d + 0.25], [0, 1, 0.85, 0])
        # filtro abre no ataque (som de metal)
        x = filtra(x, 'pb', 1800, 1.2) * 0.6 + filtra(x, 'pb', 4500) * 0.4 * np.interp(t, [0, 0.05, 0.3], [0.2, 1, 0.5])
        partes.append((x * e / len(fs), i, 1.0))
    metais = reverb(junta(*partes), seg=2.4, mix=0.3)
    # timpano no acorde final
    n = int(2.5 * SR)
    t = np.arange(n) / SR
    tim = np.zeros(n)
    for k in range(12):
        j = int(k * 0.05 * SR)
        tt = t[: n - j]
        tim[j:] += np.sin(2 * np.pi * (98 + 20 * np.exp(-tt / 0.03)) * tt) * np.exp(-tt / 0.25) * (0.4 + 0.05 * k)
    tim = reverb(tim, seg=1.8, mix=0.3)
    x = junta((metais, 0, 1.0), (tim, 1.2, 0.35), (gol(), 1.0, 0.45))
    return normaliza(x, -1, rms_db=-14)


# ------------------------------------------------------------------ versao 2 (torcida e apitos)
# A torcida da v1 era um coro de vozes "limpas" e soava artificial. Aqui a voz de cada
# torcedor tem sopro, tremor de tom e de volume, e cada grupo tem formantes proprios; por
# baixo vai um rugido de ruido com o espectro de estadio, e tudo passa pela distancia
# (menos agudo) e por um eco grande.

def torcida(dur, intensidade, tom, vogais, n_vozes=240, grupos=16, sopro=0.45,
            rugido=0.6, longe=0.5, curva_tom=None, fala=0.0):
    """intensidade: [(t, 0..1)], tom: (lo, hi) Hz, vogais: [(t, 'a'|'o'|'u'|'e')]."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    forca = curva(n, intensidade)
    onda = curva(n, curva_tom) if curva_tom else np.ones(n)
    voz = np.zeros((2, n))
    for g in range(grupos):
        esc = rng.uniform(0.86, 1.18)  # tamanho do trato vocal do grupo
        soma = np.zeros(n)
        for _ in range(n_vozes // grupos):
            f = rng.uniform(*tom)
            treme = 1 + 0.035 * filtra(rng.standard_normal(n), 'pb', 6) * 25
            fv = f * onda * treme
            fase = np.cumsum(fv) / SR
            serra = 2 * (fase % 1) - 1
            pulso = serra - np.roll(serra, 1) * 0.3
            ar = rng.standard_normal(n)
            exc = pulso * (1 - sopro) + ar * sopro * 0.8
            # cada um entra e sai num tempo um pouco diferente
            atraso = rng.uniform(0, 0.25)
            vol = np.interp(t - atraso, t, forca, left=0) * np.clip(1 + 0.5 * filtra(rng.standard_normal(n), 'pb', 3) * 20, 0.2, 1.6)
            if fala > 0:
                # conversa: cada um fala em silabas soltas, nao segura a vogal
                g = filtra(rng.standard_normal(n), 'pb', fala) * np.sqrt(SR / fala) * 0.25
                vol = vol * np.clip(g + rng.uniform(-0.6, 0.2), 0, 1) ** 1.5
            soma += exc * vol * rng.uniform(0.4, 1.0)
        filtradas = {}
        for _, v in vogais:
            if v not in filtradas:
                o = np.zeros(n)
                for fr, ga in VOGAIS[v]:
                    o += filtra(soma, 'bp', fr * esc, 4) * ga
                filtradas[v] = o
        if len(vogais) == 1:
            sg = filtradas[vogais[0][1]]
        else:
            sg = np.zeros(n)
            for i, (t0, v) in enumerate(vogais):
                t1 = vogais[i + 1][0] if i + 1 < len(vogais) else dur + 1
                w = np.clip((t - t0) / 0.15 + 1, 0, 1) if i > 0 else np.ones(n)
                w = w * np.clip((t1 - t) / 0.15, 0, 1)
                sg += filtradas[v] * w
        voz += estereo(sg, rng.uniform(-0.85, 0.85))
    voz /= (n_vozes ** 0.5)
    # rugido: ruido com espectro de estadio, mexendo junto com a intensidade
    r = np.zeros((2, n))
    for c in range(2):
        x = filtra(banda(rosa(n), 220, 3800), 'pico', 850, 0.8, 5)
        mod = np.clip(1 + 0.25 * filtra(rng.standard_normal(n), 'pb', 5) * 25, 0.5, 1.5)
        r[c] = x * mod * forca
    voz /= np.sqrt((voz ** 2).mean()) + 1e-9
    r /= np.sqrt((r ** 2).mean()) + 1e-9
    x = voz * (1 - rugido) + r * rugido
    # som de torcida gravada quase nao tem grave (abaixo de ~250 Hz) e tem o pico perto de 1 kHz
    x = filtra(filtra(x, 'pa', 270), 'pa', 270)
    x = filtra(x, 'pico', 1000, 0.9, 3)
    # distancia: tira agudo e poe eco grande
    x = filtra(x, 'pb', 6500 - 3500 * longe)
    x = reverb(x, seg=1.8 + 1.4 * longe, mix=0.25 + 0.3 * longe, brilho=4500)
    return x


def rojao(n_estouros=5):
    partes = []
    tt = 0.0
    for _ in range(n_estouros):
        d = int(0.9 * SR)
        t = np.arange(d) / SR
        x = rng.standard_normal(d) * np.exp(-t / 0.03)
        x = filtra(x, 'pb', 2500) + filtra(rng.standard_normal(d), 'pb', 200) * np.exp(-t / 0.06) * 2
        partes.append((estereo(x, rng.uniform(-0.6, 0.6)), tt, rng.uniform(0.5, 1.0)))
        tt += rng.uniform(0.15, 0.45)
    return reverb(junta(*partes), seg=2.5, mix=0.45, brilho=3000)


def apito_fox(dur, forca=1.0):
    """Apito sem bolinha (tipo Fox 40): tres tons proximos batendo, ataque forte."""
    t = tempo(dur)
    n = len(t)
    x = np.zeros(n)
    for f, a in [(2720, 1.0), (3080, 0.8), (3410, 0.6)]:
        fv = f * (1 + 0.004 * np.sin(2 * np.pi * 3 * t)) + 120 * np.exp(-t / 0.03)
        fase = 2 * np.pi * np.cumsum(fv) / SR
        x += a * (np.sin(fase) + 0.15 * np.sin(2 * fase))
    x += filtra(rng.standard_normal(n), 'bp', 3000, 3) * 0.35
    return x * envelope(n, 0.01, 0.06, 0.7) * forca * 0.6


def apitos2(sequencia, tipo='bolinha', longe=0.5):
    f = apito_fox if tipo == 'fox' else (lambda d, g: apito(d, g, 2900))
    x = junta(*[(f(d, g), i, 1.0) for i, d, g in sequencia])
    x = np.hstack([x, np.zeros((2, int(0.3 * SR)))])
    x = filtra(x, 'pb', 9000 - 3000 * longe)
    return reverb(x, seg=1.2 + 1.2 * longe, mix=0.18 + 0.25 * longe, brilho=6000)


# variantes: 'a' = estadio cheio, de longe (mais rugido, mais eco); 'b' = perto da torcida
def gol2(v):
    if v == 'a':
        x = torcida(5.0, [(0, 0.05), (0.15, 0.7), (0.5, 1.0), (3.4, 0.85), (5.0, 0.1)], (200, 340),
                    [(0, 'a'), (0.45, 'o')], rugido=0.65, longe=0.75,
                    curva_tom=[(0, 0.85), (0.4, 1.08), (3.5, 1.04), (5, 0.9)])
        s = filtra(filtra(surdo(int(5 * SR), 126, inicio=1.2), 'pb', 700), 'pa', 120) * curva(int(5 * SR), [(0, 0), (1.2, 0.5), (5, 0.4)])
        x = junta((x, 0, 1.0), (s, 0, 0.5), (palmas(int(4.5 * SR), 300) * 0.25, 0.5, 1.0))
    else:
        x = torcida(5.0, [(0, 0.05), (0.12, 0.8), (0.4, 1.0), (3.4, 0.9), (5.0, 0.1)], (210, 360),
                    [(0, 'a'), (0.35, 'o'), (3.0, 'o')], rugido=0.45, longe=0.3, sopro=0.55,
                    curva_tom=[(0, 0.85), (0.35, 1.1), (3.5, 1.05), (5, 0.9)])
        x = junta((x, 0, 1.0), (rojao(6), 0.9, 0.35), (palmas(int(4.5 * SR), 350) * 0.3, 0.6, 1.0))
    return normaliza(x, -1, rms_db=-13)


def lamento2(v):
    longe = 0.75 if v == 'a' else 0.35
    x = torcida(3.2, [(0, 0), (0.1, 1.0), (0.6, 0.75), (2.4, 0.25), (3.2, 0)], (150, 250),
                [(0, 'o'), (0.8, 'u')], rugido=0.55, longe=longe,
                curva_tom=[(0, 1.15), (0.3, 1.05), (3.2, 0.78)])
    # a torcida visitante comemorando la longe
    vis = torcida(3.2, [(0, 0), (0.3, 0.6), (2.6, 0.5), (3.2, 0)], (230, 330), [(0, 'a')],
                  n_vozes=96, rugido=0.6, longe=1.0)
    vis = filtra(vis, 'pb', 1500)
    return normaliza(junta((x, 0, 1.0), (vis, 0, 0.3)), -2, rms_db=-17)


def uuuh2(v, dur=2.4):
    longe = 0.7 if v == 'a' else 0.35
    x = torcida(dur, [(0, 0), (0.15, 0.75), (0.45, 1.0), (1.3, 0.55), (dur, 0)], (170, 290),
                [(0, 'u'), (0.6, 'o')], rugido=0.5 if v == 'a' else 0.35, longe=longe, sopro=0.5,
                curva_tom=[(0, 0.88), (0.45, 1.2), (dur, 0.86)])
    return normaliza(x, -2, rms_db=-16)


def vaia2(longe, dur=2.6):
    return torcida(dur, [(0, 0), (0.3, 1.0), (1.9, 0.85), (dur, 0)], (105, 185), [(0, 'u')],
                   rugido=0.5, longe=longe, sopro=0.4, curva_tom=[(0, 1.0), (dur, 0.93)])


def murmurio(longe, dur=2.2):
    return torcida(dur, [(0, 0), (0.3, 0.7), (1.3, 0.4), (dur, 0)], (150, 230), [(0, 'o')],
                   n_vozes=160, rugido=0.6, longe=longe, curva_tom=[(0, 1.05), (dur, 0.86)])


# apitos: 'a' = apito de bolinha (trinado), 'b' = apito sem bolinha (Fox 40)
def tipo_apito(v):
    return 'bolinha' if v == 'a' else 'fox'


def intervalo2(v):
    return normaliza(apitos2([(0, 0.35, 1.0), (0.5, 0.9, 1.0)], tipo_apito(v)), -1, rms_db=-16)


def fimjogo2(v):
    a = apitos2([(0, 0.22, 1.0), (0.34, 0.22, 1.0), (0.7, 1.3, 1.0)], tipo_apito(v))
    m = torcida(3.0, [(0, 0), (0.5, 0.6), (3.0, 0)], (180, 280), [(0, 'a')], n_vozes=128, rugido=0.7, longe=0.8)
    p = palmas(int(2.8 * SR), 160) * curva(int(2.8 * SR), [(0, 0), (0.6, 1), (2.8, 0)])
    return normaliza(junta((a, 0, 1.0), (m, 1.4, 0.35), (p, 1.5, 0.25)), -1, rms_db=-16)


def penalti2(v):
    a = apitos2([(0, 0.6, 1.0)], tipo_apito(v))
    m = torcida(2.6, [(0, 0), (0.5, 0.8), (1.6, 0.6), (2.6, 0)], (180, 280), [(0, 'o')], n_vozes=160,
                rugido=0.55, longe=0.6, curva_tom=[(0, 0.95), (2.6, 1.1)])
    return normaliza(junta((a, 0, 1.0), (m, 0.4, 0.5)), -1, rms_db=-15)


def expulsao2(v):
    a = apitos2([(0, 0.17, 1.0), (0.26, 0.55, 1.0)], tipo_apito(v))
    return normaliza(junta((a, 0, 1.0), (vaia2(0.6), 0.55, 0.5)), -1, rms_db=-15)


def contusao2(v):
    a = apitos2([(0, 0.3, 0.85)], tipo_apito(v))
    return normaliza(junta((a, 0, 1.0), (murmurio(0.6), 0.3, 0.4)), -1, rms_db=-17)


def trave2(v):
    """'a' = trave de aluminio, som surdo e curto ("tonc"); 'b' = mais metalico ("ping")."""
    d = int(1.2 * SR)
    t = np.arange(d) / SR
    x = np.zeros(d)
    if v == 'a':
        modos = [(310, 1.0, 0.16), (742, 0.55, 0.09), (1290, 0.35, 0.05), (2050, 0.2, 0.03)]
    else:
        modos = [(620, 0.8, 0.35), (1480, 0.6, 0.22), (2390, 0.45, 0.14), (3570, 0.25, 0.08)]
    for f, a, tau in modos:
        x += a * np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * np.exp(-t / tau)
    # a bola batendo (couro) por cima
    x += np.sin(2 * np.pi * np.cumsum(80 + 90 * np.exp(-t / 0.01)) / SR) * np.exp(-t / 0.04) * 0.8
    x += filtra(rng.standard_normal(d), 'bp', 1500, 1.2) * np.exp(-t / 0.006) * 0.7
    x = reverb(x, seg=1.6, mix=0.3, brilho=5000)
    return normaliza(junta((x, 0, 1.0), (uuuh2('a', 2.2), 0.12, 0.55)), -1, rms_db=-15)



# ------------------------------------------------------------------ versao 3: cada reacao com o seu clima
# Na v2 todas as reacoes soavam como comemoracao (vozes agudas segurando "a/o" bem alto).
# Aqui cada uma muda o que a torcida esta fazendo: comemorar, lamentar e calar, prender o
# ar, vaiar com assobios, conversar preocupada. Todas comecam com 0,6 s da torcida de
# fundo, para ouvir a mudanca de clima (no jogo a torcida ambiente ja estara tocando).

CAMA = 0.6
# volume fixo para todas as reacoes (a torcida de fundo fica em ~-27 dB); assim o gol e
# alto e o gol sofrido e baixo, em vez de todas terem o mesmo volume
GANHO = 0.126


def final(x):
    x = x * GANHO
    p = np.abs(x).max()
    return x * (0.89 / p) if p > 0.89 else x



SEM_CAMA = False  # no jogo a torcida ambiente ja esta tocando: tools/som/monta.py liga isto


def cama(dur, nivel=1.0):
    """Torcida de fundo: conversa e rugido baixo (so nas previas)."""
    if SEM_CAMA:
        return np.zeros((2, int(dur * SR)))
    x = torcida(dur, [(0, 0.5), (dur, 0.5)], (110, 230), [(0, 'e')], n_vozes=128, rugido=0.7,
                longe=0.85, fala=5.0)
    return x * nivel


def assobios(dur, quantos=40, inicio=0.0):
    """Assobio de dedo: tom alto deslizando (de vaia)."""
    n = int(dur * SR)
    out = np.zeros((2, n))
    for _ in range(quantos):
        d = rng.uniform(0.35, 1.1)
        k = int(d * SR)
        i = int(rng.uniform(inicio, max(inicio, dur - d)) * SR)
        if i + k > n:
            continue
        tt = np.arange(k) / SR
        f0 = rng.uniform(1900, 3100)
        forma = rng.integers(0, 3)
        if forma == 0:
            f = f0 * (1 + 0.12 * np.sin(np.pi * tt / d))  # sobe e desce
        elif forma == 1:
            f = f0 * (1 - 0.15 * tt / d)  # cai
        else:
            f = f0 * np.ones(k)  # reto
        f *= 1 + 0.01 * np.sin(2 * np.pi * rng.uniform(4, 7) * tt)
        x = np.sin(2 * np.pi * np.cumsum(f) / SR) + filtra(rng.standard_normal(k), 'bp', f0, 6) * 0.3
        x *= np.interp(tt, [0, 0.04, d - 0.08, d], [0, 1, 0.9, 0])
        out[:, i: i + k] += estereo(x, rng.uniform(-0.9, 0.9)) * rng.uniform(0.2, 0.6)
    return reverb(filtra(out, 'pb', 5000), seg=2.2, mix=0.4)


def gol3():
    """Comemoracao: grito subindo rapido para um "GOOOL" longo, rojoes, surdo e palmas."""
    dur = 5.2
    x = torcida(dur, [(0, 0.05), (0.12, 0.8), (0.45, 1.0), (3.6, 0.9), (dur, 0.1)], (220, 380),
                [(0, 'a'), (0.35, 'o')], rugido=0.45, longe=0.4, sopro=0.5,
                curva_tom=[(0, 0.8), (0.35, 1.12), (3.6, 1.06), (dur, 0.9)])
    n = int(dur * SR)
    s = filtra(filtra(surdo(n, 128, inicio=1.3), 'pb', 700), 'pa', 120) * curva(n, [(0, 0), (1.3, 0.6), (dur, 0.5)])
    x = junta((cama(CAMA + 0.1), 0, 0.5), (x, CAMA, 3.0), (rojao(6), CAMA + 0.9, 0.9), (s, CAMA, 1.2),
              (palmas(int(4.6 * SR), 350) * 0.7, CAMA + 0.6, 1.0))
    return final(x)


def lamento3():
    """Gol sofrido: um "aaahh" grave caindo, a torcida cala e fica so o burburinho; la longe
    os visitantes comemoram."""
    dur = 4.0
    queda = torcida(1.6, [(0, 0), (0.06, 1.0), (0.5, 0.5), (1.6, 0)], (110, 190), [(0, 'a'), (0.3, 'o')],
                    n_vozes=200, rugido=0.5, longe=0.55, sopro=0.6,
                    curva_tom=[(0, 1.1), (1.6, 0.72)])
    resto = torcida(dur - 1.0, [(0, 0), (0.5, 0.35), (dur - 1.0, 0.3)], (100, 200), [(0, 'e')],
                    n_vozes=96, rugido=0.75, longe=0.9, fala=4.0)
    vis = torcida(dur, [(0, 0), (0.3, 0.8), (3.0, 0.6), (dur, 0.2)], (230, 340), [(0, 'o')],
                  n_vozes=96, rugido=0.6, longe=1.0)
    vis = filtra(vis, 'pb', 1300)
    x = junta((cama(CAMA + 0.15), 0, 0.5), (queda, CAMA, 1.6), (resto, CAMA + 1.0, 0.25), (vis, CAMA + 0.2, 0.25))
    return final(x)


def uuuh3():
    """Quase gol: "UUUH!" curto e de susto (sobe rapido, cai), depois palmas de incentivo."""
    dur = 1.5
    u = torcida(dur, [(0, 0), (0.08, 0.9), (0.35, 1.0), (0.8, 0.35), (dur, 0)], (150, 260),
                [(0, 'u')], n_vozes=200, rugido=0.35, longe=0.5, sopro=0.55,
                curva_tom=[(0, 0.85), (0.3, 1.22), (dur, 0.8)])
    p = palmas(int(2.0 * SR), 220) * curva(int(2.0 * SR), [(0, 0), (0.3, 1), (2.0, 0)])
    x = junta((cama(CAMA + 0.1), 0, 0.5), (u, CAMA, 2.0), (p, CAMA + 0.9, 0.5), (cama(1.8), CAMA + 1.2, 0.45))
    return final(x)


def penalti3():
    """Apito, um "OH!" curto e a torcida prendendo o ar: o barulho cai e sobra o murmurio."""
    a = apitos2([(0, 0.6, 1.0)], 'bolinha')
    oh = torcida(1.0, [(0, 0), (0.05, 1.0), (0.3, 0.6), (1.0, 0)], (170, 270), [(0, 'o')], n_vozes=160,
                 rugido=0.4, longe=0.5, curva_tom=[(0, 1.0), (1.0, 0.85)])
    tenso = torcida(2.6, [(0, 0), (0.4, 0.4), (2.6, 0.25)], (100, 180), [(0, 'e')], n_vozes=128,
                    rugido=0.8, longe=0.85, fala=6.0)
    x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.5), (oh, CAMA + 0.15, 1.3), (tenso, CAMA + 0.9, 0.3))
    return final(x)


def expulsao3():
    """Dois apitos, vaia grave e assobios."""
    a = apitos2([(0, 0.17, 1.0), (0.26, 0.55, 1.0)], 'bolinha')
    v = torcida(3.0, [(0, 0), (0.35, 1.0), (2.3, 0.85), (3.0, 0)], (95, 165), [(0, 'u')],
                rugido=0.5, longe=0.55, sopro=0.35, curva_tom=[(0, 1.0), (3.0, 0.95)])
    x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.5), (v, CAMA + 0.5, 1.6), (assobios(3.0, 45), CAMA + 0.5, 1.2))
    return final(x)


def contusao3():
    """Apito curto, a torcida baixa a voz e conversa preocupada; palmas quando o jogador levanta."""
    a = apitos2([(0, 0.3, 0.85)], 'bolinha')
    m = torcida(2.8, [(0, 0), (0.4, 0.35), (2.8, 0.25)], (100, 190), [(0, 'o'), (0.6, 'e')], n_vozes=128,
                rugido=0.7, longe=0.8, fala=4.5)
    p = palmas(int(1.6 * SR), 140) * curva(int(1.6 * SR), [(0, 0), (0.3, 1), (1.6, 0)])
    x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.2), (m, CAMA + 0.3, 0.35), (p, CAMA + 2.0, 0.4))
    return final(x)


def intervalo3():
    """Dois apitos e a torcida se ajeita: conversa e algumas palmas (sem comemorar)."""
    a = apitos2([(0, 0.35, 1.0), (0.5, 0.9, 1.0)], 'bolinha')
    m = torcida(2.6, [(0, 0.3), (0.6, 0.5), (2.6, 0)], (110, 220), [(0, 'e')], n_vozes=128,
                rugido=0.7, longe=0.85, fala=5.0)
    p = palmas(int(2.0 * SR), 90) * curva(int(2.0 * SR), [(0, 0), (0.5, 1), (2.0, 0)])
    x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.5), (m, CAMA + 1.2, 0.45), (p, CAMA + 1.3, 0.4))
    return final(x)


def fimjogo3():
    """Dois curtos e um longo; a torcida aplaude e comeca a ir embora (sem gritar)."""
    a = apitos2([(0, 0.22, 1.0), (0.34, 0.22, 1.0), (0.7, 1.3, 1.0)], 'bolinha')
    m = torcida(3.2, [(0, 0.3), (0.8, 0.55), (3.2, 0)], (110, 220), [(0, 'e')], n_vozes=128,
                rugido=0.7, longe=0.85, fala=5.0)
    p = palmas(int(3.0 * SR), 200) * curva(int(3.0 * SR), [(0, 0), (0.6, 1), (3.0, 0)])
    x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.5), (m, CAMA + 1.6, 0.45), (p, CAMA + 1.7, 0.6))
    return final(x)


def trave3(v):
    """Bola na trave ('a' = "tonc" surdo, 'b' = "ping" metalico) e o "UUUH!" novo."""
    d = int(1.2 * SR)
    t = np.arange(d) / SR
    x = np.zeros(d)
    modos = ([(310, 1.0, 0.16), (742, 0.55, 0.09), (1290, 0.35, 0.05), (2050, 0.2, 0.03)] if v == 'a'
             else [(620, 0.8, 0.35), (1480, 0.6, 0.22), (2390, 0.45, 0.14), (3570, 0.25, 0.08)])
    for f, a, tau in modos:
        x += a * np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) * np.exp(-t / tau)
    x += np.sin(2 * np.pi * np.cumsum(80 + 90 * np.exp(-t / 0.01)) / SR) * np.exp(-t / 0.04) * 0.8
    x += filtra(rng.standard_normal(d), 'bp', 1500, 1.2) * np.exp(-t / 0.006) * 0.7
    x = reverb(x, seg=1.6, mix=0.3, brilho=5000)
    u = torcida(1.5, [(0, 0), (0.08, 0.9), (0.35, 1.0), (0.8, 0.35), (1.5, 0)], (150, 260),
                [(0, 'u')], n_vozes=200, rugido=0.35, longe=0.5, sopro=0.55,
                curva_tom=[(0, 0.85), (0.3, 1.22), (1.5, 0.8)])
    return normaliza(junta((x, 0, 1.0), (u, 0.1, 0.6)), -1, rms_db=-16)


# ------------------------------------------------------------------ versao 4: gol, gol sofrido, contusao, intervalo, fim
# Aprovados na v3: quase gol, penalti e expulsao (curtos, um clima so). As opcoes abaixo
# seguem o mesmo jeito.

def gol4(v):
    """'a' = explosao curta e seca (2,8 s); 'b' = explosao e a torcida cantando no ritmo do surdo."""
    if v == 'a':
        dur = 2.8
        x = torcida(dur, [(0, 0.05), (0.08, 0.9), (0.3, 1.0), (1.6, 0.8), (dur, 0)], (230, 380),
                    [(0, 'a'), (0.3, 'o')], n_vozes=280, rugido=0.4, longe=0.45, sopro=0.5,
                    curva_tom=[(0, 0.8), (0.25, 1.15), (dur, 0.95)])
        p = palmas(int(2.2 * SR), 300) * curva(int(2.2 * SR), [(0, 0), (0.4, 1), (2.2, 0)])
        x = junta((cama(CAMA + 0.1), 0, 0.5), (x, CAMA, 4.5), (p, CAMA + 0.5, 2.5))
    else:
        dur = 5.5
        x = torcida(2.6, [(0, 0.05), (0.08, 0.9), (0.3, 1.0), (1.8, 0.7), (2.6, 0.2)], (230, 380),
                    [(0, 'a'), (0.3, 'o')], n_vozes=280, rugido=0.4, longe=0.45, sopro=0.5,
                    curva_tom=[(0, 0.8), (0.25, 1.15), (2.6, 0.95)])
        # canto: a torcida grita junto no tempo do surdo (2 por segundo)
        n = int(3.6 * SR)
        batida = lambda t: np.clip(np.sin(np.pi * ((t * 2.0) % 1.0)) * 1.4, 0, 1) ** 2 * np.interp(t, [0, 0.3, 3.0, 3.6], [0, 1, 1, 0])
        canto = torcida(3.6, [(0, 1), (3.6, 1)], (180, 300), [(0, 'e'), (0.25, 'o')], n_vozes=200,
                        rugido=0.3, longe=0.5, sopro=0.45)
        canto = canto[:, :n] * batida(np.arange(n) / SR)
        s = filtra(filtra(surdo(n, 120), 'pb', 700), 'pa', 120)
        x = junta((cama(CAMA + 0.1), 0, 0.5), (x, CAMA, 4.5), (canto, CAMA + 1.8, 1.8), (s, CAMA + 1.8, 1.4))
    return final(x)


def lamento4(v):
    """'a' = "aaahh" coletivo forte e curto, depois quase silencio; 'b' = o mesmo com
    assobios de irritacao e uma vaia curta."""
    queda = torcida(1.3, [(0, 0), (0.05, 1.0), (0.35, 0.6), (1.3, 0)], (115, 200), [(0, 'a')],
                    n_vozes=240, rugido=0.4, longe=0.5, sopro=0.55, curva_tom=[(0, 1.12), (1.3, 0.7)])
    partes = [(cama(CAMA + 0.1), 0, 0.5), (queda, CAMA, 2.2),
              (torcida(1.6, [(0, 0), (0.4, 0.3), (1.6, 0.2)], (100, 190), [(0, 'e')], n_vozes=96,
                       rugido=0.8, longe=0.9, fala=4.0), CAMA + 1.0, 0.22)]
    if v == 'b':
        partes.append((assobios(1.8, 14), CAMA + 0.7, 0.8))
        partes.append((vaia2(0.6, 1.4), CAMA + 0.8, 0.7))
    return final(junta(*partes))


def contusao4(v):
    """'a' = apito e um "ohh" de pena (grave, caindo); 'b' = apito e palmas de apoio."""
    a = apitos2([(0, 0.3, 0.85)], 'bolinha')
    if v == 'a':
        oh = torcida(1.6, [(0, 0), (0.15, 0.8), (0.6, 0.6), (1.6, 0)], (120, 210), [(0, 'o')], n_vozes=200,
                     rugido=0.45, longe=0.6, sopro=0.55, curva_tom=[(0, 1.05), (1.6, 0.78)])
        x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.2), (oh, CAMA + 0.35, 1.3))
    else:
        p = palmas(int(2.2 * SR), 260) * curva(int(2.2 * SR), [(0, 0), (0.3, 1), (1.6, 0.8), (2.2, 0)])
        x = junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.2), (p, CAMA + 0.5, 4.0))
    return final(x)


def intervalo4(v):
    """'a' = so os dois apitos com o eco do estadio; 'b' = apitos e aplausos."""
    a = apitos2([(0, 0.35, 1.0), (0.5, 0.9, 1.0)], 'bolinha', longe=0.6)
    if v == 'a':
        return final(junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.8)))
    p = palmas(int(2.4 * SR), 320) * curva(int(2.4 * SR), [(0, 0), (0.4, 1), (2.4, 0)])
    return final(junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.8), (p, CAMA + 1.3, 4.0)))


def fimjogo4(v):
    """'a' = so os tres apitos com o eco; 'b' = apitos e aplausos."""
    a = apitos2([(0, 0.22, 1.0), (0.34, 0.22, 1.0), (0.7, 1.3, 1.0)], 'bolinha', longe=0.6)
    if v == 'a':
        return final(junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.8)))
    p = palmas(int(3.0 * SR), 360) * curva(int(3.0 * SR), [(0, 0), (0.5, 1), (3.0, 0)])
    return final(junta((cama(CAMA + 0.1), 0, 0.5), (a, CAMA, 2.8), (p, CAMA + 1.9, 4.5)))


# ------------------------------------------------------------------ versao 5: gols
# Os gols com vozes segurando vogal nao convenceram. Aqui o gol vem do que soa real em
# sintese (rugido, palmas, assobios, rojao, surdo) e de gritos curtos, como o "OH!" e o
# "UUUH!" aprovados; o gol sofrido usa o silencio da torcida.

def festa_assobios(dur, quantos=30):
    """Assobios de festa: curtos e subindo."""
    n = int(dur * SR)
    out = np.zeros((2, n))
    for _ in range(quantos):
        d = rng.uniform(0.25, 0.6)
        k = int(d * SR)
        i = int(rng.uniform(0, dur - d) * SR)
        tt = np.arange(k) / SR
        f0 = rng.uniform(2000, 3000)
        f = f0 * (1 + 0.25 * tt / d)
        x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.interp(tt, [0, 0.03, d - 0.05, d], [0, 1, 0.9, 0])
        out[:, i: i + k] += estereo(x, rng.uniform(-0.9, 0.9)) * rng.uniform(0.2, 0.6)
    return reverb(filtra(out, 'pb', 5000), seg=2.2, mix=0.4)


def gol5(v):
    """'a' = rugido do estadio: barulho que sobe de uma vez, palmas, assobios e rojoes;
    'b' = um "E!" curto de todos juntos e a festa (palmas, surdo, rojoes)."""
    dur = 3.6
    n = int(dur * SR)
    if v == 'a':
        r = torcida(dur, [(0, 0.05), (0.1, 0.9), (0.4, 1.0), (2.2, 0.75), (dur, 0)], (220, 360),
                    [(0, 'a')], n_vozes=160, rugido=0.85, longe=0.5, sopro=0.6,
                    curva_tom=[(0, 0.85), (0.3, 1.1), (dur, 0.95)])
        p = palmas(n, 420) * curva(n, [(0, 0), (0.3, 1), (2.6, 0.8), (dur, 0)])
        x = junta((cama(CAMA + 0.1), 0, 0.5), (r, CAMA, 3.5), (p, CAMA + 0.2, 3.0),
                  (festa_assobios(dur, 30), CAMA + 0.3, 1.0), (rojao(5), CAMA + 0.6, 0.8))
    else:
        e = torcida(0.9, [(0, 0), (0.04, 1.0), (0.3, 0.7), (0.9, 0)], (230, 360), [(0, 'e')],
                    n_vozes=240, rugido=0.35, longe=0.45, sopro=0.55,
                    curva_tom=[(0, 0.9), (0.15, 1.15), (0.9, 1.0)])
        resto = torcida(dur, [(0, 0.6), (2.6, 0.5), (dur, 0)], (200, 320), [(0, 'a')], n_vozes=128,
                        rugido=0.85, longe=0.65)
        p = palmas(n, 420) * curva(n, [(0, 0), (0.2, 1), (2.8, 0.8), (dur, 0)])
        s = filtra(filtra(surdo(n, 128), 'pb', 700), 'pa', 120) * curva(n, [(0, 0), (0.3, 1), (dur, 0.6)])
        x = junta((cama(CAMA + 0.1), 0, 0.5), (e, CAMA, 3.2), (resto, CAMA + 0.5, 1.6), (p, CAMA + 0.4, 3.0),
                  (s, CAMA + 0.5, 1.4), (rojao(5), CAMA + 0.7, 0.8))
    return final(x)


def lamento5(v):
    """'a' = um "Ô" curto que cai (como o "OH!" do penalti) e a torcida murcha;
    'b' = a torcida para de uma vez e so se ouve a outra comemorando la longe."""
    if v == 'a':
        o = torcida(0.8, [(0, 0), (0.04, 1.0), (0.25, 0.5), (0.8, 0)], (130, 220), [(0, 'o')],
                    n_vozes=200, rugido=0.4, longe=0.5, sopro=0.55, curva_tom=[(0, 1.05), (0.8, 0.78)])
        resto = torcida(2.4, [(0, 0), (0.4, 0.3), (2.4, 0.25)], (100, 190), [(0, 'e')], n_vozes=96,
                        rugido=0.85, longe=0.9, fala=3.5)
        return final(junta((cama(CAMA + 0.1), 0, 0.6), (o, CAMA, 1.6), (resto, CAMA + 0.5, 0.25)))
    vis = torcida(3.0, [(0, 0), (0.2, 1.0), (2.2, 0.8), (3.0, 0)], (230, 340), [(0, 'a')],
                  n_vozes=128, rugido=0.8, longe=1.0)
    vis = filtra(vis, 'pb', 1400)
    p = filtra(palmas(int(2.6 * SR), 200), 'pb', 1600)
    resto = torcida(3.0, [(0, 0.15), (3.0, 0.15)], (100, 190), [(0, 'e')], n_vozes=64,
                    rugido=0.9, longe=0.95, fala=3.0)
    return final(junta((cama(CAMA), 0, 1.0), (resto, CAMA, 0.25), (vis, CAMA + 0.1, 0.22), (p, CAMA + 0.3, 0.35)))


SONS = {
    # partida (nomes do jogo)
    'gol1': gol, 'goladv': lamento, 'penalty': penalti, 'expulsao': expulsao,
    'contusao': contusao, 'intervalo': intervalo, 'fimjogo': fimjogo,
    # novos
    'inicio': inicio, 'ambiente': ambiente, 'uuuh': uuuh,
    'chute': chute, 'rede': rede, 'trave': trave, 'defesa': defesa,
    'clique': clique, 'passa': passa, 'moedas': moedas, 'mensagem': mensagem,
    'carimbo': carimbo, 'carta': carta, 'janela': janela, 'fanfarra': fanfarra,
}
# versao 2: opcoes para o Leo escolher (nome_a, nome_b)
for _v in 'ab':
    for _n, _f in [('gol1', gol2), ('goladv', lamento2), ('uuuh', uuuh2), ('intervalo', intervalo2),
                   ('fimjogo', fimjogo2), ('penalty', penalti2), ('expulsao', expulsao2),
                   ('contusao', contusao2), ('trave', trave2)]:
        SONS[_n + '_' + _v] = (lambda f, v: (lambda: f(v)))(_f, _v)
# versao 3: um clima para cada reacao
SONS.update({'gol1_3': gol3, 'goladv_3': lamento3, 'uuuh_3': uuuh3, 'penalty_3': penalti3,
             'expulsao_3': expulsao3, 'contusao_3': contusao3, 'intervalo_3': intervalo3,
             'fimjogo_3': fimjogo3, 'trave_3a': lambda: trave3('a'), 'trave_3b': lambda: trave3('b')})

# versao 4
for _v in 'ab':
    for _n, _f in [('gol1', gol4), ('goladv', lamento4), ('contusao', contusao4),
                   ('intervalo', intervalo4), ('fimjogo', fimjogo4)]:
        SONS[_n + '_4' + _v] = (lambda f, v: (lambda: f(v)))(_f, _v)

# versao 5
for _v in 'ab':
    SONS['gol1_5' + _v] = (lambda v: (lambda: gol5(v)))(_v)
    SONS['goladv_5' + _v] = (lambda v: (lambda: lamento5(v)))(_v)

if __name__ == '__main__':
    pasta = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..', '..', 'mod', 'assets', 'mod', 'sons')
    so = sys.argv[2:]
    os.makedirs(pasta, exist_ok=True)
    for nome, f in SONS.items():
        if so and nome not in so:
            continue
        rng = np.random.default_rng(sum(map(ord, nome)) * 7919)
        grava(nome, f(), pasta)
        print(nome)
