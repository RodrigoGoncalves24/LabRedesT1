package Servidor;

public class RequestHTTP {

    /// GET / HTTP/1.X
    private String method;
    private String requestTarget;
    private String version;

    /// HOST: LOCALHOST:8080
    private String nameHeader;
    private String value;

    /// USER-AGENT: CURL/8.18.0
    private String userAgent;
    private String curl;

    public RequestHTTP(String[] methodVersion, String[] userAgent, String[] hostLocalHost) {
        this.method = methodVersion[0];
        this.requestTarget = methodVersion[1];
        this.version = methodVersion[2];
        this.nameHeader = hostLocalHost[0];
        this.value = hostLocalHost[1];
        this.userAgent = userAgent[0];
        this.curl = userAgent[1];


    }

    void RequestHTTP(String method, String requestTarget, String version, String nomeHeader, String value, String userAgent) {
        this.method = method;
        this.requestTarget = requestTarget;
        this.version = version;
        this.nameHeader = nomeHeader;
        this.value = value;
        this.userAgent = userAgent;
    }

    public String getMethod() {
        return method;
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    public String getVersion() {
        return version;
    }

    public String getNameHeader() {
        return nameHeader;
    }

    public String getValue() {
        return value;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getCurl() {
        return curl;
    }

    public String toString() {
        return "\nMethod: "+getMethod() +
                "\nRequestTarget: "+getRequestTarget() +
                "\nVersion: "+getVersion() +
                "\nValue host: "+getValue()+
                "\nUserAgent: "+getCurl();
    }
}