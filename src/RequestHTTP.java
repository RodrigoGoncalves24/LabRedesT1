public class RequestHTTP {
    private String method;
    private String requestTarget;
    private String version;
    private String nameHeader;
    private String value;
    private String userAgent;

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
}
