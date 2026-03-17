package com.example.importer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import")
public class AppProperties {

    private String configFile = "./config/import-jobs.properties";
    private long reloadIntervalSeconds = 30;
    private String stateFile = "./data/import-state.properties";

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public long getReloadIntervalSeconds() {
        return reloadIntervalSeconds;
    }

    public void setReloadIntervalSeconds(long reloadIntervalSeconds) {
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }

    public String getStateFile() {
        return stateFile;
    }

    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }
}
