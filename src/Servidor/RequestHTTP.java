package Servidor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Requisição HTTP/1.1 parseada, com nomes de headers normalizados para minúsculas. */
final class RequestHTTP {
    private static final int MAX_HEADER_BYTES_a = 32 * 1024;

    final String method_a;
    final String target_a;
    final String version_a;
    final Map<String, String> headers_a;

    private RequestHTTP(String method_a, String target_a, String version_a,
                        Map<String, String> headers_a) {
        this.method_a = method_a;
        this.target_a = target_a;
        this.version_a = version_a;
        this.headers_a = Collections.unmodifiableMap(headers_a);
    }

    /** Lê até CRLF CRLF; o stream bufferizado preserva bytes da requisição seguinte. */
    static RequestHTTP iRead(BufferedInputStream input_a) throws IOException {
        ByteArrayOutputStream header_a = new ByteArrayOutputStream();
        int state_a = 0;

        while (true) {
            int byte_a = input_a.read();
            if (byte_a == -1) {
                if (header_a.size() == 0) {
                    return null;
                }
                throw new IllegalArgumentException("Cabeçalho HTTP incompleto");
            }

            header_a.write(byte_a);
            if (header_a.size() > MAX_HEADER_BYTES_a) {
                throw new IllegalArgumentException("Cabeçalho HTTP excede o limite");
            }

            if (state_a == 0) {
                state_a = byte_a == '\r' ? 1 : 0;
            } else if (state_a == 1) {
                state_a = byte_a == '\n' ? 2 : (byte_a == '\r' ? 1 : 0);
            } else if (state_a == 2) {
                state_a = byte_a == '\r' ? 3 : 0;
            } else if (state_a == 3) {
                if (byte_a == '\n') {
                    break;
                }
                state_a = byte_a == '\r' ? 1 : 0;
            }
        }

        byte[] bytes_a = header_a.toByteArray();
        String text_a = new String(bytes_a, 0, bytes_a.length - 4, StandardCharsets.ISO_8859_1);
        String[] lines_a = text_a.split("\\r\\n", -1);
        if (lines_a.length == 0 || lines_a[0].isEmpty()) {
            throw new IllegalArgumentException("Linha de requisição ausente");
        }

        String[] requestLine_a = lines_a[0].trim().split("\\s+");
        if (requestLine_a.length != 3 || requestLine_a[1].isEmpty()
            || !iIsToken(requestLine_a[0]) || iHasInvalidTarget(requestLine_a[1])) {
            throw new IllegalArgumentException("Linha de requisição inválida");
        }
        if (!"HTTP/1.1".equals(requestLine_a[2])) {
            throw new IllegalArgumentException("Versão HTTP não suportada");
        }

        Map<String, String> headers_a = new LinkedHashMap<>();
        for (int lineIndex_a = 1; lineIndex_a < lines_a.length; lineIndex_a++) {
            String line_a = lines_a[lineIndex_a];
            int colon_a = line_a.indexOf(':');
            if (colon_a <= 0) {
                throw new IllegalArgumentException("Cabeçalho sem nome ou sem ':'");
            }

            String name_a = line_a.substring(0, colon_a);
            String value_a = line_a.substring(colon_a + 1).trim();
            if (!iIsToken(name_a) || iHasInvalidControl(value_a)) {
                throw new IllegalArgumentException("Cabeçalho HTTP inválido");
            }

            String key_a = name_a.toLowerCase(Locale.ROOT);
            headers_a.merge(key_a, value_a, (first_a, next_a) -> first_a + ", " + next_a);
        }

        return new RequestHTTP(requestLine_a[0], requestLine_a[1], requestLine_a[2], headers_a);
    }

    private static boolean iIsToken(String text_a) {
        if (text_a.isEmpty()) {
            return false;
        }
        String separators_a = "()<>@,;:/[]?={}";
        for (int index_a = 0; index_a < text_a.length(); index_a++) {
            char character_a = text_a.charAt(index_a);
            if (character_a <= 32 || character_a >= 127 || character_a == 34
                    || character_a == 92 || separators_a.indexOf(character_a) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean iHasInvalidControl(String text_a) {
        for (int index_a = 0; index_a < text_a.length(); index_a++) {
            char character_a = text_a.charAt(index_a);
            if ((character_a < 32 && character_a != '\t') || character_a == 127) {
                return true;
            }
        }
        return false;
    }

    private static boolean iHasInvalidTarget(String text_a) {
        for (int index_a = 0; index_a < text_a.length(); index_a++) {
            char character_a = text_a.charAt(index_a);
            if (character_a <= 32 || character_a == 127) {
                return true;
            }
        }
        return false;
    }
}
