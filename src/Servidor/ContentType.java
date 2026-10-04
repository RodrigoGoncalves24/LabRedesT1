package Servidor;

import java.nio.file.Path;
import java.util.Locale;

/** Mapeia extensões de arquivos para os media types enviados pelo servidor. */
final class ContentType {
    private ContentType() {
    }

    /** Usa application/octet-stream como fallback para extensões desconhecidas. */
    static String iForPath(Path path_a) {
        String name_a = path_a.getFileName() == null
                ? "" : path_a.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot_a = name_a.lastIndexOf('.');
        String extension_a = dot_a < 0 ? "" : name_a.substring(dot_a + 1);

        switch (extension_a) {
            case "html":
            case "htm":
                return "text/html; charset=utf-8";
            case "css":
                return "text/css; charset=utf-8";
            case "js":
                return "text/javascript; charset=utf-8";
            case "json":
                return "application/json";
            case "txt":
                return "text/plain; charset=utf-8";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }
}
