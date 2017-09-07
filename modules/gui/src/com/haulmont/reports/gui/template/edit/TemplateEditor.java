/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.gui.template.edit;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.reports.app.service.ReportService;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.entity.ReportOutputType;
import com.haulmont.reports.entity.ReportTemplate;
import com.haulmont.reports.entity.charts.AbstractChartDescription;
import com.haulmont.reports.entity.charts.ChartSeries;
import com.haulmont.reports.entity.charts.ChartType;
import com.haulmont.reports.entity.charts.SerialChartDescription;
import com.haulmont.reports.gui.ReportPrintHelper;
import com.haulmont.reports.gui.datasource.NotPersistenceDatasource;
import com.haulmont.reports.gui.report.run.ShowChartController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TemplateEditor extends AbstractEditor<ReportTemplate> {
    @Inject
    protected Label templateFileLabel;

    @Inject
    protected FileUploadField templateUploadField;

    @Inject
    protected TextField customDefinition;

    @Inject
    protected Label customDefinitionLabel;

    @Inject
    protected LookupField customDefinedBy;

    @Inject
    protected Label customDefinedByLabel;

    @Inject
    protected FileUploadingAPI fileUploading;

    @Inject
    protected LookupField outputType;

    @Inject
    protected TextField outputNamePattern;

    @Inject
    protected Label outputNamePatternLabel;

    @Inject
    protected ChartEditFrameController chartEdit;

    @Inject
    protected NotPersistenceDatasource<ReportTemplate> templateDs;

    @Inject
    protected BoxLayout chartEditBox;

    @Inject
    protected BoxLayout chartPreviewBox;

    @Inject
    protected SourceCodeEditor sourceCodeEditor;
    @Inject
    protected CheckBox highlightActiveLineCheck;
    @Inject
    protected CheckBox printMarginCheck;
    @Inject
    protected CheckBox showGutterCheck;
    @Inject
    protected VBoxLayout templateEditorBox;

    @Inject
    protected WindowConfig windowConfig;
    @Inject
    private Metadata metadata;

    public TemplateEditor() {
        showSaveNotification = false;
    }

    @Override
    protected void initNewItem(ReportTemplate template) {
        if (StringUtils.isEmpty(template.getCode())) {
            Report report = template.getReport();
            if (report != null) {
                if ((report.getTemplates() == null) || (report.getTemplates().size() == 0)) {
                    template.setCode(ReportService.DEFAULT_TEMPLATE_CODE);
                } else
                    template.setCode("Template_" + Integer.toString(report.getTemplates().size()));
            }
        }
    }

    @Override
    protected void postInit() {
        super.postInit();

        initUploadField(getItem().getContent());

        ReportTemplate reportTemplate = getItem();
        templateDs.addItemPropertyChangeListener(e -> {
            if ("reportOutputType".equals(e.getProperty())) {
                setupVisibility(reportTemplate.getCustom(), (ReportOutputType) e.getValue());
            } else if ("custom".equals(e.getProperty())) {
                setupVisibility(Boolean.TRUE.equals(e.getValue()), reportTemplate.getReportOutputType());
            }
        });

        chartEdit.setBands(reportTemplate.getReport().getBands());
        if (reportTemplate.getReportOutputType() == ReportOutputType.CHART) {
            chartEdit.setChartDescription(reportTemplate.getChartDescription());
        }

        ArrayList<ReportOutputType> outputTypes = new ArrayList<>(Arrays.asList(ReportOutputType.values()));

        if (!windowConfig.hasWindow(ShowChartController.JSON_CHART_SCREEN_ID)) {
            outputTypes.remove(ReportOutputType.CHART);
        }

        outputType.setOptionsList(outputTypes);
    }

    protected void initUploadField(byte[] templateFile) {
        if (templateFile != null) {
            templateUploadField.setContentProvider(() ->
                    new ByteArrayInputStream(templateFile));
            FileDescriptor fileDescriptor = metadata.create(FileDescriptor.class);
            fileDescriptor.setName(getItem().getName());
            templateUploadField.setValue(fileDescriptor);
        }
    }

    @Override
    public void ready() {
        super.ready();

        ReportTemplate reportTemplate = getItem();
        setupVisibility(reportTemplate.getCustom(), reportTemplate.getReportOutputType());

        initTemplateEditor();
    }

    protected void setupVisibility(boolean customEnabled, ReportOutputType reportOutputType) {
        templateUploadField.setVisible(!customEnabled);
        customDefinedBy.setVisible(customEnabled);
        customDefinition.setVisible(customEnabled);
        customDefinedByLabel.setVisible(customEnabled);
        customDefinitionLabel.setVisible(customEnabled);

        customDefinedBy.setRequired(customEnabled);
        customDefinedBy.setRequiredMessage(getMessage("templateEditor.customDefinedBy"));
        customDefinition.setRequired(customEnabled);
        customDefinition.setRequiredMessage(getMessage("templateEditor.classRequired"));

        boolean chartOutputType = reportOutputType == ReportOutputType.CHART;
        chartEditBox.setVisible(chartOutputType && !customEnabled);
        chartPreviewBox.setVisible(chartOutputType && !customEnabled);
        if (chartOutputType && !customEnabled) {
            chartEdit.showChartPreviewBox();
        } else {
            chartEdit.hideChartPreviewBox();
        }

        boolean tableOutputType = reportOutputType == ReportOutputType.TABLE;
        boolean templateOutputVisibility = !(chartOutputType || tableOutputType);

        templateUploadField.setVisible(templateOutputVisibility);
        templateFileLabel.setVisible(templateOutputVisibility);
        outputNamePattern.setVisible(templateOutputVisibility);
        outputNamePatternLabel.setVisible(templateOutputVisibility);
    }

    @Override
    @SuppressWarnings({"serial", "unchecked"})
    public void init(Map<String, Object> params) {
        super.init(params);

        getDialogOptions()
                .setWidthAuto()
                .setResizable(true);

        templateUploadField.addFileUploadErrorListener(e ->
                showNotification(getMessage("templateEditor.uploadUnsuccess"), NotificationType.WARNING));

        templateUploadField.addFileUploadSucceedListener(e -> {
            String fileName = templateUploadField.getFileName();
            getItem().setName(fileName);

            saveTemplateContentFromFile();
            updateOutputType();

            showNotification(getMessage("templateEditor.uploadSuccess"), NotificationType.TRAY);
        });
    }

    protected void updateOutputType() {
        setupVisibility(getItem().isCustom(), getItem().getReportOutputType());

        if (outputType.getValue() == null) {
            String extension = FilenameUtils.getExtension(templateUploadField.getFileDescriptor().getName()).toUpperCase();
            ReportOutputType reportOutputType = ReportOutputType.getTypeFromExtension(extension);
            if (reportOutputType != null) {
                outputType.setValue(reportOutputType);
            }
        }
    }

    protected void saveTemplateContentFromFile() {
        File file = fileUploading.getFile(templateUploadField.getFileId());
        try {
            byte[] data = FileUtils.readFileToByteArray(file);
            getItem().setContent(data);
            initEditorValue();
        } catch (IOException ex) {
            throw new RuntimeException(
                    String.format("An error occurred while uploading file for template [%s]", getItem().getCode()), ex);
        }
    }

    protected void initTemplateEditor() {
        String extension = FilenameUtils.getExtension(templateUploadField.getFileName());
        if (extension != null && (extension.toLowerCase().equals("csv") || extension.toLowerCase().equals("html"))) {
            templateEditorBox.setVisible(true);
            initEditorValue();
            initSourceCodeControls();
        } else {
            templateEditorBox.setVisible(false);
        }
    }

    protected void initEditorValue() {
        String templateContent = new String(getItem().getContent(), StandardCharsets.UTF_8);
        sourceCodeEditor.setMode(SourceCodeEditor.Mode.HTML);
        sourceCodeEditor.setValue(templateContent);
    }

    protected void initSourceCodeControls() {
        highlightActiveLineCheck.setValue(sourceCodeEditor.isHighlightActiveLine());
        highlightActiveLineCheck.addValueChangeListener(e -> sourceCodeEditor.setHighlightActiveLine(Boolean.TRUE.equals(e.getValue())));

        printMarginCheck.setValue(sourceCodeEditor.isShowPrintMargin());
        printMarginCheck.addValueChangeListener(e -> sourceCodeEditor.setShowPrintMargin(Boolean.TRUE.equals(e.getValue())));

        showGutterCheck.setValue(sourceCodeEditor.isShowGutter());
        showGutterCheck.addValueChangeListener(e -> sourceCodeEditor.setShowGutter(Boolean.TRUE.equals(e.getValue())));
    }

    @Override
    public boolean commit(boolean validate) {
        if (!validateTemplateFile() || !validateInputOutputFormats()) {
            return false;
        }
        ReportTemplate reportTemplate = getItem();
        if (reportTemplate.getReportOutputType() == ReportOutputType.CHART) {
            if (!validateChart()) {
                return false;
            }
            AbstractChartDescription chartDescription = chartEdit.getChartDescription();
            reportTemplate.setChartDescription(chartDescription);
        }
        if (reportTemplate.getReportOutputType() == ReportOutputType.TABLE) {
            reportTemplate.setTableName();
        }

        String extension = FilenameUtils.getExtension(templateUploadField.getFileName());
        if (extension != null && (extension.toLowerCase().equals("csv") || extension.toLowerCase().equals("html"))) {
            getItem().setContent(sourceCodeEditor.getValue().getBytes());
            initUploadField(sourceCodeEditor.getValue().getBytes());
        }

        return super.commit(validate);
    }

    protected boolean validateInputOutputFormats() {
        ReportTemplate reportTemplate = getItem();
        String name = reportTemplate.getName();
        if (!Boolean.TRUE.equals(reportTemplate.getCustom())
                && reportTemplate.getReportOutputType() != ReportOutputType.CHART
                && reportTemplate.getReportOutputType() != ReportOutputType.TABLE
                && name != null) {
            String inputType = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";

            ReportOutputType outputTypeValue = outputType.getValue();
            if (!ReportPrintHelper.getInputOutputTypesMapping().containsKey(inputType) ||
                    !ReportPrintHelper.getInputOutputTypesMapping().get(inputType).contains(outputTypeValue)) {
                showNotification(getMessage("inputOutputTypesError"), NotificationType.TRAY);
                return false;
            }
        }
        return true;
    }

    protected boolean validateTemplateFile() {
        ReportTemplate template = getItem();
        if (!Boolean.TRUE.equals(template.getCustom())
                && template.getReportOutputType() != ReportOutputType.CHART
                && template.getReportOutputType() != ReportOutputType.TABLE
                && template.getContent() == null) {
            StringBuilder notification = new StringBuilder(getMessage("template.uploadTemplate"));

            if (StringUtils.isEmpty(template.getCode())) {
                notification.append("\n").append(getMessage("template.codeMsg"));
            }

            if (template.getOutputType() == null) {
                notification.append("\n").append(getMessage("template.outputTypeMsg"));
            }

            showNotification(getMessage("validationFail.caption"),
                    notification.toString(), NotificationType.TRAY);

            return false;
        }
        return true;
    }

    protected boolean validateChart() {
        AbstractChartDescription chartDescription = chartEdit.getChartDescription();
        if (chartDescription.getType() == ChartType.SERIAL) {
            List<ChartSeries> series = ((SerialChartDescription) chartDescription).getSeries();
            if (series == null || series.size() == 0) {
                showNotification(getMessage("validationFail.caption"),
                        getMessage("chartEdit.seriesEmptyMsg"), NotificationType.TRAY);
                return false;
            }
            for (ChartSeries it : series) {
                if (it.getType() == null) {
                    showNotification(getMessage("validationFail.caption"),
                            getMessage("chartEdit.seriesTypeNullMsg"), NotificationType.TRAY);
                    return false;
                }
                if (it.getValueField() == null) {
                    showNotification(getMessage("validationFail.caption"),
                            getMessage("chartEdit.seriesValueFieldNullMsg"), NotificationType.TRAY);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void commitAndClose() {
        if (!validateTemplateFile()) {
            return;
        }

        if (!getItem().getCustom()) {
            getItem().setCustomDefinition("");
        }
        if (commit(true)) {
            close(COMMIT_ACTION_ID);
        }
    }
}