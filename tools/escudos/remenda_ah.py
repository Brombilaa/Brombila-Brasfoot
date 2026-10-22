# Remenda best/ah.class (o time) para os escudos sairem nitidos.
#
# O jogo reduz o escudo de 60x60 para 18x18 (tabelas, listas) e para outros tamanhos com
# Graphics2D.drawImage sem interpolacao, que pega o pixel mais proximo e serrilha. Aqui as tres
# chamadas de drawImage(Image, x, y, w, h, observer) dentro de ah viram mod.Escala.desenha(g, ...),
# que reduz em passos com interpolacao bicubica. A troca e de uma instrucao de 3 bytes por outra
# de 3 bytes (invokevirtual -> invokestatic) com a mesma pilha, entao nada mais muda na classe
# (campos, metodos e serialVersionUID ficam iguais e os jogos salvos continuam abrindo).
#
# Uso: python3 tools/escudos/remenda_ah.py <jar do jogo> <saida: mod/assets/best/ah.class>
import struct
import sys
import zipfile

ALVO = ("java/awt/Graphics2D", "drawImage", "(Ljava/awt/Image;IIIILjava/awt/image/ImageObserver;)Z")
NOVO = ("mod/Escala", "desenha", "(Ljava/awt/Graphics2D;Ljava/awt/Image;IIIILjava/awt/image/ImageObserver;)Z")


def le_pool(b):
    n = struct.unpack(">H", b[8:10])[0]
    i, pos, pool = 1, 10, {}
    while i < n:
        tag = b[pos]
        if tag == 1:
            ln = struct.unpack(">H", b[pos + 1:pos + 3])[0]
            pool[i] = (1, b[pos + 3:pos + 3 + ln].decode("utf-8", "replace"))
            pos += 3 + ln
        elif tag in (7, 8, 16, 19, 20):
            pool[i] = (tag, struct.unpack(">H", b[pos + 1:pos + 3])[0])
            pos += 3
        elif tag in (9, 10, 11, 12, 18, 3, 4):
            pool[i] = (tag, struct.unpack(">HH", b[pos + 1:pos + 5]))
            pos += 5
        elif tag == 15:
            pool[i] = (tag, None)
            pos += 4
        elif tag in (5, 6):
            pool[i] = (tag, None)
            pos += 9
            i += 1
        else:
            raise ValueError("tag %d" % tag)
        i += 1
    return n, pos, pool


def nome(pool, i):
    return pool[i][1]


def main():
    jar, saida = sys.argv[1], sys.argv[2]
    b = bytearray(zipfile.ZipFile(jar).read("best/ah.class"))
    n, fim, pool = le_pool(b)
    alvo = None
    for i, (tag, v) in pool.items():
        if tag == 10:
            c, nt = v
            cls = nome(pool, pool[c][1])
            nm, ds = pool[nt][1]
            if (cls, nome(pool, nm), nome(pool, ds)) == ALVO:
                alvo = i
    assert alvo, "drawImage nao encontrado"
    # entradas novas no fim do pool
    novo = bytearray()
    k = n
    def utf(s):
        nonlocal k
        e = s.encode("utf-8")
        novo.extend(b"\x01" + struct.pack(">H", len(e)) + e)
        k += 1
        return k - 1
    u_cls = utf(NOVO[0])
    novo.extend(b"\x07" + struct.pack(">H", u_cls)); i_cls = k; k += 1
    u_nm, u_ds = utf(NOVO[1]), utf(NOVO[2])
    novo.extend(b"\x0c" + struct.pack(">HH", u_nm, u_ds)); i_nt = k; k += 1
    novo.extend(b"\x0a" + struct.pack(">HH", i_cls, i_nt)); i_m = k; k += 1
    velho = bytes([0xB6]) + struct.pack(">H", alvo)
    troca = bytes([0xB8]) + struct.pack(">H", i_m)
    resto = bytes(b[fim:])
    achados = resto.count(velho)
    assert achados == 3, "esperava 3 chamadas, achei %d" % achados
    resto = resto.replace(velho, troca)
    out = bytes(b[:8]) + struct.pack(">H", k) + bytes(b[10:fim]) + bytes(novo) + resto
    import os
    os.makedirs(os.path.dirname(saida), exist_ok=True)
    open(saida, "wb").write(out)
    print("ok", saida, "pool", n, "->", k)


if __name__ == "__main__":
    main()
