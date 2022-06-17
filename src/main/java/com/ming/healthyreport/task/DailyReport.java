package com.ming.healthyreport.task;

import com.ming.healthyreport.model.User;
import com.ming.healthyreport.repository.UserRepository;
import com.ming.healthyreport.service.ReportService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyReport {
    private final ReportService reportService;
    private final UserRepository repository;

    public DailyReport(ReportService reportService, UserRepository repository) {
        this.reportService = reportService;
        this.repository = repository;
    }

    @Scheduled(cron = "0 30 6 * * ?")
    public void scheduledReport() {
        List<User> userList = repository.findAll();
        userList.forEach(user -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            reportService.report(user, 10, 5);

        });
    }
}
