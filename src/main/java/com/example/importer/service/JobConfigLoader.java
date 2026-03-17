package com.example.importer.service;

import com.example.importer.config.AppProperties;
import com.example.importer.model.ImportJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class JobConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(JobConfigLoader.class);

    private final AppProperties appProperties;

    public JobConfigLoader(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public List<ImportJobConfig> loadJobs() {
        Properties p = new Properties();
        String file = appProperties.getConfigFile();
        try (InputStream in = new FileInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            log.error("无法读取任务配置文件: {}", file, e);
            return Collections.emptyList();
        }

        String rawJobs = p.getProperty("jobs", "").trim();
        if (rawJobs.isEmpty()) {
            log.warn("任务配置文件中未配置 jobs, file={}", file);
            return Collections.emptyList();
        }

        List<ImportJobConfig> jobs = new ArrayList<ImportJobConfig>();
        for (String name : splitCsv(rawJobs)) {
            String prefix = "job." + name + ".";
            ImportJobConfig c = new ImportJobConfig();
            c.setName(name);
            c.setEnabled(Boolean.parseBoolean(p.getProperty(prefix + "enabled", "true")));
            c.setCron(p.getProperty(prefix + "cron", "0 0/10 * * * ?"));
            c.setBucket(p.getProperty(prefix + "bucket"));
            c.setPrefix(p.getProperty(prefix + "prefix", ""));
            c.setIndex(p.getProperty(prefix + "index"));
            c.setDelimiter(p.getProperty(prefix + "delimiter", "|"));
            c.setFields(splitCsv(p.getProperty(prefix + "fields", "")));
            List<String> suffixes = splitCsv(p.getProperty(prefix + "file-suffixes", ".dat,.txt"));
            c.setFileSuffixes(suffixes);
            c.setSkipHeader(Boolean.parseBoolean(p.getProperty(prefix + "skip-header", "false")));
            c.setIdField(p.getProperty(prefix + "id-field", ""));
            jobs.add(c);
        }

        log.info("加载任务配置成功, 共 {} 个任务", jobs.size());
        return jobs;
    }

    private List<String> splitCsv(String v) {
        if (v == null || v.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(v.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
