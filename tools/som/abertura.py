"""Som da abertura (MenuInicial.abertura), sintetizado do zero com numpy.

Tempos iguais aos do Java: bola voa de 0,12 s ate a batida em 1,1 s, flashes de camera
(Random(7) do Java) de 1,15 a 2,55 s, brilho em 1,45 s, logo sobe de 2,6 a 3,65 s.
Saida: mod/assets/mod/sons/abertura.wav (44,1 kHz, estereo, 16 bits).
uso: python3 tools/som/abertura.py
"""
import os
import wave

import numpy as np

SR = 44100
DUR = 5.6
N = int(SR * DUR)
BATE, SOBE = 1.1, 2.6
rng = np.random.default_rng(22)
t = np.arange(N) / SR


def ruido(n=N):
    return rng.standard_normal(n)


def rosa(n=N):
    # ruido rosa por FFT (1/f)
    f = np.fft.rfft(rng.standard_normal(n))
    k = np.arange(len(f)); k[0] = 1
    return np.fft.irfft(f / np.sqrt(k), n)


def banda(x, lo, hi, suave=1.0):
    f = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    g = 1 / (1 + (lo / np.maximum(fr, 1)) ** (4 * suave)) / (1 + (fr / hi) ** (4 * suave))
    return np.fft.irfft(f * g, len(x))


def svf(x, fc, q=0.7):
    """Passa-banda de estado variavel com frequencia mudando por amostra."""
    lp = bp = 0.0
    out = np.empty_like(x)
    for i in range(len(x)):
        f = 2 * np.sin(np.pi * min(fc[i], SR / 6) / SR)
        hp = x[i] - lp - q * bp
        bp += f * hp
        lp += f * bp
        out[i] = bp
    return out


def env(a, b, ataque, queda, forma=2.0):
    """Envelope: sobe de a ate a+ataque, desce ate b."""
    e = np.zeros(N)
    s = (t >= a) & (t < a + ataque)
    e[s] = ((t[s] - a) / ataque) ** forma
    s = (t >= a + ataque) & (t < b)
    e[s] = 1.0
    s = (t >= b) & (t < b + queda)
    e[s] = 1 - ((t[s] - b) / queda)
    return e


def pan(x, p):
    """p de -1 (esquerda) a 1 (direita), lei de potencia constante."""
    a = (np.asarray(p) + 1) * np.pi / 4
    return np.stack([x * np.cos(a), x * np.sin(a)])


def reverb(x, seg=1.6, mix=0.35):
    n = int(seg * SR)
    ir = rng.standard_normal(n) * np.exp(-np.arange(n) / SR * 5.5 / seg)
    ir = banda(np.concatenate([ir, np.zeros(len(x))]), 120, 6000)[:n]
    ir /= np.sqrt(np.sum(ir ** 2))
    m = len(x) + n
    y = np.fft.irfft(np.fft.rfft(x, m) * np.fft.rfft(ir, m), m)[:len(x)]
    return x * (1 - mix) + y * mix


def random_java(seed):
    mult, mask = 0x5DEECE66D, (1 << 48) - 1
    s = [(seed ^ mult) & mask]

    def next_float():
        s[0] = (s[0] * mult + 0xB) & mask
        return (s[0] >> 24) / float(1 << 24)
    return next_float


saida = np.zeros((2, N))

# 1) torcida ao fundo desde o comeco (murmurio), cresce depois da batida
mur = banda(rosa(), 180, 1400)
mod_lento = 1 + 0.25 * banda(ruido(), 0.3, 3)  # variacao lenta
mur *= mod_lento / np.std(mur)
nivel = 0.035 * np.clip(t / 0.4, 0, 1)
nivel += 0.05 * np.clip((t - BATE) / 0.5, 0, 1) ** 1.5
nivel *= np.clip((DUR - 0.25 - t) / 1.8, 0, 1)
for c in range(2):
    m = banda(rosa(), 180, 1400) * mod_lento
    saida[c] += nivel * m / np.std(m)

# 2) "uuuh" da torcida: varias vozes (pulsos filtrados em vogal), estoura na batida
vozes = np.zeros((2, N))
for v in range(36):
    f0 = rng.uniform(140, 330)
    vib = 1 + 0.03 * np.sin(2 * np.pi * rng.uniform(4, 7) * t + rng.uniform(0, 6))
    deslize = 1 + 0.10 * np.clip((t - BATE) / 0.6, 0, 1)
    fase = 2 * np.pi * np.cumsum(f0 * vib * deslize) / SR
    onda = np.sign(np.sin(fase)) * 0.3 + np.sin(fase)
    comeca = BATE + rng.uniform(-0.02, 0.18)
    e = env(comeca, comeca + rng.uniform(1.4, 2.6), rng.uniform(0.15, 0.35), rng.uniform(1.0, 1.8), 1.5)
    vozes += pan(onda * e * rng.uniform(0.6, 1.0), rng.uniform(-0.8, 0.8))
