package com.yangchen.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 公告已读记录表 sys_notice_read
 *
 * @author yangchen
 */
@Data
@TableName("sys_notice_read")
@Schema(description = "公告已读记录表 sys_notice_read")
public class SysNoticeRead {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(value = "read_id", type = IdType.ASSIGN_ID)
    private Long readId;

    /**
     * 公告ID
     */
    @Schema(description = "公告ID")
    private Long noticeId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 阅读时间
     */
    @Schema(description = "阅读时间")
    private Date readTime;
}
