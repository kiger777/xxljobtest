package com.xxl.job.admin.core.alarm.impl;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class SmsJobAlarmTest {

    @Test
    void sendSms_Http() {
        JsonObject jsonObj =new JsonObject();
        jsonObj.addProperty("account","yw@006");
        jsonObj.addProperty("phoneNumber","18602755732");
        jsonObj.addProperty("status","PROBLEM");
        jsonObj.addProperty("happenTime","16:59:00");
        jsonObj.addProperty("level","Disaster");
        jsonObj.addProperty("hostname","分布式任务管理中心");
        jsonObj.addProperty("hostip","10.11.82.74");
        jsonObj.addProperty("messageInfo","test");
        jsonObj.addProperty("value","up");
        SmsJobAlarm.SendSms_Http(jsonObj);
    }

}