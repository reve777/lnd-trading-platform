package com.lightning.trading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lnd")
public class LndConfig {

    private String mode = "mock";
    private String host = "localhost";
    private int port = 8080;
    private String macaroonHex = "";
    private String tlsCertPath = "";
    private boolean useSsl = false;

    public String getBaseUrl() {
        String scheme = useSsl ? "https" : "http";
        return scheme + "://" + host + ":" + port;
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getMacaroonHex() { return macaroonHex; }
    public void setMacaroonHex(String macaroonHex) { this.macaroonHex = macaroonHex; }

    public String getTlsCertPath() { return tlsCertPath; }
    public void setTlsCertPath(String tlsCertPath) { this.tlsCertPath = tlsCertPath; }

    public boolean isUseSsl() { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
}
