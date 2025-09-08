@echo off
chcp 65001 > nul

set JAR_FILE=target/servico-mensagens-a8-1.0-SNAPSHOT-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo ***** ERRO: O ficheiro %JAR_FILE% nao foi encontrado! *****
    echo Por favor, execute o COMPILAR.bat primeiro.
    pause
    exit
)

set /p port="Digite a porta do no para conectar (ex: 8001, 8002, 8003): "

echo.
echo --- Iniciando Cliente na porta %port% ---
rem Executa a classe Cliente, passando a porta como argumento
java -cp %JAR_FILE% mensageria.cliente.Cliente %port%

pause