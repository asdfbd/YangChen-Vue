package com.yangchen.ai.controller;

import com.yangchen.ai.entity.AiFormConfigField;
import com.yangchen.ai.service.AiFormConfigFieldService;
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
 * AI动态单-单项配置Controller
 *
 * @author yangchen
 * @date 2026-08-21
 */
@Tag(name = "AI动态单-单项配置")
@RestController
@RequestMapping("/ai/ai")
public class AiFormConfigFieldController extends BaseController {
    @Autowired
    private AiFormConfigFieldService aiFormConfigFieldService;

    /**
     * 查询AI动态单-单项配置列表
     */
    @Operation(summary = "查询AI动态单-单项配置列表")
    @PreAuthorize("@ss.hasPermi('ai:ai:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiFormConfigField aiFormConfigField) {
        startPage();
        List<AiFormConfigField> list = aiFormConfigFieldService.selectAiFormConfigFieldList(aiFormConfigField);
        return getDataTable(list);
    }

    /**
     * 导出AI动态单-单项配置列表
     */
    @Operation(summary = "导出AI动态单-单项配置列表")
    @PreAuthorize("@ss.hasPermi('ai:ai:export')")
    @Log(title = "AI动态单-单项配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiFormConfigField aiFormConfigField) {
        List<AiFormConfigField> list = aiFormConfigFieldService.selectAiFormConfigFieldList(aiFormConfigField);
        ExcelUtil<AiFormConfigField> util = new ExcelUtil<AiFormConfigField>(AiFormConfigField.class);
        util.exportExcel(response, list, "AI动态单-单项配置数据");
    }

    /**
     * 获取AI动态单-单项配置详细信息
     */
    @Operation(summary = "获取AI动态单-单项配置详细信息")
    @PreAuthorize("@ss.hasPermi('ai:ai:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") @Parameter(description = "AI动态单-单项配置主键") Long id) {
        return success(aiFormConfigFieldService.selectAiFormConfigFieldById(id));
    }

    /**
     * 新增AI动态单-单项配置
     */
    @Operation(summary = "新增AI动态单-单项配置")
    @PreAuthorize("@ss.hasPermi('ai:ai:add')")
    @Log(title = "AI动态单-单项配置", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody AiFormConfigField aiFormConfigField) {
        return toAjax(aiFormConfigFieldService.insertAiFormConfigField(aiFormConfigField));
    }

    /**
     * 修改AI动态单-单项配置
     */
    @Operation(summary = "修改AI动态单-单项配置")
    @PreAuthorize("@ss.hasPermi('ai:ai:edit')")
    @Log(title = "AI动态单-单项配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody AiFormConfigField aiFormConfigField) {
        return toAjax(aiFormConfigFieldService.updateAiFormConfigField(aiFormConfigField));
    }

    /**
     * 删除AI动态单-单项配置
     */
    @Operation(summary = "删除AI动态单-单项配置")
    @PreAuthorize("@ss.hasPermi('ai:ai:remove')")
    @Log(title = "AI动态单-单项配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R remove(@PathVariable @Parameter(description = "AI动态单-单项配置主键集合") Long[] ids) {
        return toAjax(aiFormConfigFieldService.deleteAiFormConfigFieldByIds(ids));
    }
}
