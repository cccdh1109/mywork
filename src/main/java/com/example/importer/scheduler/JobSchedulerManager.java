package com.example.importer.scheduler;

import com.example.importer.config.AppProperties;
import com.example.importer.model.ImportJobConfig;
import com.example.importer.service.JobConfigLoader;
import com.example.importer.service.OssToEsImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
public class JobSchedulerManager {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulerManager.class);

    private final JobConfigLoader loader;
    private final OssToEsImportService importService;
    private final AppProperties appProperties;

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();
    private String jobsFingerprint = "";

    public JobSchedulerManager(JobConfigLoader loader, OssToEsImportService importService, AppProperties appProperties) {
        this.loader = loader;
        this.importService = importService;
        this.appProperties = appProperties;
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("import-job-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        reloadJobs();
        long intervalMs = appProperties.getReloadIntervalSeconds() * 1000;
        taskScheduler.scheduleWithFixedDelay(this::safeReloadJobs, intervalMs);
        log.info("任务调度初始化完成, reloadIntervalSeconds={}", appProperties.getReloadIntervalSeconds());
    }

    public synchronized void reloadJobs() {
        List<ImportJobConfig> jobs = loader.loadJobs();
        StringBuilder fp = new StringBuilder();
        for (ImportJobConfig job : jobs) {
            fp.append(job.fingerprint()).append(";");
        }
        if (fp.toString().equals(jobsFingerprint)) {
            return;
        }

        for (ScheduledFuture<?> future : futures.values()) {
            future.cancel(false);
        }
        futures.clear();

        for (ImportJobConfig job : jobs) {
            if (!job.isEnabled()) {
                log.info("任务禁用, 跳过调度: {}", job.getName());
                continue;
            }
            ScheduledFuture<?> future = taskScheduler.schedule(
                () -> importService.runJob(job),
                new CronTrigger(job.getCron())
            );
            futures.put(job.getName(), future);
            log.info("任务调度完成: {}, cron={}", job.getName(), job.getCron());
        }

        jobsFingerprint = fp.toString();
        log.info("任务配置已刷新, 生效任务数={}", futures.size());
    }

    public synchronized Map<String, String> currentSchedules() {
        Map<String, String> result = new HashMap<String, String>();
        for (String name : futures.keySet()) {
            result.put(name, "scheduled");
        }
        return result;
    }

    private void safeReloadJobs() {
        try {
            reloadJobs();
        } catch (Exception e) {
            log.error("自动刷新任务配置失败", e);
        }
    }
}
