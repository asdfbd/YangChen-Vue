package com.yangchen.quartz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangchen.quartz.entity.SysJob;

import java.util.List;

/**
 * 调度任务信息 数据层
 *
 * @author yangchen
 */
public interface SysJobMapper extends BaseMapper<SysJob> {
    /**
     * 查询所有调度任务
     *
     * @return 调度任务列表
     */
    public List<SysJob> selectJobAll();

    /**
     * 通过调度ID查询调度任务信息
     *
     * @param jobId 调度ID
     * @return 角色对象信息
     */
    default SysJob selectJobById(Long jobId) {
        return selectById(jobId);
    }

    /**
     * 通过调度ID删除调度任务信息
     *
     * @param jobId 调度ID
     * @return 结果
     */
    default int deleteJobById(Long jobId) {
        return deleteById(jobId);
    }

    /**
     * 批量删除调度任务信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    default int deleteJobByIds(Long[] ids) {
        return deleteByIds(java.util.Arrays.asList(ids));
    }

    /**
     * 修改调度任务信息
     *
     * @param job 调度任务信息
     * @return 结果
     */
    default int updateJob(SysJob job) {
        return updateById(job);
    }

    /**
     * 新增调度任务信息
     *
     * @param job 调度任务信息
     * @return 结果
     */
    default int insertJob(SysJob job) {
        return insert(job);
    }
}
