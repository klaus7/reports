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

package com.haulmont.reports.gui.report.run;

import com.google.common.collect.ImmutableMap;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.reports.entity.ParameterType;
import com.haulmont.reports.entity.ReportInputParameter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

@Component(ParameterClassResolver.NAME)
public class ParameterClassResolver {

    public static final String NAME = "report_ParameterClassResolver";

    protected Map<ParameterType, Class> primitiveParameterTypeMapping = new ImmutableMap.Builder<ParameterType, Class>()
            .put(ParameterType.BOOLEAN, Boolean.class)
            .put(ParameterType.DATE, Date.class)
            .put(ParameterType.DATETIME, Date.class)
            .put(ParameterType.TEXT, String.class)
            .put(ParameterType.NUMERIC, Double.class)
            .put(ParameterType.TIME, Date.class)
            .build();

    protected Map<Class, ParameterType> primitiveParameterClassMapping = new ImmutableMap.Builder<Class, ParameterType>()
            .put(Boolean.class, ParameterType.BOOLEAN)
            .put(Date.class, ParameterType.DATE)
            .put(String.class, ParameterType.TEXT)
            .put(Double.class, ParameterType.NUMERIC)
            .put(Integer.class, ParameterType.NUMERIC)
            .put(BigDecimal.class, ParameterType.NUMERIC)
            .put(Float.class, ParameterType.NUMERIC)
            .put(Long.class, ParameterType.NUMERIC)
            .build();

    @Inject
    protected Scripting scripting;

    @Inject
    protected Metadata metadata;

    @Nullable
    public Class resolveClass(ReportInputParameter parameter) {
        Class aClass = primitiveParameterTypeMapping.get(parameter.getType());
        if (aClass == null) {
            if (parameter.getType() == ParameterType.ENTITY || parameter.getType() == ParameterType.ENTITY_LIST) {
                MetaClass metaClass = metadata.getSession().getClass(parameter.getEntityMetaClass());
                if (metaClass != null) {
                    return metaClass.getJavaClass();
                } else {
                    return null;
                }
            } else if (parameter.getType() == ParameterType.ENUMERATION) {
                if (StringUtils.isNotBlank(parameter.getEnumerationClass())) {
                    return scripting.loadClass(parameter.getEnumerationClass());
                }
            }
        }

        return aClass;
    }

    @Nullable
    public ParameterType resolveParameterType(@Nullable Class parameterClass) {
        if (parameterClass == null) {
            return null;
        }

        ParameterType parameterType = primitiveParameterClassMapping.get(parameterClass);
        if (parameterType != null) {
            return parameterType;
        } else {
            if (Entity.class.isAssignableFrom(parameterClass)) {
                return ParameterType.ENTITY;
            } else if (Collection.class.isAssignableFrom(parameterClass)) {
                return ParameterType.ENTITY_LIST;
            } else if (Enum.class.isAssignableFrom(parameterClass)) {
                return ParameterType.ENUMERATION;
            }
        }

        return null;
    }
}
