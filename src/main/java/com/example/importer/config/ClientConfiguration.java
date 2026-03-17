package com.example.importer.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfiguration {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(OssProperties properties) {
        return new OSSClientBuilder().build(
            properties.getEndpoint(),
            properties.getAccessKeyId(),
            properties.getAccessKeySecret()
        );
    }

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient(EsProperties properties) {
        return new RestHighLevelClient(
            RestClient.builder(new HttpHost(properties.getHost(), properties.getPort(), properties.getScheme()))
        );
    }
}