for c in range(2):
    x = banda(vozes[c], 250, 1100) * 0.6 + banda(vozes[c], 1800, 3200) * 0.25
    saida[c] += 0.07 * x / (np.std(x) + 1e-9)
grito = banda(rosa(), 500, 3500)
grito /= np.std(grito)
eg = env(BATE, BATE + 1.2, 0.25, 2.0, 1.2)
saida += pan(0.07 * grito * eg, 0.0)

# 3) bola voando: sopro que acelera e vem da esquerda para o centro
k = np.clip((t - 0.12) / (BATE - 0.12), 0, 1)
fc = 350 + 2600 * k ** 2
sopro = svf(ruido(), fc, 0.45)
ev = np.where((t > 0.12) & (t < BATE + 0.04), k ** 2.2, 0.0)
ev *= np.clip((BATE + 0.04 - t) / 0.04, 0, 1)
giro = 1 + 0.35 * np.sin(2 * np.pi * (6 + 18 * k) * t)  # "fu-fu-fu" da bola girando
saida += pan(0.22 * sopro / np.std(sopro[ev > 0.1]) * ev * giro, -0.85 + 0.85 * k)

# 4) batida: chute grave + estalo + boom com eco
d = t - BATE
pos = d >= 0
chute = np.zeros(N)
fq = 48 + 110 * np.exp(-np.maximum(d, 0) / 0.04)
fase = 2 * np.pi * np.cumsum(np.where(pos, fq, 0)) / SR
chute[pos] = np.sin(fase[pos]) * np.exp(-d[pos] / 0.22)
estalo = banda(ruido(), 1500, 9000) * np.where(pos, np.exp(-np.maximum(d, 0) / 0.006), 0)
boom = banda(rosa(), 30, 180)
boom = boom / np.std(boom) * np.where(pos, np.exp(-np.maximum(d, 0) / 0.7), 0) * np.clip(d / 0.01, 0, 1)
batida = 1.0 * chute + 0.30 * estalo / np.std(estalo[pos][:400]) + 0.35 * boom
batida = reverb(batida, 1.8, 0.4)
saida += pan(batida, 0.0)

# 5) cliques de camera junto com os flashes (mesmo sorteio do Java)
rnd = random_java(7)
for i in range(18):
    quando = BATE + 0.05 + rnd() * 1.4
    fx = rnd(); rnd(); rnd()
    for j, atraso in enumerate((0.0, 0.035)):
        i0 = int((quando + atraso) * SR)
        n = int(0.012 * SR)
        c = banda(ruido(n * 4), 2500, 12000)[:n] * np.exp(-np.arange(n) / SR / 0.0025)
        c *= 0.10 * (1 if j == 0 else 0.6) / (np.max(np.abs(c)) + 1e-9)
        p = fx * 2 - 1
        a = (p + 1) * np.pi / 4
        saida[0, i0:i0 + n] += c * np.cos(a)
        saida[1, i0:i0 + n] += c * np.sin(a)

# 6) brilho dourado: sininho suave quando a luz atravessa o logo
d = t - 1.48
pos = d >= 0
sino = np.zeros(N)
for f, a, dec in ((1568, 0.5, 0.9), (2349, 0.35, 0.7), (3136, 0.25, 0.5), (4699, 0.15, 0.35)):
    sino += a * np.sin(2 * np.pi * f * d) * np.where(pos, np.exp(-np.maximum(d, 0) / dec), 0)
sino *= np.clip(d / 0.008, 0, 1)
saida += pan(reverb(0.06 * sino, 1.2, 0.5), 0.15)

# 7) logo subindo: sopro suave que sobe e assenta
k2 = np.clip((t - SOBE) / 1.05, 0, 1)
sopro2 = svf(ruido(), 300 + 1500 * np.sin(np.pi * k2) ** 1.2 + 0 * t, 0.6)
e2 = np.where((t > SOBE) & (t < SOBE + 1.1), np.sin(np.pi * k2) ** 2, 0)
saida += pan(0.05 * sopro2 / np.std(sopro2[e2 > 0.1]) * e2, 0.0)

# mixagem final: limitador suave e pico em -1 dBFS; 15 ms de entrada e 300 ms de saida
saida = np.tanh(saida * 1.1) / np.tanh(1.1)
saida *= 10 ** (-1 / 20) / np.max(np.abs(saida))
saida *= np.clip(t / 0.015, 0, 1) * np.clip((DUR - t) / 0.3, 0, 1)

raiz = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
dest = os.path.join(raiz, 'mod', 'assets', 'mod', 'sons', 'abertura.wav')
os.makedirs(os.path.dirname(dest), exist_ok=True)
pcm = (np.clip(saida.T, -1, 1) * 32767).astype('<i2')
with wave.open(dest, 'wb') as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print(dest, os.path.getsize(dest))
