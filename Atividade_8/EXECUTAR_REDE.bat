@echo off
chcp 65001 > nul

echo --- Iniciando a Rede de Nos do Sistema de Mensagens ---
echo.
set JAR_FILE=target/servico-mensagens-a8-1.0-SNAPSHOT-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo ***** ERRO: O ficheiro %JAR_FILE% nao foi encontrado! *****
    echo Por favor, execute o COMPILAR.bat primeiro.
    pause
    exit
)

rem Executa o .jar que inicia o Simulador (a rede de nós)
java -cp %JAR_FILE% mensageria.Simulador

pause