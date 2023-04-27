package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 上下文，项目初始化的时候  加载这个
 */
@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {

    //    定义 当前类 日志对象
    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);
    //    上下文  管理器
    private ApplicationContext applicationContext;
//    发送告警信息  list
//    多个报警类  bean对象的  集合
    private List<JobAlarm> jobAlarmList;


    /**
     * 设置上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 设置上下文
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //        从spring管理器 里面  获取到  报警类的 bean对象
        //        根据类型获取  多个bean对象
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
            jobAlarmList = new ArrayList<JobAlarm>(serviceBeanMap.values());
        }
    }

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean alarm(XxlJobInfo info, XxlJobLog jobLog) {

        boolean result = false;
        //        如果  多个报警类  bean对象的  集合 不为空
        if (jobAlarmList!=null && jobAlarmList.size()>0) {
            //        result = true 意味的  全部的 报警email 发放  都成功了
            result = true;  // success means all-success
            for (JobAlarm alarm: jobAlarmList) {
                //                遍历  每一个  email发送类对象
                //               每一个  email发送类对象   是否发送成功
                boolean resultItem = false;
                try {
                    //                    发送email
                    resultItem = alarm.doAlarm(info, jobLog);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }

        return result;
    }

}
