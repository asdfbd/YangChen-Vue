package com.yangchen.ai.controller;

import com.yangchen.ai.entity.AiFormConfig;
import com.yangchen.ai.service.AiFormConfigService;
import com.yangchen.common.annotation.Log;
import com.yangchen.common.core.controller.BaseController;
import com.yangchen.common.core.domain.R;
import com.yangchen.common.core.page.TableDataInfo;
import com.yangchen.common.enums.BusinessType;
import com.yangchen.common.utils.poi.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ai单配置Controller
 *
 * @author yangchen
 * @date 2026-08-21
 */
@Tag(name = "ai单配置")
@RestController
@RequestMapping("/ai/ai")
public class AiFormConfigController extends BaseController {
    @Autowired
    private AiFormConfigService aiFormConfigService;

    /**
     * 查询ai单配置列表
     */
    @Operation(summary = "查询ai单配置列表")
    @PreAuthorize("@ss.hasPermi('ai:ai:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiFormConfig aiFormConfig) {
        startPage();
        List<AiFormConfig> list = aiFormConfigService.selectAiFormConfigList(aiFormConfig);
        return getDataTable(list);
    }

    /**
     * 导出ai单配置列表
     */
    @Operation(summary = "导出ai单配置列表")
    @PreAuthorize("@ss.hasPermi('ai:ai:export')")
    @Log(title = "ai单配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiFormConfig aiFormConfig) {
        List<AiFormConfig> list = aiFormConfigService.selectAiFormConfigList(aiFormConfig);
        ExcelUtil<AiFormConfig> util = new ExcelUtil<AiFormConfig>(AiFormConfig.class);
        util.exportExcel(response, list, "ai单配置数据");
    }

    /**
     * 获取ai单配置详细信息
     */
    @Operation(summary = "获取ai单配置详细信息")
    @PreAuthorize("@ss.hasPermi('ai:ai:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") @Parameter(description = "ai单配置主键") Long id) {
        return success(aiFormConfigService.selectAiFormConfigById(id));
    }

    /**
     * 新增ai单配置
     */
    @Operation(summary = "新增ai单配置")
    @PreAuthorize("@ss.hasPermi('ai:ai:add')")
    @Log(title = "ai单配置", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody AiFormConfig aiFormConfig) {
        return toAjax(aiFormConfigService.insertAiFormConfig(aiFormConfig));
    }

    /**
     * 修改ai单配置
     */
    @Operation(summary = "修改ai单配置")
    @PreAuthorize("@ss.hasPermi('ai:ai:edit')")
    @Log(title = "ai单配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody AiFormConfig aiFormConfig) {
        return toAjax(aiFormConfigService.updateAiFormConfig(aiFormConfig));
    }

    /**
     * 删除ai单配置
     */
    @Operation(summary = "删除ai单配置")
    @PreAuthorize("@ss.hasPermi('ai:ai:remove')")
    @Log(title = "ai单配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R remove(@PathVariable @Parameter(description = "ai单配置主键集合") Long[] ids) {
        return toAjax(aiFormConfigService.deleteAiFormConfigByIds(ids));
    }
}
