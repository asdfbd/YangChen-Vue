package com.yangchen.common.core.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Tabular response payload.
 */
@Data
public class TableDataInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private long total;
    private List<?> rows;
    private int code;
    private String msg;

    public TableDataInfo() {
    }

    public TableDataInfo(List<?> list, long total) {
        this.rows = list;
        this.total = total;
    }
}