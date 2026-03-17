package com.example.importer.service;

import com.example.importer.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProcessedFileStore {

    private static final Logger log = LoggerFactory.getLogger(ProcessedFileStore.class);

    private final AppProperties appProperties;
    private final ConcurrentMap<String, String> processed = new ConcurrentHashMap<String, String>();

    public ProcessedFileStore(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        File file = new File(appProperties.getStateFile());
        if (!file.exists()) {
            return;
        }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
            for (String name : p.stringPropertyNames()) {
                processed.put(name, p.getProperty(name));
            }
            log.info("加载导入状态成功, 共 {} 条", processed.size());
        } catch (IOException e) {
            log.warn("加载导入状态失败", e);
        }
    }

    public boolean isProcessed(String jobName, String objectKey, String etag) {
        return etag != null && etag.equals(processed.get(jobName + "::" + objectKey));
    }

    public synchronized void markProcessed(String jobName, String objectKey, String etag) {
        processed.put(jobName + "::" + objectKey, etag == null ? "NO_ETAG" : etag);
        flush();
    }

    private void flush() {
        File file = new File(appProperties.getStateFile());
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Properties p = new Properties();
        for (String key : processed.keySet()) {
            p.setProperty(key, processed.get(key));
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            p.store(out, "processed file state");
        } catch (IOException e) {
            log.error("写入导入状态失败", e);
        }
    }
}
