package Servidor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/// AGORA, IDENTICAR A REQUSIÇÃO DO USUÁRIO, LOCALIZAR O CONTEÚDO E RETORNAR
/// COM ISSO, UTILIZAR OS CÓDIGOS DE RETONRO
/// CLASSE RESPONSE JÁ GARANTE O HEADER, APENAS PRECISA DEFINIR AS RESPOSTAS A SEREM RETORNARAS COMO TEXTO, IMG, ENTRE OUTROS
/// SERVIDOR RETORNA MENSAGEM BÁSICA PELO TERMINAL.
///
/// ATÉ O MOMENTO, APÓS A REQUISIÇÃO, SERVIDOR FECHA, PORÉM ELE NÃO DEVERÁ SER FECHADO (ISSO DEVERÁ SER FEITO POR ÚLTIMO, NÃO MUITO IMPORTÂNTE AGORA)


public class Server {
    private static int port = 1025;
    private static int servidorPorta = 0;
    private static String servidorRoot = "";
    private static RequestHTTP requisicaoCompleta;
    private static String finalPath = " ";
    private static String codeReq = " ";

    private static final ArrayList<String> METHODS = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"));

    private static Path root = Paths.get("./www");
    private static String request;

    private static String[] methodVersion;
    private static String[] userAgent;
    private static String[] hostLocalHost;

    void main(String[] args) {

        interpretarRequisicao(args);

        try {
            // Bloqueante, ou seja, espera uma conexão
            ServerSocket serverSocket = new ServerSocket(servidorPorta);
            System.out.println("Waiting connection...: ");
            Socket cliente = serverSocket.accept();

            // Recebo requisição do cliente
            cliente.getInputStream();

            parseRequisicao(cliente.getInputStream());

            // Combina o caimnho para achar a árvore de diretório
            root = Path.of(servidorRoot);

            // Escrevendo o retorno
            OutputStream out = cliente.getOutputStream();

            retornoRequisicao(out, finalPath);

            //System.out.println(requisicaoCompleta.toString());

            System.out.println("\nConnection close!");

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void retornoRequisicao(OutputStream out, String request) throws IOException {


        Response response = new Response(codeReq, ContentType.txt, request, request.length());

        String resposta = response.toString();
        out.write(resposta.getBytes());
        out.flush();
    }


    // Recebe requisção e trata ela
    private void parseRequisicao(InputStream clienteInputStream) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clienteInputStream));
        ArrayList<String> linhas = new ArrayList<>();
        String linha;

        //  Garante que não ocorra deadlock, lê até encontrar uma linha vazia
        while ((linha = bufferedReader.readLine()) != null && !linha.isEmpty()) {
            linhas.add(linha);
        }

        // Transforma requisicao em array de strings
        String[] req = linhas.toArray(new String[0]);

        int pos = 0;
        String valor = req[pos];

        while (!Objects.equals(valor, "") && pos < req.length) {
            valor = req[pos];

            if (valor.contains("HTTP")) {
                resolveReqHttp(req[pos]);
            } else if (valor.contains("Host:")) {
                reolveHost(req[pos]);
            } else if (valor.contains("User-Agent:")) {
                resolveUserAgent(req[pos]);

            }
            pos++;

        }
        requisicaoCompleta = new RequestHTTP(methodVersion, userAgent, hostLocalHost);

    }

    private void resolveUserAgent(String userAgentReq) {
        String[] params = userAgentReq.split(" ");
        userAgent = new String[]{params[0], params[1]};
    }

    private void reolveHost(String resolveHost) {
        String[] params = resolveHost.split(" ");
        hostLocalHost = new String[]{params[0], params[1]};

    }

    // Identificar o tipo da requisição e retornar
    /*
     * [GET /teste.txt HTTP/1.1, Host: localhost:8080, User-Agent: curl/8.18.0, Accept:
     *  0->GET
     *  1->/teste.txt -- IDENTIFICAR ESSE CAMPO E RETORNAR ELE, CASO NÃO EXISTA, RETORNAR OS CÓDIGOS DE ERRO
     *  2->HTTP/1.1
     *
     */

    private void resolveReqHttp(String resolveReqHttp) throws IOException {
        String[] params = resolveReqHttp.split(" ");

        // Completa enviada pelo usuário
        request = params[0];

        if (request.contains("GET")) {
            request = params[1].substring(1);
            System.out.println(request);
        }

        root = root.resolve(request);

        if (verificaExistenciaArquivo(root)) {
            System.out.println("FILE EXITS!");
            finalPath = Files.readString(root);
        } else {
            System.out.println("FILE DO NOT EXIST");
        }


        methodVersion = new String[]{params[0], params[1], params[2]};
    }

    private boolean verificaExistenciaArquivo(Path root) {

        if (Files.exists(root)) {
            codeReq = "200";
            return true;
        }
        codeReq = "403";
        return false;
    }


    // Deverá interpretar a requisição, argumento por argumento, para conexão
    private void interpretarRequisicao(String[] args) {

        for (int i = 0; i < args.length; i++) {
            String req = args[i];


            // Le a requisição recebida e divide no parâmetro certo - feito para receber requisição fora de ordem
            switch (req) {
                case "--port": {
                    servidorPorta = Integer.parseInt(args[i += 1]);
                    break;
                }
                case "--root": {
                    servidorRoot = args[i += 1];
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Parametro invalido");
                }
            }

        }

    }
}
