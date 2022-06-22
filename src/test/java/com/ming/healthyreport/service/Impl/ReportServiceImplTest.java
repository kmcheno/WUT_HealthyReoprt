package com.ming.healthyreport.service.Impl;

import static org.junit.jupiter.api.Assertions.*;

import com.ming.healthyreport.model.User;
import org.junit.jupiter.api.Test;

class ReportServiceImplTest {

    @Test
    void report() {
        User user = new User();
        user.setUserId("xxx");
        user.setUserPassword("xxx");
        new ReportServiceImpl().report(user, 10, 5);
    }
}
