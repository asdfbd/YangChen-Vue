package com.yangchen.common.exception.file;

import com.yangchen.common.exception.base.BaseException;

/**
 * 文件信息异常类
 *
 * @author yangchen
 */
public class FileException extends BaseException {
    private static final long serialVersionUID = 1L;

    public FileException(String code, Object[] args) {
        super("file", code, args, null);
    }

}
