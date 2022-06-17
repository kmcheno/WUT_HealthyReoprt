package com.ming.healthyreport.service;

import com.ming.healthyreport.model.User;

public interface ReportService {
    boolean report(User user, int waitTime, int attemptNum);
}
