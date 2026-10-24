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

if __name__ == '__main__':
    pasta = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..', '..', 'mod', 'assets', 'mod', 'sons')
    so = sys.argv[2:]
    os.makedirs(pasta, exist_ok=True)
    for nome, f in SONS.items():
        if so and nome not in so:
            continue
        grava(nome, f(), pasta)
        print(nome)
