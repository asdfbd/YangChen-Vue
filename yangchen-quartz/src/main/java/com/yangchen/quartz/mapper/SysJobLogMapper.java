package com.yangchen.quartz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.quartz.entity.SysJobLog;

import java.util.List;

/**
 * 调度任务日志信息 数据层
 *
 * @author yangchen
 */
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {
    /**
     * 查询所有调度任务日志
     *
     * @return 调度任务日志列表
     */
    public List<SysJobLog> selectJobLogAll();

    /**
     * 通过调度任务日志ID查询调度信息
     *
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    default SysJobLog selectJobLogById(Long jobLogId) {
        return selectById(jobLogId);
    }

    /**
     * 新增任务日志
     *
     * @param jobLog 调度日志信息
     * @return 结果
     */
    default int insertJobLog(SysJobLog jobLog) {
        return insert(jobLog);
    }

    /**
     * 批量删除调度日志信息
     *
     * @param logIds 需要删除的数据ID
     * @return 结果
     */
    default int deleteJobLogByIds(Long[] logIds) {
        return deleteByIds(java.util.Arrays.asList(logIds));
    }

    /**
     * 删除任务日志
     *
     * @param jobId 调度日志ID
     * @return 结果
     */
    default int deleteJobLogById(Long jobId) {
        return deleteById(jobId);
    }

    /**
     * 清空任务日志
     */
    public void cleanJobLog();
}
