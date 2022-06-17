package com.ming.healthyreport.service.Impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;


class ReportImplTest {

    @Test
    void report() {
        JSONObject data = JSONUtil.createObj()
            .set("sn", "01")
            .set("idCard", "01");
    }
}