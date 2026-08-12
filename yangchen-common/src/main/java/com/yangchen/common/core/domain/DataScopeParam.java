package com.yangchen.common.core.domain;

import java.util.Map;

/**
 * Marker for request entities that can carry a data-scope SQL fragment.
 */
public interface DataScopeParam {
    Map<String, Object> getParams();
}
