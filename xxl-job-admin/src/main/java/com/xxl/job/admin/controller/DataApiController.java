package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestParam;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequestMapping("/api")
public class DataApiController {

    @Resource
    private AdminBiz adminBiz;
    @Autowired
    private DataSource dataSource;

    @GetMapping("/query-data")
    @ResponseBody
    @PermissionLimit(limit=false)
    public List<Map<String, Object>> queryData(@RequestParam String tableName, @RequestParam(required = false) List<String> fields, @RequestParam(required = false) Map<String, Object> conditions) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String sql = "SELECT ";
        if (fields != null && !fields.isEmpty()) {
            sql += String.join(",", fields);
        } else {
            sql += "*";
        }
        sql += " FROM " + tableName;
        if (conditions != null && !conditions.isEmpty()) {
            sql += " WHERE 1=1";
            List<Object> values = new ArrayList<>();
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                sql += " AND " + key + "=?";
                values.add(value);
            }
            return jdbcTemplate.queryForList(sql, values.toArray());
        } else {
            return jdbcTemplate.queryForList(sql);
        }
    }
}