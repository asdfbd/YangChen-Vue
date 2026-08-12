package com.yangchen.quartz.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yangchen.common.utils.PageUtils;
import com.yangchen.quartz.entity.SysJobLog;
import com.yangchen.quartz.mapper.SysJobLogMapper;
import com.yangchen.quartz.service.SysJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务调度日志信息 服务层
 *
 * @author yangchen
 */
@Service
public class SysJobLogServiceImpl implements SysJobLogService {
    @Autowired
    private SysJobLogMapper jobLogMapper;

    /**
     * 获取quartz调度器日志的计划任务
     *
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog) {
        return PageUtils.selectPage(jobLogMapper, new LambdaQueryWrapper<SysJobLog>()
                .like(StrUtil.isNotBlank(jobLog.getJobName()), SysJobLog::getJobName, jobLog.getJobName())
                .eq(StrUtil.isNotBlank(jobLog.getJobGroup()), SysJobLog::getJobGroup, jobLog.getJobGroup())
                .eq(StrUtil.isNotBlank(jobLog.getStatus()), SysJobLog::getStatus, jobLog.getStatus())
                .like(StrUtil.isNotBlank(jobLog.getInvokeTarget()), SysJobLog::getInvokeTarget, jobLog.getInvokeTarget())
                .apply(ObjectUtil.isNotNull(jobLog.getParams().get("beginTime")),
                        "to_char(create_time, 'YYYYMMDD') >= to_char({0}::timestamp, 'YYYYMMDD')",
                        jobLog.getParams().get("beginTime"))
                .apply(ObjectUtil.isNotNull(jobLog.getParams().get("endTime")),
                        "to_char(create_time, 'YYYYMMDD') <= to_char({0}::timestamp, 'YYYYMMDD')",
                        jobLog.getParams().get("endTime"))
                .orderByDesc(SysJobLog::getCreateTime));
    }

    /**
     * 通过调度任务日志ID查询调度信息
     *
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    @Override
    public SysJobLog selectJobLogById(Long jobLogId) {
        return jobLogMapper.selectJobLogById(jobLogId);
    }

    /**
     * 新增任务日志
     *
     * @param jobLog 调度日志信息
     */
    @Override
    public void addJobLog(SysJobLog jobLog) {
        jobLogMapper.insertJobLog(jobLog);
    }

    /**
     * 批量删除调度日志信息
     *
     * @param logIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteJobLogByIds(Long[] logIds) {
        return jobLogMapper.deleteJobLogByIds(logIds);
    }

    /**
     * 删除任务日志
     *
     * @param jobId 调度日志ID
     */
    @Override
    public int deleteJobLogById(Long jobId) {
        return jobLogMapper.deleteJobLogById(jobId);
    }

    /**
     * 清空任务日志
     */
    @Override
    public void cleanJobLog() {
        jobLogMapper.cleanJobLog();
    }
}
