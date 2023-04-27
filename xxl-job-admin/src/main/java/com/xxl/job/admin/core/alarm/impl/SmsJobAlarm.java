package com.xxl.job.admin.core.alarm.impl;

import com.google.gson.JsonObject;
import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import java.util.stream.Collectors;
import java.util.Collections;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * job alarm by Sms
 *
 * @author huqian 2020-09-02
 */
@Component
public class SmsJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(SmsJobAlarm.class);
//日志对象
    /**
     * fail alarm
     *任务失败，开始进行告警逻辑
     * @param jobLog
     **/

    // 新增方法：发送POST告警

    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean alarmResult = true; //定义报警结果状态
        // send monitor sms
        if (info!=null && info.getAlarmSms()!=null && info.getAlarmSms().trim().length()>0) {
            // 如果 任务信息不为空，报警邮件不为空
            XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(info.getJobGroup());
            String executorIp = jobLog.getExecutorAddress();
            if (executorIp == null || executorIp.isEmpty()) {
                executorIp = "NULL";
            }

            //             alarmContent报警内容
            String alarmContent = "【" + I18nUtil.getString("jobinfo_field_jobgroup") + ": [" + group.getTitle()+"] ";
            alarmContent += I18nUtil.getString("jobinfo_field_id") +": [" + info.getId()+ "] "; //记录id
            alarmContent += I18nUtil.getString("jobinfo_field_jobdesc") +": [" + info.getJobDesc()+"] ";//记录任务描述

            System.out.println(jobLog.getTriggerMsg());
            String[] arr = jobLog.getTriggerMsg().split("<br>");
            String cron_type = arr[0];
            String exector_addr = arr[3];
            String exec_msg = arr[10];

            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += I18nUtil.getString("jobconf_monitor_alarm_title") +": [" +
                        I18nUtil.getString("jobconf_monitor_alarm_type")+"] ";
                alarmContent += I18nUtil.getString("jobconf_monitor_alarm_content") +": [" +
                cron_type + "; " + exector_addr + "; " + exec_msg + ",平台异常！！！]】";
            }
            if (jobLog.getHandleCode()>0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                alarmContent += I18nUtil.getString("jobconf_monitor_alarm_title") +": [" +
                        "执行失败"+"]";
                alarmContent += I18nUtil.getString("jobconf_monitor_alarm_content") + ": [" +
                        jobLog.getHandleMsg() + "]】";
            }

            // sms info
            String happenTime = new SimpleDateFormat("HH:mm:ss").format(jobLog.getTriggerTime());
            String personal = I18nUtil.getString("admin_name_full");
            String hostip = null;


            try {
                hostip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String messageInfo = I18nUtil.getString("jobconf_monitor");
            String phoneSet = info.getAlarmSms();
            String groupFlag = info.getgroupFlag();

            JsonObject jsonObj =new JsonObject();
            jsonObj.addProperty("toUser",phoneSet);
            jsonObj.addProperty("fromUser",XxlJobAdminConfig.getAdminConfig().getfromUser());
            jsonObj.addProperty("type",XxlJobAdminConfig.getAdminConfig().gettype());
            jsonObj.addProperty("content",alarmContent);
            jsonObj.addProperty("smsType",XxlJobAdminConfig.getAdminConfig().getsmsType());
            jsonObj.addProperty("groupFlag",groupFlag);
            jsonObj.addProperty("md5Key",XxlJobAdminConfig.getAdminConfig().getmd5Key());
            System.out.println(jsonObj);
            //make phone

            // 在此处创建一个新的 Map 来存储自定义告警内容，这里的告警内容不会影响 SendSms_Http 方法
           Map<String, String> customAlarmData = new HashMap<>();
            customAlarmData.put("alarm_type", "mysql"); //固定
            customAlarmData.put("alarm_plat", info != null && group.getAppname() != null ? group.getAppname() : "xxl_job");//获取appname
            customAlarmData.put("alarm_plat_ch_name", info != null && group.getTitle() != null ? group.getTitle() : "定时任务管理平台");//平台名,title
           // customAlarmData.put("alarm_plat_ssh_ip", "localhost");
            customAlarmData.put("alarm_plat_ssh_ip", executorIp);//平台ip

            customAlarmData.put("alarm_plat_user", info != null && group.getAppname() != null ? group.getAppname() : "xxl_job");//平台用户
            customAlarmData.put("alarm_level", "error");//告警级别
            customAlarmData.put("alarm_id", info != null ? String.valueOf(info.getId()) : "任务id");//告警id
            customAlarmData.put("alarm_msg_name", info != null && info.getJobDesc() != null ? info.getJobDesc() : "任务id");//告警名称
            customAlarmData.put("alarm_interval", "60");//固定
            customAlarmData.put("alarm_detail", alarmContent);//告警详情


            try {
                    SendSms_Http(jsonObj);
                    sendPostAlarm(customAlarmData); // 使用新的参数类型调用方法
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>> xxl-job, job fail alarm sms send error, JobLogId:{}", jobLog.getId(), e);

                    alarmResult = false;
                }

        }
        return alarmResult;
    }


    public static void SendSms_Http(JsonObject json){
        String url = XxlJobAdminConfig.getAdminConfig().getSmsUrl();  //获取配置文件的短信smsurl
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type","application/json");
        requestHeaders.set("charset","utf-8");

        HttpEntity request = new HttpEntity(json.toString(), requestHeaders);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object> responseEntity = restTemplate.exchange(url, HttpMethod.POST,request,Object.class);
        Object object = responseEntity.getBody();
        System.out.println(object);
        HttpStatus statusCode=responseEntity.getStatusCode();
        System.out.println(statusCode);
        HttpHeaders headers1 =responseEntity.getHeaders();
        System.out.println(headers1);
    }

    //小商平台的页面告警
    public void sendPostAlarm(Map<String, String> alarmData) {
        String postAlarmUrl = XxlJobAdminConfig.getAdminConfig().getwebUrl(); // 设置为实际的 POST 告警 URL

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        requestHeaders.set("charset", "utf-8");

        // 将 Map 转换为 URL 编码格式
        String urlEncodedData = UriComponentsBuilder.newInstance().queryParams(new LinkedMultiValueMap<>(alarmData.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Collections.singletonList(e.getValue())))
        )).build().toUriString().substring(1);

        System.out.println("JSON data: " + urlEncodedData);

        HttpEntity<String> request = new HttpEntity<>(urlEncodedData, requestHeaders);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(postAlarmUrl, HttpMethod.POST, request, String.class);

        String responseBody = responseEntity.getBody();
        HttpStatus statusCode = responseEntity.getStatusCode();
        HttpHeaders responseHeaders = responseEntity.getHeaders();

        // 打印响应信息
        System.out.println("Response Body: " + responseBody);
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response Headers: " + responseHeaders);
    }
}
