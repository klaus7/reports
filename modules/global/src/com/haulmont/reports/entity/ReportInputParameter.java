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
package com.haulmont.reports.entity;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.entity.LocaleHelper;
import com.haulmont.yarg.structure.ReportParameterWithDefaultValue;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Transient;

@MetaClass(name = "report$ReportInputParameter")
@SystemLevel
@NamePattern("%s|locName")
@SuppressWarnings("unused")
public class ReportInputParameter extends BaseUuidEntity implements ReportParameterWithDefaultValue {

    private static final long serialVersionUID = 6231014880104406246L;

    @MetaProperty
    protected Report report;

    @MetaProperty
    protected Integer type = ParameterType.TEXT.getId();

    @MetaProperty
    protected String name;

    @MetaProperty
    protected String localeNames;

    @MetaProperty
    protected String alias;

    @MetaProperty
    protected Integer position;

    @MetaProperty
    protected String entityMetaClass;

    @MetaProperty
    protected Boolean lookup = false;

    @MetaProperty
    protected String lookupJoin;

    @MetaProperty
    protected String lookupWhere;

    @MetaProperty
    protected String enumerationClass;

    @MetaProperty
    protected String screen;

    @MetaProperty
    protected Boolean required = false;

    @MetaProperty
    protected String defaultValue;

    @MetaProperty
    protected String parameterClassName;

    @MetaProperty
    protected String transformationScript;

    @MetaProperty
    protected String validationScript;

    @MetaProperty
    protected Boolean validationOn = false;

    @MetaProperty
    protected Integer predefinedTransformation;

    @MetaProperty
    protected Boolean hidden;

    @MetaProperty
    protected Boolean defaultDateIsCurrent = false;

    @Transient
    protected String localeName;

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public ParameterType getType() {
        return type != null ? ParameterType.fromId(type) : null;
    }

    public void setType(ParameterType type) {
        this.type = type != null ? type.getId() : null;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (ObjectUtils.notEqual(name, this.name)) {
            localeName = null;
        }
        this.name = name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getEntityMetaClass() {
        return entityMetaClass;
    }

    public Boolean getLookup() {
        return lookup;
    }

    public void setLookup(Boolean lookup) {
        this.lookup = lookup;
    }

    public String getLookupJoin() {
        return lookupJoin;
    }

    public void setLookupJoin(String lookupJoin) {
        this.lookupJoin = lookupJoin;
    }

    public String getLookupWhere() {
        return lookupWhere;
    }

    public void setLookupWhere(String lookupWhere) {
        this.lookupWhere = lookupWhere;
    }

    public void setEntityMetaClass(String entityMetaClass) {
        this.entityMetaClass = entityMetaClass;
    }

    public String getEnumerationClass() {
        return enumerationClass;
    }

    public void setEnumerationClass(String enumerationClass) {
        this.enumerationClass = enumerationClass;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public String getLocaleNames() {
        return localeNames;
    }

    public void setLocaleNames(String localeNames) {
        if (ObjectUtils.notEqual(localeNames, this.localeNames)) {
            localeName = null;
        }
        this.localeNames = localeNames;
    }

    @MetaProperty
    public String getLocName() {
        if (localeName == null) {
            localeName = LocaleHelper.getLocalizedName(localeNames);
            if (localeName == null)
                localeName = name;
        }
        return localeName;
    }

    @Override
    public Class getParameterClass() {
        try {
            if (StringUtils.isNotBlank(parameterClassName)) {
                return Class.forName(parameterClassName);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("An error occurred while resolving class name " + parameterClassName, e);
        }

        return null;
    }

    public void setParameterClass(Class parameterClass) {
        if (parameterClass != null) {
            parameterClassName = parameterClass.getName();
        }
    }

    public String getParameterClassName() {
        return parameterClassName;
    }

    public void setParameterClassName(String parameterClassName) {
        this.parameterClassName = parameterClassName;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getTransformationScript() {
        return transformationScript;
    }

    public void setTransformationScript(String transformationScript) {
        this.transformationScript = transformationScript;
    }

    public PredefinedTransformation getPredefinedTransformation() {
        return predefinedTransformation != null ? PredefinedTransformation.fromId(predefinedTransformation) : null;
    }

    public void setPredefinedTransformation(PredefinedTransformation predefinedTransformation) {
        this.predefinedTransformation = predefinedTransformation != null ? predefinedTransformation.getId() : null;
    }

    public String getValidationScript() {
        return validationScript;
    }

    public void setValidationScript(String validationScript) {
        this.validationScript = validationScript;
    }

    public Boolean getValidationOn() {
        return validationOn;
    }

    public void setValidationOn(Boolean validationOn) {
        this.validationOn = validationOn;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Boolean getDefaultDateIsCurrent() {
        return defaultDateIsCurrent;
    }

    public void setDefaultDateIsCurrent(Boolean defaultDateIsCurrent) {
        this.defaultDateIsCurrent = defaultDateIsCurrent;
    }
}