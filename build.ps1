<#
  Monta o BrombilaBrasfoot.exe a partir do executavel original do Brasfoot 22-23.

  O .exe do jogo e um stub Launch4j seguido de um .jar. Este script:
    1. separa o stub do .jar;
    2. recria o .jar trocando o Rsrc-Main-Class do MANIFEST para mod.Boot,
       substituindo as imagens de mod\assets e adicionando as classes do mod (mod\classes);
    3. junta stub + novo .jar no executavel de saida. O stub (com o icone novo) vem do
       jogo\BrombilaBrasfoot.exe do repositorio; sem ele, fica o do original.

  Os fundos img\f01.jpg e f02.jpg vao dentro do .exe e o proprio jogo grava na pasta img ao abrir.
  O tools\monta_exe.py faz a mesma coisa em Python.

  Uso:
    powershell -ExecutionPolicy Bypass -File .\build.ps1
    powershell -ExecutionPolicy Bypass -File .\build.ps1 -Original C:\Brasfoot22-23\bf22-23.exe -Output C:\BrombilaBrasfoot\BrombilaBrasfoot.exe
#>
param(
    [string]$Original = 'C:\Brasfoot22-23\bf22-23.exe',
    [string]$Output   = 'C:\BrombilaBrasfoot\BrombilaBrasfoot.exe'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root   = $PSScriptRoot
$modDir = Join-Path $root 'mod'
$assets = Join-Path $modDir 'assets'

# Separa stub e jar usando o diretorio central do zip (fim do arquivo).
$exe = [IO.File]::ReadAllBytes($Original)
$eocd = -1
for ($i = $exe.Length - 22; $i -ge [Math]::Max(0, $exe.Length - 65557); $i--) {
    if ($exe[$i] -eq 0x50 -and $exe[$i+1] -eq 0x4B -and $exe[$i+2] -eq 0x05 -and $exe[$i+3] -eq 0x06) { $eocd = $i; break }
}
if ($eocd -lt 0) { throw "Nao achei o .jar dentro de $Original" }
$cdSize   = [BitConverter]::ToUInt32($exe, $eocd + 12)
$cdOffset = [BitConverter]::ToUInt32($exe, $eocd + 16)
$stubLen  = [int]($eocd - $cdSize - $cdOffset)
Write-Host "Stub Launch4j: $stubLen bytes"

$inJar  = New-Object IO.MemoryStream(, $exe)
$inJar.Position = $stubLen
$jarBytes = New-Object byte[] ($exe.Length - $stubLen)
[void]$inJar.Read($jarBytes, 0, $jarBytes.Length)

# Classes do mod: com JDK, compila mod\src + mod\src-jogo (as classes do jogo reescritas pelo mod,
# como components\ao) com o .jar do jogo no classpath; sem JDK, usa as versionadas em mod\classes.
$classes = Join-Path $modDir 'classes'
if (Get-Command javac -ErrorAction SilentlyContinue) {
    $tmp = Join-Path ([IO.Path]::GetTempPath()) ('brombila-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $tmp | Out-Null
    $cp = Join-Path $tmp 'jogo.jar'
    [IO.File]::WriteAllBytes($cp, $jarBytes)
    $tmpCls = Join-Path $tmp 'classes'
    $fontes = Get-ChildItem -Path (Join-Path $modDir 'src'), (Join-Path $modDir 'src-jogo') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    & javac --release 8 -nowarn -encoding UTF-8 -cp $cp -d $tmpCls $fontes
    if ($LASTEXITCODE -ne 0) {
        # JDK 8 nao conhece --release
        & javac -source 8 -target 8 -nowarn -encoding UTF-8 -cp $cp -d $tmpCls $fontes
    }
    if ($LASTEXITCODE -ne 0) { throw 'javac falhou ao compilar mod\src' }
    $classes = $tmpCls
}

# Arquivos que entram no .jar: caminho dentro do jar -> bytes
$replace = @{}
Get-ChildItem -Path $classes -Recurse -File -Filter *.class | ForEach-Object {
    $rel = $_.FullName.Substring($classes.Length + 1).Replace('\', '/')
    $replace[$rel] = [IO.File]::ReadAllBytes($_.FullName)
}
Get-ChildItem -Path $assets -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($assets.Length + 1).Replace('\', '/')
    $replace[$rel] = [IO.File]::ReadAllBytes($_.FullName)
}

# Stub com o icone novo, se o .exe do repositorio existir.
$versionado = Join-Path $root 'jogo\BrombilaBrasfoot.exe'
$stub = New-Object byte[] $stubLen
[Array]::Copy($exe, 0, $stub, 0, $stubLen)
if (Test-Path $versionado) {
    $fsv = [IO.File]::OpenRead($versionado)
    [void]$fsv.Read($stub, 0, $stubLen)
    $fsv.Close()
} else {
    Write-Warning 'Sem jogo\BrombilaBrasfoot.exe: o icone fica o original.'
}

$src = New-Object IO.Compression.ZipArchive((New-Object IO.MemoryStream(, $jarBytes)), [IO.Compression.ZipArchiveMode]::Read)

$outJar = New-Object IO.MemoryStream
$dst = New-Object IO.Compression.ZipArchive($outJar, [IO.Compression.ZipArchiveMode]::Create, $true)

function Write-Entry($zip, [string]$name, [byte[]]$data) {
    $e = $zip.CreateEntry($name, [IO.Compression.CompressionLevel]::Optimal)
    $s = $e.Open(); $s.Write($data, 0, $data.Length); $s.Close()
}

$done = @{}
foreach ($entry in $src.Entries) {
    $name = $entry.FullName
    if ($name -eq 'META-INF/MANIFEST.MF') {
        $r = New-Object IO.StreamReader($entry.Open())
        $mf = $r.ReadToEnd(); $r.Close()
        if ($mf -notmatch 'Rsrc-Main-Class: best\.h2') { Write-Warning 'MANIFEST nao tem Rsrc-Main-Class: best.h2; mod.Boot chama best.h2 mesmo assim.' }
        $mf = $mf -replace 'Rsrc-Main-Class: \S+', 'Rsrc-Main-Class: mod.Boot'
        Write-Entry $dst $name ([Text.Encoding]::ASCII.GetBytes($mf))
    } elseif ($replace.ContainsKey($name)) {
        Write-Entry $dst $name $replace[$name]
    } else {
        $ms = New-Object IO.MemoryStream
        $s = $entry.Open(); $s.CopyTo($ms); $s.Close()
        Write-Entry $dst $name $ms.ToArray()
    }
    $done[$name] = $true
}
foreach ($name in ($replace.Keys | Sort-Object)) {
    if (-not $done.ContainsKey($name)) { Write-Entry $dst $name $replace[$name] }
}
$dst.Dispose(); $src.Dispose()

$outDir = Split-Path -Parent $Output
if ($outDir -and -not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
$fs = [IO.File]::Create($Output)
$fs.Write($stub, 0, $stubLen)
$jar = $outJar.ToArray()
$fs.Write($jar, 0, $jar.Length)
$fs.Close()

Write-Host ("Pronto: {0} ({1:N0} bytes, {2} arquivos do mod)" -f $Output, ($stubLen + $jar.Length), $replace.Count)
