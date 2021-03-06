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

package com.haulmont.reports.gui.report.validators;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.ValidationException;
import org.apache.commons.lang3.StringUtils;

public class ReportParamAliasValidator implements Field.Validator {
    protected Messages messages = AppBeans.get(Messages.class);

    @Override
    public void validate(Object value) throws ValidationException {
        String stringValue = (String) value;

        if (StringUtils.isNotEmpty(stringValue)) {
            if (!stringValue.matches("[\\w]*")) {
                String incorrectParamName = messages.getMessage(ReportParamAliasValidator.class, "incorrectParamAlias");
                throw new ValidationException(incorrectParamName);
            }

            if (stringValue.equals("_")) {
                String incorrectParamName = messages.getMessage(ReportParamAliasValidator.class, "notOnlyUnderscore");
                throw new ValidationException(incorrectParamName);
            }
        }
    }
}
