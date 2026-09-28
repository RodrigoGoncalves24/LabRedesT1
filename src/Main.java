import java.net.ServerSocket;
import java.util.*;

private static int port = 1025;
private static int servidorPorta = 8080;

void main(String[] args) throws IOException {

    interpretarRequisicao(args);

    try {
        // Bloqueante, ou seja, espera uma conexão
        ServerSocket serverSocket = new ServerSocket(servidorPorta);
        System.out.println("Aguradanodo conexão...: ");


        Socket socket = new Socket("localhost", servidorPorta);

        Thread.sleep(5000);

        Socket cliente = serverSocket.accept();
        System.out.println("Cliente conectado");

    } catch (IOException e) {
        e.printStackTrace();
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }


}


// Deverá interpretar a requisição, argumento por argumento, para conexão
private void interpretarRequisicao(String[] args) {

    servidorPorta = Integer.parseInt(args[1]);

//    String[] requisicao = args[0].split(" ");
//
//    for (int i = 0; i < requisicao.length; i++) {
//
//        System.out.printf("%s ", requisicao[i]);
//    }

}