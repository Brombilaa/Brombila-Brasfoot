# Brasfoot 22-23 — Mod Brombila

Mod pessoal do Brasfoot 22-23 que deixa a interface vermelha e troca o título para "Brombila Brasfoot".

Além dos arquivos do mod, a pasta `jogo/` guarda uma cópia completa do jogo do mod (`C:\BrombilaBrasfoot`, sem o backup zip): `BrombilaBrasfoot.exe`, times, escudos, camisas, troféus, sons e configs. Por isso o repositório deve continuar **privado**.

## Estrutura

- `jogo/` — o jogo pronto para jogar. Copie o conteúdo para `C:\BrombilaBrasfoot` e abra `BrombilaBrasfoot.exe` (precisa de Java 8).
- `mod/red.js` — script Nashorn que roda antes do jogo: pinta a paleta Swing/Nimbus de vermelho, troca o título das janelas e depois chama o `main` original (`best.h2`).
- `mod/Boot.java` / `mod/Boot.class` — ponto de entrada do mod; só executa o `red.js`. O `.class` fica versionado para quem não tem JDK.
- `mod/assets/` — imagens do jogo já recoloridas para vermelho (substituem as originais dentro do .jar).
- `mod/files/` — arquivos soltos do mod copiados para a pasta do jogo (os fundos `img/f01.jpg` e `img/f02.jpg` recoloridos).
- `build.ps1` — monta o `BrombilaBrasfoot.exe` a partir do executável original.

## Como montar

1. Tenha o jogo original em `C:\Brasfoot22-23\bf22-23.exe`.
2. No PowerShell, dentro desta pasta:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\build.ps1
   ```

   Isso gera `C:\BrombilaBrasfoot\BrombilaBrasfoot.exe` e copia os arquivos de `mod/files` para `C:\BrombilaBrasfoot`. Use `-Original` e `-Output` para outros caminhos.

O executável é um stub Launch4j seguido de um .jar. O script separa os dois, troca o `Rsrc-Main-Class` do MANIFEST de `best.h2` para `mod.Boot`, substitui as imagens de `mod/assets`, adiciona `mod/Boot.class` e `mod/red.js` e junta tudo de novo. O jogo precisa de Java 8 (Nashorn).
