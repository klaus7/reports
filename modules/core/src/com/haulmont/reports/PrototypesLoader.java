/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.reports;

import com.haulmont.bali.util.StringHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.global.*;
import com.haulmont.reports.app.ParameterPrototype;
import com.haulmont.reports.exception.ReportingException;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author artamonov
 * @version $Id$
 */
public class PrototypesLoader {

    private Log log = LogFactory.getLog(PrototypesLoader.class);

    /**
     * Load parameter data
     *
     * @param parameterPrototype Parameter prototype
     * @return Entities list
     */
    public List loadData(ParameterPrototype parameterPrototype) {
        Metadata metadata = AppBeans.get(Metadata.class);

        MetaClass metaClass = metadata.getSession().getClass(parameterPrototype.getMetaClassName());

        PersistenceSecurity security = AppBeans.get(PersistenceSecurity.NAME);
        if (parameterPrototype.isUseSecurityConstraints()) {
            if (!security.isEntityOpPermitted(metaClass, EntityOp.READ)) {
                log.debug("reading of " + metaClass + " not permitted, returning empty list");
                return Collections.emptyList();
            }
        }

        Map<String, Object> queryParams = parameterPrototype.getQueryParams();

        View queryView = metadata.getViewRepository().getView(metaClass, parameterPrototype.getViewName());

        Persistence persistence = AppBeans.get(Persistence.class);
        Transaction tx = persistence.createTransaction();

        EntityManager entityManager = persistence.getEntityManager();
        Query query = entityManager.createQuery(parameterPrototype.getQueryString());

        if (parameterPrototype.isUseSecurityConstraints()) {
            boolean constraintsApplied = security.applyConstraints(query, metaClass.getName());
            if (constraintsApplied)
                log.debug("Constraints applyed: " + printQuery(query.getQueryString()));
        }

        query.setView(queryView);
        if (queryParams != null) {
            for (Map.Entry<String, Object> queryParamEntry : queryParams.entrySet()) {
                query.setParameter(queryParamEntry.getKey(), queryParamEntry.getValue());
            }
        }

        List queryResult;
        try {
            queryResult = query.getResultList();
            tx.commit();
        } catch (Exception e) {
            throw new ReportingException(e);
        } finally {
            tx.end();
        }

        return queryResult;
    }

    private String printQuery(String query) {
        if (query == null)
            return null;

        String str = StringHelper.removeExtraSpaces(query.replace("\n", " "));

        if (AppBeans.get(Configuration.class).getConfig(ServerConfig.class).getCutLoadListQueries()) {
            str = StringUtils.abbreviate(str.replaceAll("[\\n\\r]", " "), 50);
        }

        return str;
    }
}