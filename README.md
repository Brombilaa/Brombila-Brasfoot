# Brombila Brasfoot (mod do Brasfoot 22-23)

Mod pessoal do Brasfoot 22-23 com o tema "Noite de estreia": interface escura e dourada, menu inicial com foto de estádio, fontes Saira Extra Condensed e Manrope, ícones em traço fino, partida ao vivo em 2D e 3D, ordens ao vivo, pênaltis em 3D e janelas novas para a temporada.

A pasta `jogo/` guarda uma cópia completa do jogo do mod (`C:\BrombilaBrasfoot`), com times, escudos, camisas, troféus, sons e configs. Por isso o repositório deve continuar **privado**.

## Estrutura

- `jogo/` — o jogo pronto para jogar. Copie o conteúdo para `C:\BrombilaBrasfoot` e abra `BrombilaBrasfoot.exe` (precisa de Java 8 ou mais novo).
- `mod/src/mod/` — o código do mod (Java 8). Entrada em `Boot` (instala o `Tema` e chama o `main` original, `best.h2`). Cada arquivo começa com um comentário dizendo qual tela ou parte do jogo ele cuida.
- `mod/src-jogo/` — classes do próprio jogo reescritas pelo mod (por exemplo `components/ao`, a tela Escalar time). Compilam com o .jar do jogo no classpath.
- `mod/classes/` — tudo compilado, versionado para montar o .exe sem JDK. Tem que bater com o código.
- `mod/assets/` — arquivos que entram no .jar: imagens do jogo redesenhadas (mesmo nome do original), arquivos novos do mod em `mod/` (logo, fontes, taças, bonecos 3D, fundos) e as classes remendadas `best/ah` e `a/dR`, `a/hz` (geradas por `tools/escudos` e `tools/carreira`).
- `tools/` — geradores das artes e remendos: ícones (`icones/`), tema dourado e fundos (`tema/`), gramados (`escalar/`), taças (`tacas/`), bonecos 3D (`tv/`), simulação da partida (`partida/`), ícone do .exe (`logo/`) e a montagem do .exe (`monta_exe.py`).

## Como montar o .exe

O executável é um stub Launch4j seguido de um .jar. A montagem pega o .jar do jogo original, troca o `Rsrc-Main-Class` do MANIFEST de `best.h2` para `mod.Boot`, substitui/adiciona as classes e os arquivos de `mod/assets` e junta com o stub do `jogo/BrombilaBrasfoot.exe` (que já tem o ícone novo).

No Windows (PowerShell), com o jogo original em `C:\Brasfoot22-23\bf22-23.exe`:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Isso gera `C:\BrombilaBrasfoot\BrombilaBrasfoot.exe`. Use `-Original` e `-Output` para outros caminhos. Com JDK instalado, o código é compilado na hora; sem JDK, usa `mod/classes`.

Com Python 3:

```sh
python3 tools/monta_exe.py caminho/bf22-23.exe jogo/BrombilaBrasfoot.exe --compila
```

`--compila` recompila `mod/src` e `mod/src-jogo` para `mod/classes` antes de montar.

Os fundos `img/f01.jpg` e `img/f02.jpg` vão dentro do .exe; o jogo lê esses dois do disco, então o mod grava os do tema na pasta `img` ao abrir.
