# Temporada de inicio 2026 ou 2027 no lugar de 2022 ou 2023.
#
# O ano de inicio do jogo e best.f.rM (op()/I()). A tela Configuracoes iniciais (a.hU.oq) grava
# I(2022) ou I(2023) conforme o combo, e o comeco do jogo novo (best.p.dg) faz op() == 2023 ->
# SR.rU(), que adianta a contagem da Copa do Mundo, Euro etc. (em 2023 a Copa vem na 4a
# temporada). Como esses torneios sao contados de 4 em 4 temporadas, 2026 tem as mesmas contas
# de 2022 e 2027 as de 2023. Entao aqui so os numeros mudam: cada "sipush 2022/2023" vira
# invokestatic mod.Ano.primeira()/segunda() (3 bytes por 3 bytes, mesmo efeito na pilha).
# Saves antigos guardam o proprio ano e nao passam por aqui.
#
# Uso: python3 tools/ano/remenda_ano.py <jar do jogo> <pasta de saida: mod/assets>
import os
import struct
import sys
import zipfile

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "escudos"))
from remenda_ah import le_pool  # noqa: E402

TROCAS = {
    "a/hU.class": {2022: "primeira", 2023: "segunda"},
    "best/p.class": {2023: "segunda"},
}


def remenda(b, trocas):
    n, fim, pool = le_pool(b)
    novo = bytearray()
    k = n

    def utf(s):
        nonlocal k
        e = s.encode("utf-8")
        novo.extend(b"\x01" + struct.pack(">H", len(e)) + e)
        k += 1
        return k - 1

    u_cls = utf("mod/Ano")
    novo.extend(b"\x07" + struct.pack(">H", u_cls)); i_cls = k; k += 1
    u_ds = utf("()I")
    resto = bytes(b[fim:])
    for ano, metodo in trocas.items():
        u_nm = utf(metodo)
        novo.extend(b"\x0c" + struct.pack(">HH", u_nm, u_ds)); i_nt = k; k += 1
        novo.extend(b"\x0a" + struct.pack(">HH", i_cls, i_nt)); i_m = k; k += 1
        velho = b"\x11" + struct.pack(">h", ano)
        achados = resto.count(velho)
        assert achados == 1, "sipush %d: esperava 1, achei %d" % (ano, achados)
        resto = resto.replace(velho, b"\xb8" + struct.pack(">H", i_m))
    return bytes(b[:8]) + struct.pack(">H", k) + bytes(b[10:fim]) + bytes(novo) + resto


def main():
    jar, saida = sys.argv[1], sys.argv[2]
    z = zipfile.ZipFile(jar)
    for cls, trocas in TROCAS.items():
        out = remenda(bytearray(z.read(cls)), trocas)
        dest = os.path.join(saida, cls)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        open(dest, "wb").write(out)
        print("ok", dest)


if __name__ == "__main__":
    main()
