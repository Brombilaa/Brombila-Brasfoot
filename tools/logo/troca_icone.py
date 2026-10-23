# Troca o icone do .exe (recurso RT_ICON do stub Launch4j, 150x150 32 bits)
# pelo icone novo (mod/assets/mod/logo/icone-256.png), no mesmo lugar e tamanho.
# Uso: python tools/logo/troca_icone.py jogo/BrombilaBrasfoot.exe
import struct, sys
import pefile
from PIL import Image

STUB = 125440
exe = sys.argv[1]
icone = sys.argv[2] if len(sys.argv) > 2 else 'mod/assets/mod/logo/icone-256.png'
d = bytearray(open(exe, 'rb').read())
pe = pefile.PE(data=bytes(d[:STUB]))
for t in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if t.id != 3:  # RT_ICON
        continue
    for e in t.directory.entries:
        for l in e.directory.entries:
            off = pe.get_offset_from_rva(l.data.struct.OffsetToData)
            tam, w, h2, planos, bpp = struct.unpack('<IiiHH', d[off:off + 16])
            if tam != 40 or bpp != 32 or h2 != 2 * w:
                raise SystemExit('formato inesperado: %r' % ((tam, w, h2, bpp),))
            img = Image.open(icone).convert('RGBA').resize((w, w), Image.LANCZOS)
            px = img.load()
            cor = bytearray()
            for y in range(w - 1, -1, -1):  # BMP de baixo para cima, BGRA
                for x in range(w):
                    r, g, b, a = px[x, y]
                    cor += bytes((b, g, r, a))
            linha = ((w + 31) // 32) * 4
            mascara = bytes(linha * w)  # tudo visivel; a transparencia vem do alfa
            novo = cor + mascara
            ini = off + tam
            if len(novo) != l.data.struct.Size - tam:
                raise SystemExit('tamanho diferente')
            d[ini:ini + len(novo)] = novo
            print('icone %dx%d trocado' % (w, w))
open(exe, 'wb').write(bytes(d))
