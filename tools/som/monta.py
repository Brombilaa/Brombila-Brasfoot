"""Monta os sons escolhidos pelo Leo em mod/assets/mod/sons (vao dentro do .exe).

- jogo/: os 7 sons que o proprio jogo toca (lidos do disco, pasta sons/). O mod.Boot
  copia estes para a pasta sons/ do jogo ao abrir (mesmo jeito dos fundos).
- raiz: os sons novos do mod (mod.Sons).
Os sons sintetizados saem sem a "cama" de torcida das previas, porque no jogo a torcida
ambiente ja esta tocando. Gol e quase gol sao arquivos do Leo (tools/som/leo).
Tudo em PCM 16 bits estereo (o Java so toca PCM); 32 kHz, a torcida ambiente 22,05 kHz.
uso: python3 tools/som/monta.py
"""
import os
import subprocess
import sys
import tempfile

sys.argv = sys.argv[:1]
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np  # noqa: E402
import sons  # noqa: E402

AQUI = os.path.dirname(os.path.abspath(__file__))
DESTINO = os.path.join(AQUI, '..', '..', 'mod', 'assets', 'mod', 'sons')

# nome no jogo -> nome em sons.SONS
JOGO = {'goladv': 'goladv_5a', 'penalty': 'penalty_3', 'expulsao': 'expulsao_3',
        'contusao': 'contusao_4a', 'intervalo': 'intervalo_4a', 'fimjogo': 'fimjogo_4b'}
NOVOS = {'inicio': 'inicio', 'ambiente': 'ambiente', 'chute': 'chute', 'rede': 'rede',
         'trave': 'trave_3a', 'defesa': 'defesa', 'clique': 'clique', 'passa': 'passa',
         'moedas': 'moedas', 'mensagem': 'mensagem', 'carimbo': 'carimbo', 'carta': 'carta',
         'janela': 'janela', 'fanfarra': 'fanfarra'}
# arquivos do Leo: (origem, destino, ganho dB)
LEO = [('gol.mp3', 'jogo/gol1', 0.0), ('uuh.mp3', 'uuuh', -3.0)]


def converte(origem, destino, taxa=32000, filtro=''):
    af = filtro + ('' if not filtro else ',') + 'aformat=sample_fmts=s16:channel_layouts=stereo'
    subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', origem, '-af', af, '-ar', str(taxa),
                    '-c:a', 'pcm_s16le', '-map_metadata', '-1', '-fflags', '+bitexact', destino], check=True)


def main():
    sons.SEM_CAMA = True
    os.makedirs(os.path.join(DESTINO, 'jogo'), exist_ok=True)
    tmp = tempfile.mkdtemp()
    for pasta, tabela in (('jogo', JOGO), ('', NOVOS)):
        for nome, fonte in tabela.items():
            sons.rng = np.random.default_rng(sum(map(ord, fonte)) * 7919)
            sons.grava(nome, sons.SONS[fonte](), tmp)
            converte(os.path.join(tmp, nome + '.wav'), os.path.join(DESTINO, pasta, nome + '.wav'),
                     22050 if nome == 'ambiente' else 32000)
            print(os.path.join(pasta, nome))
    corta = ('silenceremove=start_periods=1:start_threshold=-50dB,areverse,'
             'silenceremove=start_periods=1:start_threshold=-70dB,afade=t=in:d=0.15,areverse')
    for origem, destino, ganho in LEO:
        converte(os.path.join(AQUI, 'leo', origem), os.path.join(DESTINO, destino + '.wav'), 32000,
                 corta + (',volume=%gdB' % ganho if ganho else ''))
        print(destino)


if __name__ == '__main__':
    main()
