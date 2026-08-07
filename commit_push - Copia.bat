@echo off
setlocal enabledelayedexpansion
echo ==========================================
echo Iniciando processo de commit e push...
echo ==========================================

REM Verifica se há alterações na working tree
git status --porcelain | findstr . >nul
if errorlevel 1 (
    echo NENHUMA ALTERACAO NA WORKING TREE.
    echo Verificando se ha commits locais para enviar...
    goto :push
)

echo.
echo ALTERACOES ENCONTRADAS:
git status
echo.

set /p arquivos="Quais arquivos deseja incluir ( . )? "
if "%arquivos%"=="" set arquivos=.
git add %arquivos%


echo.
echo TAGS EXISTENTES:
git tag
echo.

REM Pega a última tag e incrementa a versão
for /f "delims=" %%i in ('git tag --sort=-v:refname ^| findstr /r "v[0-9]*\.[0-9]*\.[0-9]*" ^| more +0') do (
    set "ultima_tag=%%i"
    goto :sair_loop
)
:sair_loop

if defined ultima_tag (
    REM Extrai os números da versão
    for /f "tokens=1,2,3 delims=v." %%a in ("%ultima_tag%") do (
        set "major=%%a"
        set "minor=%%b"
        set "patch=%%c"
    )
    
    REM Remove zeros à esquerda e incrementa
    set /a patch=100%patch% %% 100
    set /a patch+=1
    
    REM Formata patch com dois dígitos
    if %patch% LSS 10 (
        set "patch=0%patch%"
    )
    
    set "sugestao_tag=v%major%.%minor%.%patch%"
    echo Ultima tag: %ultima_tag%
    echo Sugestao: %sugestao_tag%
    echo.
    
    set /p tagv="Digite a tag da versao (%sugestao_tag%): "
    if "%tagv%"=="" set tagv=%sugestao_tag%
) else (
    echo Nenhuma tag encontrada. Usando versao inicial v0.0.01
    set /p tagv="Digite a tag da versao (v0.0.01): "
    if "%tagv%"=="" set tagv=v0.0.01
)

:loop_mensagem
set /p mensagem="Digite a mensagem do commit: "
if "%mensagem%"=="" (
    echo ERRO: Mensagem nao pode estar vazia!
    goto loop_mensagem
)

git commit -m "%mensagem%"
git tag %tagv%

:push
echo.
echo ENVIANDO PARA O GITHUB...
git push --all
if errorlevel 1 (
    echo.
    echo ERRO: Falha no push --all!
    pause
)

git push --tags
if errorlevel 1 (
    echo.
    echo ERRO: Falha no push --tags!
    pause
)

echo ==========================================
echo Processo concluido!
pause