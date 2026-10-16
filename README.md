# Brasfoot 22-23 — Mod Brombila

Mod pessoal do Brasfoot 22-23 que deixa a interface vermelha e troca o título para "Brombila Brasfoot".

Este repositório guarda só os arquivos do mod. O jogo original (`bf22-23.exe`, imagens, sons, times, saves) **não** é versionado.

## Estrutura

- `mod/red.js` — script Nashorn que roda antes do jogo: pinta a paleta Swing/Nimbus de vermelho, troca o título das janelas e depois chama o `main` original (`best.h2`).
- `mod/Boot.java` / `mod/Boot.class` — ponto de entrada do mod; só executa o `red.js`. O `.class` fica versionado para quem não tem JDK.
- `mod/assets/` — imagens do jogo já recoloridas para vermelho (substituem as originais dentro do .jar).
- `build.ps1` — monta o `BrombilaBrasfoot.exe` a partir do executável original.

## Como montar

1. Tenha o jogo original em `C:\Brasfoot22-23\bf22-23.exe`.
2. No PowerShell, dentro desta pasta:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\build.ps1
   ```

   Isso gera `C:\Brasfoot22-23-Brombila\BrombilaBrasfoot.exe`. Use `-Original` e `-Output` para outros caminhos.

O executável é um stub Launch4j seguido de um .jar. O script separa os dois, troca o `Rsrc-Main-Class` do MANIFEST de `best.h2` para `mod.Boot`, substitui as imagens de `mod/assets`, adiciona `mod/Boot.class` e `mod/red.js` e junta tudo de novo. O jogo precisa de Java 8 (Nashorn).
