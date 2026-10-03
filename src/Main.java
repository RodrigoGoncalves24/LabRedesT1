import java.net.ServerSocket;
import java.util.*;

private static int port = 1025;
private static int servidorPorta; // Server fica escutando a port
private static String servidorRoot; // Considera como raiz
private static RequestHTTP requisicao;

void main(String[] args){

    interpretarRequisicao(args);

    try {
        // Bloqueante, ou seja, espera uma conexão
        ServerSocket serverSocket = new ServerSocket(servidorPorta);
        System.out.println("Waiting connection...: ");

        Socket cliente = serverSocket.accept();

        // Recebo requisição do cliente
        cliente.getInputStream();

        parseRequisicao(cliente.getInputStream());

        System.out.println("\nConnection close!");

    } catch (IOException e) {
        e.printStackTrace();
    }


}

// Recebe requisção e trata ela
private void parseRequisicao(InputStream clienteInputStream) throws IOException {

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clienteInputStream));

    // Transforma requisicao em array de strings
    String[] req = bufferedReader.readAllLines().toArray(new String[0]);

    for (int i = 0; i < req.length; i++) {

        System.out.println("\n"+req[i]);

    }

    //requisicao = new RequestHTTP();

}


// Deverá interpretar a requisição, argumento por argumento, para conexão
private void interpretarRequisicao(String[] args) {

    for (int i = 0; i < args.length; i++) {
        String req =  args[i];


        // Le a requisição recebida e divide no parâmetro certo - feito para receber requisição fora de ordem
        switch (req) {
            case "--port":{
                servidorPorta = Integer.parseInt(args[i+=1]);
                break;
            }
            case "--root":{
                servidorRoot = args[i+=1];
                break;
            }
            default:{
                    throw new IllegalArgumentException("Parametro invalido");
            }
        }

    }

}