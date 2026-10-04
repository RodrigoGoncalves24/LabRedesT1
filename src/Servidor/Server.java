package Servidor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Inicia o listener TCP e converte cada conexão em uma sequência de requisições HTTP. */
public final class Server {
    private static final int IDLE_TIMEOUT_MILLIS_a = 5000;
    private static final int WORKER_COUNT_a = 16;
    private static final String ERROR_TYPE_a = "text/plain; charset=utf-8";

    private Server() {
    }

    /** Ponto de entrada da aplicação: valida os argumentos e inicia o servidor. */
    public static void main(String[] args_a) {
        try {
            ServerConfig config_a = iParseArguments(args_a);
            iStart(config_a);
        } catch (IllegalArgumentException exception_a) {
            System.err.println(exception_a.getMessage());
            System.err.println("Uso: java Servidor.Server --port 8080 --root ./src/www");
            System.exit(2);
        } catch (IOException exception_a) {
            System.err.println("Falha ao iniciar o servidor: " + exception_a.getMessage());
            System.exit(1);
        }
    }

    /** Exige porta e raiz válidas; guarda a raiz como caminho canônico. */
    private static ServerConfig iParseArguments(String[] args_a) throws IOException {
        Integer port_a = null;
        Path root_a = null;

        for (int index_a = 0; index_a < args_a.length; index_a++) {
            String argument_a = args_a[index_a];
            if (index_a + 1 >= args_a.length) {
                throw new IllegalArgumentException("Falta valor para " + argument_a);
            }

            String value_a = args_a[++index_a];
            if ("--port".equals(argument_a)) {
                try {
                    port_a = Integer.parseInt(value_a);
                } catch (NumberFormatException exception_a) {
                    throw new IllegalArgumentException("A porta deve ser um número inteiro.");
                }
                if (port_a < 1025 || port_a > 65535) {
                    throw new IllegalArgumentException("A porta deve estar entre 1025 e 65535.");
                }
            } else if ("--root".equals(argument_a)) {
                root_a = Paths.get(value_a).toRealPath();
                if (!Files.isDirectory(root_a)) {
                    throw new IllegalArgumentException("A raiz informada não é um diretório.");
                }
            } else {
                throw new IllegalArgumentException("Argumento desconhecido: " + argument_a);
            }
        }

        if (port_a == null || root_a == null) {
            throw new IllegalArgumentException("Informe --port e --root.");
        }
        return new ServerConfig(port_a, root_a);
    }

    /** Aceita conexões indefinidamente e delega cada socket ao pool de trabalhadores. */
    private static void iStart(ServerConfig config_a) throws IOException {
        ExecutorService workers_a = Executors.newFixedThreadPool(WORKER_COUNT_a);
        try (ServerSocket listener_a = new ServerSocket()) {
            listener_a.setReuseAddress(true);
            listener_a.bind(new InetSocketAddress("0.0.0.0", config_a.port_a), 50);
            System.out.println("Servidor ouvindo em 0.0.0.0:" + config_a.port_a
                    + " com raiz " + config_a.root_a);

            while (true) {
                Socket client_a = listener_a.accept();
                try {
                    workers_a.execute(() -> iHandleConnection(client_a, config_a.root_a));
                } catch (RejectedExecutionException exception_a) {
                    try {
                        client_a.close();
                    } catch (IOException ignored_a) {
                        // A conexão já não pode ser atendida.
                    }
                }
            }
        } finally {
            workers_a.shutdownNow();
        }
    }

    /** Processa múltiplas requisições da mesma conexão até close, EOF ou timeout. */
    private static void iHandleConnection(Socket client_a, Path root_a) {
        try (Socket socket_a = client_a;
             BufferedInputStream input_a = new BufferedInputStream(socket_a.getInputStream());
             OutputStream output_a = socket_a.getOutputStream()) {
            socket_a.setSoTimeout(IDLE_TIMEOUT_MILLIS_a);

            while (true) {
                RequestHTTP request_a;
                try {
                    request_a = RequestHTTP.iRead(input_a);
                } catch (IllegalArgumentException exception_a) {
                    iSendError(output_a, 400, false, true, null);
                    return;
                }
                if (request_a == null) {
                    return;
                }

                boolean head_a = "HEAD".equals(request_a.method_a);
                boolean close_a = iHasConnectionClose(request_a)
                        || iHasRequestBody(request_a)
                        || !("GET".equals(request_a.method_a) || head_a);

                if (iHasTransferEncoding(request_a)) {
                    iSendError(output_a, 400, head_a, true, null);
                    return;
                }
                if (iHasInvalidContentLength(request_a)) {
                    iSendError(output_a, 400, head_a, true, null);
                    return;
                }

                try {
                    iHandleRequest(request_a, root_a, output_a, close_a);
                } catch (IllegalArgumentException exception_a) {
                    iSendError(output_a, 400, head_a, close_a, null);
                } catch (IOException exception_a) {
                    iSendError(output_a, 404, head_a, true, null);
                    return;
                }

                if (close_a) {
                    return;
                }
            }
        } catch (IOException ignored_a) {
            // EOF, timeout ou cliente desconectado: liberar a conexão sem afetar outras.
        }
    }

