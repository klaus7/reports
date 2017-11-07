/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.exception;

import com.haulmont.cuba.core.global.SupportedByClient;

@SupportedByClient
public class ReportingException extends RuntimeException {

    public ReportingException() {
    }

    public ReportingException(String message) {
        super(message);
    }
       
    public ReportingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportingException(Throwable cause) {
        super(cause);
    }
}
