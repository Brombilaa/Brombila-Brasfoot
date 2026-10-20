# Brasfoot 22-23 — Mod Brombila

Mod pessoal do Brasfoot 22-23 com um tema verde próprio (cores, botões arredondados com animação, painéis com textura, rolagem suave) e o título "Brombila Brasfoot".

Além dos arquivos do mod, a pasta `jogo/` guarda uma cópia completa do jogo do mod (`C:\BrombilaBrasfoot`, sem o backup zip): `BrombilaBrasfoot.exe`, times, escudos, camisas, troféus, sons e configs. Por isso o repositório deve continuar **privado**.

## Estrutura

- `jogo/` — o jogo pronto para jogar. Copie o conteúdo para `C:\BrombilaBrasfoot` e abra `BrombilaBrasfoot.exe` (precisa de Java 8).
- `mod/src/mod/` — código do tema verde (Java, compila para Java 8; não depende mais do Nashorn):
  - `Boot` — ponto de entrada: liga o antialiasing do texto, instala o tema e chama o `main` original (`best.h2`).
  - `Tema` — paleta; troca qualquer azul por verde e as cores dos 5 temas internos do jogo pelo verde do mod; título das janelas; tabelas zebradas com destaque animado da linha sob o mouse.
  - `BotaoUI` — botões verdes arredondados, com degradê, sombra e hover/clique animados.
  - `PainelUI` — painéis escuros com degradê suave e textura leve.
  - `RolagemSuave` — rolagem animada da rodinha do mouse.
  - `Animacao` — fade-in das janelas sem borda (popups do jogo) e curvas de animação.
- `mod/classes/` — as classes já compiladas, versionadas para quem não tem JDK.
- `mod/assets/` — ícones que eram azuis, recoloridos para verde (substituem os originais dentro do .jar).
- `mod/files/` — arquivos soltos do mod copiados para a pasta do jogo (fundos do estádio `img/f01.jpg` e `img/f02.jpg` em verde).
- `build.ps1` — monta o `BrombilaBrasfoot.exe` a partir do executável original.

## Como montar

1. Tenha o jogo original em `C:\Brasfoot22-23\bf22-23.exe`.
2. No PowerShell, dentro desta pasta:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\build.ps1
   ```

   Isso gera `C:\BrombilaBrasfoot\BrombilaBrasfoot.exe` e copia os arquivos de `mod/files` para `C:\BrombilaBrasfoot`. Use `-Original` e `-Output` para outros caminhos.

O executável é um stub Launch4j seguido de um .jar. O script separa os dois, troca o `Rsrc-Main-Class` do MANIFEST de `best.h2` para `mod.Boot`, substitui as imagens de `mod/assets`, adiciona as classes do tema (compila `mod/src` se tiver `javac`, senão usa `mod/classes`) e junta tudo de novo.
