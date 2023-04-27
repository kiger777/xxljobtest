package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * job alarm by email
 *
 * @author xuxueli 2020-01-19
 */
@Component
public class EmailJobAlarm implements JobAlarm {
    //    定义当前的  日志对象
    private static Logger logger = LoggerFactory.getLogger(EmailJobAlarm.class);

    /**
     * fail alarm
     *  失败报警 逻辑
     *  开始进行  报警的逻辑
     * @param jobLog
     */

    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog){
        //        定义报警结果状态
        boolean alarmResult = true;

        // send monitor email
        if (info!=null && info.getAlarmEmail()!=null && info.getAlarmEmail().trim().length()>0) {
            // 如果 任务信息不为空，报警邮件不为空
            // alarmContent  报警内容
            String alarmContent = "Alarm Job LogId=" + jobLog.getId(); // 记录日志id
            if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
                //                如果 日志的 调度结果 不成功  ，内容保存 调度-日志
                alarmContent += "<br>TriggerMsg=<br>" + jobLog.getTriggerMsg();
            }
            if (jobLog.getHandleCode()>0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                //               如果 执行-状态  不为空，内容保存  执行-日志具体结果信息
                alarmContent += "<br>HandleCode=" + jobLog.getHandleMsg();
            }

            // email info

            //            根据项目 id  查询出  执行器（项目）
            XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(Integer.valueOf(info.getJobGroup()));
            //          警告信息  发送人
            String personal = I18nUtil.getString("admin_name_full");//  值为这个：  分布式任务调度平台XXL-JOB
            //          告警信息  标题
            String title = I18nUtil.getString("jobconf_monitor");//  值为这个：任务调度中心监控报警

            //            根据模板，填充信息
            String content = MessageFormat.format(loadEmailJobAlarmTemplate(),
                    group!=null?group.getTitle():"null",// 项目名称
                    info.getId(),// 任务id
                    info.getJobDesc(),// 任务 描述
                    alarmContent);// 报警内容

//            多个  报警邮件
            Set<String> emailSet = new HashSet<String>(Arrays.asList(info.getAlarmEmail().split(",")));
            for (String email: emailSet) {
                // 遍历多个报警邮件
                // make mail

                try {
                    //  创建邮件发送者 对象
                    MimeMessage mimeMessage = XxlJobAdminConfig.getAdminConfig().getMailSender().createMimeMessage();
                     //  创建 邮件发送线程
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    //                    从配置文件里面获取自己配置的发送者 的邮箱，发送者的姓名
                    helper.setFrom(XxlJobAdminConfig.getAdminConfig().getEmailFrom(), personal);
                    //                    将邮件发送给谁
                    helper.setTo(email);
                    //                    发送的标题
                    helper.setSubject(title);
                    //                    发送的内容
                    helper.setText(content, true);
                    //                  进行发送
                    XxlJobAdminConfig.getAdminConfig().getMailSender().send(mimeMessage);
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>> xxl-job, job fail alarm email send error, JobLogId:{}", jobLog.getId(), e);

                    alarmResult = false;
                }

            }
        }
//        返回报警状态（是否成功报警，默认 是）
        return alarmResult;
    }

    /**
     * load email job alarm template
     * 加载邮件作业告警模板，，前端的页面模板
     * @return
     */
    private static final String loadEmailJobAlarmTemplate(){
        String mailBodyTemplate = "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>" +
                "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
                "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
                "      <tr>\n" +
                "         <td width=\"20%\" >"+ I18nUtil.getString("jobinfo_field_jobgroup") +"</td>\n" +
                "         <td width=\"10%\" >"+ I18nUtil.getString("jobinfo_field_id") +"</td>\n" +
                "         <td width=\"20%\" >"+ I18nUtil.getString("jobinfo_field_jobdesc") +"</td>\n" +
                "         <td width=\"10%\" >"+ I18nUtil.getString("jobconf_monitor_alarm_title") +"</td>\n" +
                "         <td width=\"40%\" >"+ I18nUtil.getString("jobconf_monitor_alarm_content") +"</td>\n" +
                "      </tr>\n" +
                "   </thead>\n" +
                "   <tbody>\n" +
                "      <tr>\n" +
                "         <td>{0}</td>\n" +
                "         <td>{1}</td>\n" +
                "         <td>{2}</td>\n" +
                "         <td>"+ I18nUtil.getString("jobconf_monitor_alarm_type") +"</td>\n" +
                "         <td>{3}</td>\n" +
                "      </tr>\n" +
                "   </tbody>\n" +
                "</table>";

        return mailBodyTemplate;
    }

}
