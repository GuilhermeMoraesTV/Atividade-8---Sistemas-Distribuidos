@echo off
rem Define a página de código para UTF-8 para garantir que os acentos apareçam corretamente.
chcp 65001 > nul

echo --- Limpando e empacotando o projeto com Maven... ---
echo.

rem Executa o Maven para limpar, compilar e empacotar o projeto.
mvn clean package

rem Verifica se o comando Maven falhou.
if %errorlevel% neq 0 (
    echo.
    echo ----------------------------------------------------
    echo ***** FALHA NA COMPILACAO! *****
    echo ----------------------------------------------------
    echo Verifique as mensagens de erro detalhadas acima.
) else (
    echo.
    echo ------------------------------------------------------
    echo Compilacao e empacotamento finalizados com SUCESSO!
    echo ------------------------------------------------------
)

echo.
echo Pressione qualquer tecla para fechar esta janela...
pause > nul