package Servidor;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Response {

    private final Map<String, String> codes = new HashMap<>(Map.of(
            "200", "OK",
            "400", "Bad Request" ,
            "403", "Forbidden" ,
            "404", "Not Found" ,
            "405", "Not Allowed"));

    private final String httpVersion = "HTTP/1.1";
    private String code;
    private int contentLength;
    private ContentType contentType;
    private final String grupo = "Grupo 10 - João Krampe, Matheus Cademartori, Ravel, Rodrigo Gonçalves";
    private String contentEncoding;


    // Date format
    private static final ZonedDateTime date = ZonedDateTime.now(ZoneId.of("GMT"));
    private static final String fixDate = date.format(DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US));


    public Response(String code, ContentType contentType, String contentEncoding, int contentLength) {
        this.code = code;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.contentLength = contentLength;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public String getCode() {

        return  code;
    }

    public String getCodeSignificate(){
        if(codes.containsKey(code)){
            return codes.get(code);
        }

        return codes.get("404");
    }

    public int getContentLength() {
        return contentLength;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String toString() {
        return  getHttpVersion()+" "+getCode() +" "+getCodeSignificate()+"\r\n"+
                "Content-Length: " + getContentLength()+ "\r\n" +
                "Content-Type: "+ getContentType()+"\r\n" +
                "\r\n" +
                getContentEncoding();
    }
}
