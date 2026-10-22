# Liga o arquivo da carreira do mod (mod.Carreira, sav/<nome>.bbf) no salvar e no carregar do jogo.
#
# O jogo salva em c.a.ae(String) (chamado so por a.hz) e carrega em c.a.c(String, boolean)
# (chamado so por a.dR). Aqui cada chamada vira mod.Carreira.salva / mod.Carreira.carrega, que
# tem a mesma assinatura: chamam o metodo original do jogo e depois gravam ou leem o .bbf.
# A troca e so do indice da instrucao invokestatic (3 bytes por 3 bytes); campos, metodos e
# serialVersionUID das classes ficam iguais. O c.a em si nao e mexido.
#
# Uso: python3 tools/carreira/remenda_salvar.py <jar do jogo> <pasta de saida: mod/assets>
import os
import struct
import sys
import zipfile

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "escudos"))
from remenda_ah import le_pool, nome  # noqa: E402

TROCAS = {
    "a/hz.class": (("c/a", "ae", "(Ljava/lang/String;)V"), ("mod/Carreira", "salva", "(Ljava/lang/String;)V")),
    "a/dR.class": (("c/a", "c", "(Ljava/lang/String;Z)Z"), ("mod/Carreira", "carrega", "(Ljava/lang/String;Z)Z")),
}


def remenda(b, alvo_ref, novo_ref):
    n, fim, pool = le_pool(b)
    alvo = None
    for i, (tag, v) in pool.items():
        if tag == 10:
            c, nt = v
            nm, ds = pool[nt][1]
            if (nome(pool, pool[c][1]), nome(pool, nm), nome(pool, ds)) == alvo_ref:
                alvo = i
    assert alvo, "chamada nao encontrada: %s" % (alvo_ref,)
    novo = bytearray()
    k = n

    def utf(s):
        nonlocal k
        e = s.encode("utf-8")
        novo.extend(b"\x01" + struct.pack(">H", len(e)) + e)
        k += 1
        return k - 1
    u_cls = utf(novo_ref[0])
    novo.extend(b"\x07" + struct.pack(">H", u_cls)); i_cls = k; k += 1
    u_nm, u_ds = utf(novo_ref[1]), utf(novo_ref[2])
    novo.extend(b"\x0c" + struct.pack(">HH", u_nm, u_ds)); i_nt = k; k += 1
    novo.extend(b"\x0a" + struct.pack(">HH", i_cls, i_nt)); i_m = k; k += 1
    velho = bytes([0xB8]) + struct.pack(">H", alvo)
    resto = bytes(b[fim:])
    assert resto.count(velho) == 1, "esperava 1 chamada, achei %d" % resto.count(velho)
    resto = resto.replace(velho, bytes([0xB8]) + struct.pack(">H", i_m))
    return bytes(b[:8]) + struct.pack(">H", k) + bytes(b[10:fim]) + bytes(novo) + resto


def main():
    jar, pasta = sys.argv[1], sys.argv[2]
    z = zipfile.ZipFile(jar)
    for entrada, (alvo, novo) in TROCAS.items():
        out = remenda(bytearray(z.read(entrada)), alvo, novo)
        caminho = os.path.join(pasta, entrada)
        os.makedirs(os.path.dirname(caminho), exist_ok=True)
        open(caminho, "wb").write(out)
        print("ok", caminho)


if __name__ == "__main__":
    main()
