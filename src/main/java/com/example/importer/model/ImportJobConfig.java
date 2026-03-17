package com.example.importer.model;

import java.util.ArrayList;
import java.util.List;

public class ImportJobConfig {

    private String name;
    private boolean enabled = true;
    private String cron;
    private String bucket;
    private String prefix;
    private String index;
    private String delimiter = "|";
    private List<String> fields = new ArrayList<String>();
    private List<String> fileSuffixes = new ArrayList<String>();
    private boolean skipHeader;
    private String idField;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getFileSuffixes() {
        return fileSuffixes;
    }

    public void setFileSuffixes(List<String> fileSuffixes) {
        this.fileSuffixes = fileSuffixes;
    }

    public boolean isSkipHeader() {
        return skipHeader;
    }

    public void setSkipHeader(boolean skipHeader) {
        this.skipHeader = skipHeader;
    }

    public String getIdField() {
        return idField;
    }

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public String fingerprint() {
        return name + "|" + enabled + "|" + cron + "|" + bucket + "|" + prefix + "|" + index + "|" + delimiter
            + "|" + fields.toString() + "|" + fileSuffixes.toString() + "|" + skipHeader + "|" + idField;
    }
}
