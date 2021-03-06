/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.reports.entity.wizard;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.reports.entity.*;
import com.haulmont.reports.entity.charts.ChartType;

import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@MetaClass(name = "report$WizardReportData")
@SystemLevel
public class ReportData extends BaseUuidEntity {

    private static final long serialVersionUID = -1649648403032678085L;

    public static class Parameter implements Serializable {
        public final String name;
        public final Class javaClass;
        public final ParameterType parameterType;
        public final String defaultValue;
        public final PredefinedTransformation predefinedTransformation;
        public final Boolean hidden;

        public Parameter(String name, Class javaClass, ParameterType parameterType, String defaultValue, Boolean hidden) {
            this(name, javaClass, parameterType, defaultValue, null, hidden);
        }

        public Parameter(String name, Class javaClass, ParameterType parameterType, String defaultValue, PredefinedTransformation transformation,
                         Boolean hidden) {
            this.name = name;
            this.javaClass = javaClass;
            this.parameterType = parameterType;
            this.defaultValue = defaultValue;
            this.predefinedTransformation = transformation;
            this.hidden = hidden;
        }
    }

    public enum ReportType {
        SINGLE_ENTITY(false, true),
        LIST_OF_ENTITIES(true, true),
        LIST_OF_ENTITIES_WITH_QUERY(true, false);

        private boolean list;
        private boolean entity;

        ReportType(boolean list, boolean entity) {
            this.list = list;
            this.entity = entity;
        }

        public boolean isList() {
            return list;
        }

        public boolean isEntity() {
            return entity;
        }
    }

    @MetaProperty
    @Transient
    protected String name;

    @MetaProperty
    @Transient
    protected EntityTreeNode entityTreeRootNode;

    @MetaProperty
    @Transient
    protected Report generatedReport;

    @MetaProperty
    @Transient
    protected ReportGroup group;

    @MetaProperty
    @Transient
    protected ReportType reportType;

    @MetaProperty
    @Transient
    protected String templateFileName;

    @MetaProperty
    @Transient
    protected String outputNamePattern;

    @MetaProperty
    @Transient
    protected ReportOutputType outputFileType;

    @MetaProperty
    @Composition
    @Transient
    @OneToMany(targetEntity = RegionProperty.class)
    protected List<ReportRegion> reportRegions = new ArrayList<>();

    @Transient
    protected String query;

    @Transient
    protected List<Parameter> queryParameters;

    @Transient
    protected String dataStore;

    @Transient
    protected TemplateFileType templateFileType;

    @Transient
    protected byte[] templateContent;

    @Transient
    protected ChartType chartType = ChartType.SERIAL;

    public Report getGeneratedReport() {
        return generatedReport;
    }

    public void setGeneratedReport(Report generatedReport) {
        this.generatedReport = generatedReport;
    }

    public ReportGroup getGroup() {
        return group;
    }

    public void setGroup(ReportGroup group) {
        this.group = group;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getTemplateFileName() {
        return templateFileName;
    }

    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    public ReportOutputType getOutputFileType() {
        return outputFileType;
    }

    public void setOutputFileType(ReportOutputType outputFileType) {
        this.outputFileType = outputFileType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityTreeNode getEntityTreeRootNode() {
        return entityTreeRootNode;
    }

    public void setEntityTreeRootNode(EntityTreeNode entityTreeRootNode) {
        this.entityTreeRootNode = entityTreeRootNode;
    }

    public List<ReportRegion> getReportRegions() {
        return reportRegions;
    }

    public void setReportRegions(List<ReportRegion> reportRegions) {
        this.reportRegions = reportRegions;
    }

    @Transient
    public ReportData addRegion(ReportRegion region) {
        reportRegions.add(region);
        return this;
    }

    @Transient
    public ReportData addRegion(int index, ReportRegion region) {
        reportRegions.add(index, region);
        return this;
    }

    public String getOutputNamePattern() {
        return outputNamePattern;
    }

    public void setOutputNamePattern(String outputNamePattern) {
        this.outputNamePattern = outputNamePattern;
    }

    @Transient
    public void removeRegion(int index) {
        reportRegions.remove(index);
    }

    @Transient
    public void clearRegions() {
        reportRegions.clear();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDataStore() {
        return dataStore;
    }

    public void setDataStore(String dataStore) {
        this.dataStore = dataStore;
    }

    public List<Parameter> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(List<Parameter> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public TemplateFileType getTemplateFileType() {
        return templateFileType;
    }

    public void setTemplateFileType(TemplateFileType templateFileType) {
        this.templateFileType = templateFileType;
    }

    public byte[] getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(byte[] templateContent) {
        this.templateContent = templateContent;
    }

    public ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
    }
}
