package com.collabx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "collabx.judge0")
public class Judge0Properties {

    private String apiUrl = "https://ce.judge0.com";
    private String apiKey = "";
    private String apiHost = "";
    private long pollIntervalMs = 1000;
    private int maxPollAttempts = 25;
    private long fetchTimeoutMs = 15000;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getMaxPollAttempts() {
        return maxPollAttempts;
    }

    public void setMaxPollAttempts(int maxPollAttempts) {
        this.maxPollAttempts = maxPollAttempts;
    }

    public long getFetchTimeoutMs() {
        return fetchTimeoutMs;
    }

    public void setFetchTimeoutMs(long fetchTimeoutMs) {
        this.fetchTimeoutMs = fetchTimeoutMs;
    }
}