    /** Escolhe status/método, resolve o recurso e escreve a resposta apropriada. */
    private static void iHandleRequest(RequestHTTP request_a, Path root_a,
                                       OutputStream output_a, boolean close_a) throws IOException {
        boolean head_a = "HEAD".equals(request_a.method_a);
        if (!("GET".equals(request_a.method_a) || head_a)) {
            iSendError(output_a, 405, false, true, "Allow: GET, HEAD");
            return;
        }

        Path file_a;
        try {
            file_a = iResolveFile(request_a.target_a, root_a);
        } catch (SecurityException exception_a) {
            iSendError(output_a, 403, head_a, close_a, null);
            return;
        }
        if (file_a == null) {
            iSendError(output_a, 404, head_a, close_a, null);
            return;
        }

        byte[] body_a = Files.readAllBytes(file_a);
        Response.iWrite(output_a, 200, body_a, ContentType.iForPath(file_a),
                head_a, close_a, null);
    }

    /** Decodifica e normaliza o caminho, impedindo acesso fora da raiz configurada. */
    private static Path iResolveFile(String target_a, Path root_a) throws IOException {
        if (target_a.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Fragmento inválido em request-target");
        }
        int query_a = target_a.indexOf('?');
        String rawPath_a = query_a < 0 ? target_a : target_a.substring(0, query_a);
        String decodedPath_a = URLDecoder.decode(rawPath_a.replace("+", "%2B"), "UTF-8");
        if (decodedPath_a.indexOf('\\') >= 0 || decodedPath_a.indexOf('\0') >= 0) {
            throw new SecurityException("Separador ou caractere inválido no caminho");
        }

        String relativeText_a = decodedPath_a.startsWith("/")
                ? decodedPath_a.substring(1) : decodedPath_a;
        Path candidate_a;
        try {
            candidate_a = root_a.resolve(Paths.get(relativeText_a)).normalize();
        } catch (InvalidPathException exception_a) {
            throw new IllegalArgumentException("Caminho inválido");
        }
        if (!candidate_a.startsWith(root_a)) {
            throw new SecurityException("Caminho fora da raiz");
        }
        if (!Files.exists(candidate_a)) {
            return null;
        }

        Path realPath_a = candidate_a.toRealPath();
        if (!realPath_a.startsWith(root_a)) {
            throw new SecurityException("Link aponta para fora da raiz");
        }
        if (Files.isDirectory(realPath_a)) {
            realPath_a = realPath_a.resolve("index.html");
            if (!Files.exists(realPath_a)) {
                return null;
            }
            realPath_a = realPath_a.toRealPath();
            if (!realPath_a.startsWith(root_a)) {
                throw new SecurityException("Index aponta para fora da raiz");
            }
        }
        if (!Files.isRegularFile(realPath_a)) {
            return null;
        }
        return realPath_a;
    }

    private static boolean iHasConnectionClose(RequestHTTP request_a) {
        String connection_a = request_a.headers_a.get("connection");
        if (connection_a == null) {
            return false;
        }
        for (String token_a : connection_a.split(",")) {
            if ("close".equals(token_a.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean iHasTransferEncoding(RequestHTTP request_a) {
        String value_a = request_a.headers_a.get("transfer-encoding");
        return value_a != null && !value_a.isEmpty();
    }

    private static boolean iHasInvalidContentLength(RequestHTTP request_a) {
        String value_a = request_a.headers_a.get("content-length");
        if (value_a == null) {
            return false;
        }
        try {
            return Long.parseLong(value_a) < 0;
        } catch (NumberFormatException exception_a) {
            return true;
        }
    }

    private static boolean iHasRequestBody(RequestHTTP request_a) {
        String value_a = request_a.headers_a.get("content-length");
        if (value_a == null) {
            return false;
        }
        try {
            return Long.parseLong(value_a) > 0;
        } catch (NumberFormatException exception_a) {
            return true;
        }
    }

    private static void iSendError(OutputStream output_a, int status_a, boolean head_a,
                                   boolean close_a, String extraHeader_a) throws IOException {
        byte[] body_a = (status_a + " " + iReason(status_a) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        Response.iWrite(output_a, status_a, body_a, ERROR_TYPE_a,
                head_a, close_a, extraHeader_a);
    }

    private static String iReason(int status_a) {
        switch (status_a) {
            case 400:
                return "Bad Request";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            default:
                return "Internal Server Error";
        }
    }

    private static final class ServerConfig {
        private final int port_a;
        private final Path root_a;

        private ServerConfig(int port_a, Path root_a) {
            this.port_a = port_a;
            this.root_a = root_a;
        }
    }
}
