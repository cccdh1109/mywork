package com.example.importer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.example.importer.model.ImportJobConfig;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OssToEsImportService {

    private static final Logger log = LoggerFactory.getLogger(OssToEsImportService.class);

    private final OSS oss;
    private final RestHighLevelClient esClient;
    private final ProcessedFileStore processedFileStore;

    public OssToEsImportService(OSS oss, RestHighLevelClient esClient, ProcessedFileStore processedFileStore) {
        this.oss = oss;
        this.esClient = esClient;
        this.processedFileStore = processedFileStore;
    }

    public void runJob(ImportJobConfig job) {
        long start = System.currentTimeMillis();
        log.info("[{}] 开始导入, bucket={}, prefix={}, index={}", job.getName(), job.getBucket(), job.getPrefix(), job.getIndex());

        int fileCount = 0;
        int docCount = 0;
        ObjectListing listing = oss.listObjects(job.getBucket(), job.getPrefix());
        List<OSSObjectSummary> all = new ArrayList<OSSObjectSummary>(listing.getObjectSummaries());
        while (listing.isTruncated()) {
            listing = oss.listNextBatchOfObjects(listing);
            all.addAll(listing.getObjectSummaries());
        }

        for (OSSObjectSummary objectSummary : all) {
            String key = objectSummary.getKey();
            if (!matchSuffix(key, job.getFileSuffixes())) {
                continue;
            }
            String etag = objectSummary.getETag();
            if (processedFileStore.isProcessed(job.getName(), key, etag)) {
                log.info("[{}] 跳过已处理文件: {}", job.getName(), key);
                continue;
            }

            int imported = importSingleFile(job, key);
            fileCount++;
            docCount += imported;
            processedFileStore.markProcessed(job.getName(), key, etag);
            log.info("[{}] 文件导入完成: {}, 文档数={}", job.getName(), key, imported);
        }

        log.info("[{}] 导入结束, 文件数={}, 文档数={}, 耗时={}ms", job.getName(), fileCount, docCount, (System.currentTimeMillis() - start));
    }

    private int importSingleFile(ImportJobConfig job, String key) {
        int lines = 0;
        try (OSSObject ossObject = oss.getObject(job.getBucket(), key);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(ossObject.getObjectContent(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            BulkRequest bulkRequest = new BulkRequest();
            while ((line = reader.readLine()) != null) {
                if (job.isSkipHeader() && firstLine) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                Map<String, Object> source = parseLine(job, line);
                if (source == null) {
                    continue;
                }

                IndexRequest request = new IndexRequest(job.getIndex())
                    .source(source, XContentType.JSON);
                if (job.getIdField() != null && !job.getIdField().isEmpty()) {
                    Object id = source.get(job.getIdField());
                    if (id != null) {
                        request.id(String.valueOf(id));
                    }
                }
                bulkRequest.add(request);
                lines++;

                if (bulkRequest.numberOfActions() >= 500) {
                    executeBulk(job.getName(), bulkRequest);
                    bulkRequest = new BulkRequest();
                }
            }

            if (bulkRequest.numberOfActions() > 0) {
                executeBulk(job.getName(), bulkRequest);
            }
        } catch (IOException e) {
            throw new RuntimeException("导入文件失败, key=" + key, e);
        }
        return lines;
    }

    private void executeBulk(String jobName, BulkRequest bulkRequest) throws IOException {
        BulkResponse response = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (response.hasFailures()) {
            log.warn("[{}] ES批量写入存在失败: {}", jobName, response.buildFailureMessage());
        } else {
            log.info("[{}] ES批量写入成功, actions={}", jobName, bulkRequest.numberOfActions());
        }
    }

    private Map<String, Object> parseLine(ImportJobConfig job, String line) {
        String[] values = line.split(java.util.regex.Pattern.quote(job.getDelimiter()), -1);
        List<String> fields = job.getFields();
        if (fields.isEmpty()) {
            log.error("[{}] 未配置字段映射, 无法导入", job.getName());
            return null;
        }
        if (values.length != fields.size()) {
            log.warn("[{}] 字段数不匹配, line={}, expected={}, actual={}", job.getName(), line, fields.size(), values.length);
            return null;
        }

        Map<String, Object> data = new HashMap<String, Object>();
        for (int i = 0; i < fields.size(); i++) {
            data.put(fields.get(i), values[i]);
        }
        return data;
    }

    private boolean matchSuffix(String key, List<String> suffixes) {
        for (String suffix : suffixes) {
            if (key.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
