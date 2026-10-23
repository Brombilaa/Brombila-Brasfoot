# Monta o BrombilaBrasfoot.exe (mesma receita do build.ps1, para Linux/Mac/Windows com Python).
#
# O .exe do jogo e um stub Launch4j seguido de um .jar. Aqui:
#   1. o .jar vem do executavel ORIGINAL (bf22-23.exe), com o MANIFEST apontando para mod.Boot;
#   2. entram as classes do mod (mod/classes, ou compiladas na hora com --compila) e as
#      imagens/arquivos de mod/assets, substituindo os do jogo quando o nome e o mesmo;
#   3. o stub (com o icone novo) vem do jogo/BrombilaBrasfoot.exe ja versionado. Sem ele, usa o
#      stub do original e e preciso trocar o icone depois com tools/logo/troca_icone.py.
#
# Uso:
#   python3 tools/monta_exe.py <bf22-23.exe original> [saida.exe] [--compila]
#   (saida padrao: jogo/BrombilaBrasfoot.exe)
#
# --compila roda javac --release 8 em mod/src + mod/src-jogo, com o .jar do jogo no classpath,
# e grava o resultado em mod/classes (as classes versionadas precisam bater com o codigo).
import io
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

RAIZ = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
STUB = 125440


def separa(exe):
    dados = open(exe, 'rb').read()
    fim = dados.rfind(b'PK\x05\x06')
    if fim < 0:
        raise SystemExit('nao achei o .jar dentro de %s' % exe)
    tam_cd = int.from_bytes(dados[fim + 12:fim + 16], 'little')
    ini_cd = int.from_bytes(dados[fim + 16:fim + 20], 'little')
    stub = fim - tam_cd - ini_cd
    return dados[:stub], dados[stub:]


def arquivos(pasta):
    for raiz, _, nomes in os.walk(pasta):
        for n in nomes:
            if n.endswith('.pyc'):
                continue
            p = os.path.join(raiz, n)
            yield os.path.relpath(p, pasta).replace(os.sep, '/'), p


def compila(jar):
    classes = os.path.join(RAIZ, 'mod', 'classes')
    fontes = [p for d in ('src', 'src-jogo') for _, p in arquivos(os.path.join(RAIZ, 'mod', d)) if p.endswith('.java')]
    tmp = tempfile.mkdtemp(prefix='brombila-')
    try:
        cp = os.path.join(tmp, 'jogo.jar')
        open(cp, 'wb').write(jar)
        saida = os.path.join(tmp, 'classes')
        subprocess.check_call(['javac', '--release', '8', '-nowarn', '-encoding', 'UTF-8', '-cp', cp, '-d', saida] + fontes)
        shutil.rmtree(classes, ignore_errors=True)
        shutil.copytree(saida, classes)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print('compilado: %d fontes -> mod/classes' % len(fontes))


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if not args:
        raise SystemExit(__doc__ or 'uso: monta_exe.py <bf22-23.exe> [saida.exe] [--compila]')
    original = args[0]
    saida = args[1] if len(args) > 1 else os.path.join(RAIZ, 'jogo', 'BrombilaBrasfoot.exe')

    stub, jar = separa(original)
    if len(stub) != STUB:
        print('aviso: stub com %d bytes (esperado %d)' % (len(stub), STUB))
    if '--compila' in sys.argv:
        compila(jar)
    versionado = os.path.join(RAIZ, 'jogo', 'BrombilaBrasfoot.exe')
    if os.path.isfile(versionado):
        stub = open(versionado, 'rb').read(STUB)  # stub com o icone novo
    else:
        print('aviso: sem jogo/BrombilaBrasfoot.exe; o icone fica o original')

    troca = {}
    for nome, p in arquivos(os.path.join(RAIZ, 'mod', 'classes')):
        troca[nome] = p
    for nome, p in arquivos(os.path.join(RAIZ, 'mod', 'assets')):
        troca[nome] = p

    out = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(jar)) as src, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
        feitos = set()
        for info in src.infolist():
            nome = info.filename
            if nome == 'META-INF/MANIFEST.MF':
                mf = src.read(nome).decode('ascii')
                if 'Rsrc-Main-Class: best.h2' not in mf:
                    print('aviso: MANIFEST sem Rsrc-Main-Class: best.h2')
                dst.writestr(nome, re.sub(r'Rsrc-Main-Class: \S+', 'Rsrc-Main-Class: mod.Boot', mf))
            elif nome in troca:
                dst.write(troca[nome], nome)
            else:
                dst.writestr(info, src.read(nome))
            feitos.add(nome)
        for nome in sorted(troca):
            if nome not in feitos:
                dst.write(troca[nome], nome)

    os.makedirs(os.path.dirname(os.path.abspath(saida)), exist_ok=True)
    with open(saida, 'wb') as f:
        f.write(stub)
        f.write(out.getvalue())
    print('pronto: %s (%d bytes, %d arquivos do mod)' % (saida, len(stub) + len(out.getvalue()), len(troca)))


if __name__ == '__main__':
    main()
