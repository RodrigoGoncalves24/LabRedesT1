package Servidor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Serializa respostas HTTP em bytes, mantendo o comprimento correto para GET e HEAD. */
final class Response {
    private static final DateTimeFormatter IMF_FIXDATE_a =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                    .withZone(ZoneOffset.UTC);
    private static final String SERVER_ID_a = "Grupo10-HTTPServer";

    private Response() {
    }

    /** Escreve status line, headers e, exceto em HEAD, o corpo binário. */
    static void iWrite(OutputStream output_a, int status_a, byte[] body_a, String contentType_a,
                       boolean head_a, boolean close_a, String extraHeader_a) throws IOException {
        String reason_a = iReason(status_a);
        StringBuilder header_a = new StringBuilder();
        header_a.append("HTTP/1.1 ").append(status_a).append(' ').append(reason_a).append("\r\n");
        header_a.append("Date: ").append(IMF_FIXDATE_a.format(Instant.now())).append("\r\n");
        header_a.append("Server: ").append(SERVER_ID_a).append("\r\n");
        header_a.append("Content-Length: ").append(body_a.length).append("\r\n");
        header_a.append("Content-Type: ").append(contentType_a).append("\r\n");
        if (extraHeader_a != null && !extraHeader_a.isEmpty()) {
            header_a.append(extraHeader_a).append("\r\n");
        }
        if (close_a) {
            header_a.append("Connection: close\r\n");
        }
        header_a.append("\r\n");

        output_a.write(header_a.toString().getBytes(StandardCharsets.US_ASCII));
        if (!head_a) {
            output_a.write(body_a);
        }
        output_a.flush();
    }

    private static String iReason(int status_a) {
        switch (status_a) {
            case 200:
                return "OK";
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
}
