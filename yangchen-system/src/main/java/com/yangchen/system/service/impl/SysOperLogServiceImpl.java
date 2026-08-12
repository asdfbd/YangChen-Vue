package com.yangchen.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yangchen.common.utils.PageUtils;
import com.yangchen.system.entity.SysOperLog;
import com.yangchen.system.mapper.SysOperLogMapper;
import com.yangchen.system.service.SysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志 服务层处理
 *
 * @author yangchen
 */
@Service
public class SysOperLogServiceImpl implements SysOperLogService {
    @Autowired
    private SysOperLogMapper operLogMapper;

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(SysOperLog operLog) {
        operLogMapper.insert(operLog);
    }

    /**
     * 查询系统操作日志集合
     *
     * @param operLog 操作日志对象
     * @return 操作日志集合
     */
    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog operLog) {
        return PageUtils.selectPage(operLogMapper, new LambdaQueryWrapper<SysOperLog>()
                .like(StrUtil.isNotBlank(operLog.getOperIp()), SysOperLog::getOperIp, operLog.getOperIp())
                .like(StrUtil.isNotBlank(operLog.getTitle()), SysOperLog::getTitle, operLog.getTitle())
                .eq(ObjectUtil.isNotNull(operLog.getBusinessType()), SysOperLog::getBusinessType, operLog.getBusinessType())
                .in(operLog.getBusinessTypes() != null && operLog.getBusinessTypes().length > 0,
                        SysOperLog::getBusinessType, operLog.getBusinessTypes())
                .eq(ObjectUtil.isNotNull(operLog.getStatus()), SysOperLog::getStatus, operLog.getStatus())
                .like(StrUtil.isNotBlank(operLog.getOperName()), SysOperLog::getOperName, operLog.getOperName())
                .ge(ObjectUtil.isNotNull(operLog.getParams().get("beginTime")), SysOperLog::getOperTime, operLog.getParams().get("beginTime"))
                .le(ObjectUtil.isNotNull(operLog.getParams().get("endTime")), SysOperLog::getOperTime, operLog.getParams().get("endTime"))
                .orderByDesc(SysOperLog::getOperId));
    }

    /**
     * 批量删除系统操作日志
     *
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(Long[] operIds) {
        return operLogMapper.deleteByIds(java.util.Arrays.asList(operIds));
    }

    /**
     * 查询操作日志详细
     *
     * @param operId 操作ID
     * @return 操作日志对象
     */
    @Override
    public SysOperLog selectOperLogById(Long operId) {
        return operLogMapper.selectById(operId);
    }

    /**
     * 清空操作日志
     */
    @Override
    public void cleanOperLog() {
        operLogMapper.delete(null);
    }
}
