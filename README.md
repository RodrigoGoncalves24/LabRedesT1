# Servidor HTTP/1.1 sobre sockets TCP

Servidor didático em Java que implementa parsing e respostas HTTP/1.1 diretamente sobre sockets TCP.

## Organização do projeto

```text
.
├── README.md                 # compilação e execução
├── docs/                     # especificação, roteiro e explicações
├── tests/                    # testes locais automatizados
├── src/Servidor/             # código Java do servidor
└── src/www/                  # raiz de arquivos servidos
```

## Requisitos

- JDK instalado (Java 8 ou superior; validado com Java 21).
- Porta TCP alta disponível, por exemplo `8080`.
- Acesso à rede e regra de firewall permitindo conexões de entrada nessa porta para os testes entre máquinas.

## Compilar

Execute os comandos a partir da raiz do repositório:

```powershell
javac -d out src/Servidor/*.java
```

## Iniciar

```powershell
java -cp out Servidor.Server --port 8080 --root .\src\www
```

O servidor escuta em `0.0.0.0`, usa um pool de 16 trabalhadores para atender conexões concorrentes e fecha uma conexão persistente após 5 segundos sem dados. Para interromper, use `Ctrl+C`.

## Acessar

No próprio computador, para teste de fumaça:

```powershell
curl.exe -i http://localhost:8080/teste.txt
```

De outra máquina da rede, substitua `<IP-DO-SERVIDOR>` pelo endereço IPv4 mostrado por `ipconfig` no computador que executa o servidor:

```powershell
curl.exe -i http://<IP-DO-SERVIDOR>:8080/
```

A página inicial `index.html` carrega uma imagem, CSS, JavaScript e JSON de `src/www/`, para permitir o teste de interoperabilidade no navegador.

## Testes locais automatizados

Com o servidor iniciado na porta `8080`, execute na raiz do projeto:

```powershell
. .\tests\TestesLocal.ps1
```

Os 39 checks locais validam também argumentos/raiz inválidos, query string, nomes com pontos, HEAD sem corpo por TCP, tamanho UTF-8 em bytes, EOF e dez requisições na mesma conexão. Os testes de acesso remoto e as capturas C1/C2 continuam obrigatórios e devem ser feitos entre máquinas distintas.

## Argumentos

- `--port`: porta TCP entre 1025 e 65535.
- `--root`: diretório raiz de arquivos. O caminho é resolvido e validado na inicialização.

## Documentação

- [Como o código funciona](docs/ComoFunciona.md): componentes, fluxo de uma conexão e diagramas.
- [Casos de teste](docs/CasosDeTeste.md): conformidade, segurança, persistência e medições.
- [Análise de implementação](docs/AnaliseImplementacaoPendente.md): situação e limitações.
- [Especificação do trabalho](docs/Espec.md).
