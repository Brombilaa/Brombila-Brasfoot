# Enxuga PNGs sem mexer na imagem: tira os blocos de texto/metadados (tEXt, zTXt, iTXt, tIME,
# eXIf) e recomprime os dados (IDAT) com zlib nivel 9. Os bytes da imagem descomprimida sao os
# mesmos (o script confere), e os blocos de cor (iCCP, cHRM, gAMA, sRGB) ficam como estao.
#
# Uso: python3 tools/limpeza/png_enxuto.py <pasta ou arquivo> [...]
import os
import struct
import sys
import zlib

TIRA = {b'tEXt', b'zTXt', b'iTXt', b'tIME', b'eXIf'}
ASSINATURA = b'\x89PNG\r\n\x1a\n'


def blocos(d):
    p = 8
    while p < len(d):
        n, t = struct.unpack('>I4s', d[p:p + 8])
        yield t, d[p + 8:p + 8 + n]
        p += 12 + n


def bloco(t, dados):
    return struct.pack('>I', len(dados)) + t + dados + struct.pack('>I', zlib.crc32(t + dados) & 0xffffffff)


def enxuga(d):
    if d[:8] != ASSINATURA:
        return None
    lista = list(blocos(d))
    idat = b''.join(x for t, x in lista if t == b'IDAT')
    bruto = zlib.decompress(idat)
    novo_idat = zlib.compress(bruto, 9)
    if len(novo_idat) >= len(idat):
        novo_idat = idat
    assert zlib.decompress(novo_idat) == bruto
    out = [ASSINATURA]
    pos = False
    for t, x in lista:
        if t in TIRA:
            continue
        if t == b'IDAT':
            if not pos:
                out.append(bloco(b'IDAT', novo_idat))
                pos = True
            continue
        out.append(bloco(t, x))
    r = b''.join(out)
    return r if len(r) < len(d) else None


def main():
    antes = depois = n = 0
    for alvo in sys.argv[1:]:
        caminhos = [alvo] if os.path.isfile(alvo) else [os.path.join(r, f) for r, _, fs in os.walk(alvo) for f in fs]
        for c in caminhos:
            if not c.lower().endswith('.png'):
                continue
            d = open(c, 'rb').read()
            novo = enxuga(d)
            antes += len(d)
            if novo is None:
                depois += len(d)
                continue
            open(c, 'wb').write(novo)
            depois += len(novo)
            n += 1
    print('%d arquivos enxugados: %.1f MB -> %.1f MB' % (n, antes / 1e6, depois / 1e6))


if __name__ == '__main__':
    main()
