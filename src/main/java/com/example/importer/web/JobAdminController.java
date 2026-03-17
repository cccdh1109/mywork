package com.example.importer.web;

import com.example.importer.scheduler.JobSchedulerManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/import-jobs")
public class JobAdminController {

    private final JobSchedulerManager schedulerManager;

    public JobAdminController(JobSchedulerManager schedulerManager) {
        this.schedulerManager = schedulerManager;
    }

    @PostMapping("/reload")
    public Map<String, String> reload() {
        schedulerManager.reloadJobs();
        return schedulerManager.currentSchedules();
    }
}
