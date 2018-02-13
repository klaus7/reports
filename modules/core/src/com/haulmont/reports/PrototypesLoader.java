/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports;

import com.haulmont.bali.util.StringHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.reports.app.ParameterPrototype;
import com.haulmont.reports.exception.ReportingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PrototypesLoader {

    private static final Logger log = LoggerFactory.getLogger(PrototypesLoader.class);

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
        if (!security.isEntityOpPermitted(metaClass, EntityOp.READ)) {
            log.debug("reading of " + metaClass + " not permitted, returning empty list");
            return Collections.emptyList();
        }

        Map<String, Object> queryParams = parameterPrototype.getQueryParams();

        View queryView = metadata.getViewRepository().getView(metaClass, parameterPrototype.getViewName());

        Persistence persistence = AppBeans.get(Persistence.class);
        Transaction tx = persistence.createTransaction();

        EntityManager entityManager = persistence.getEntityManager();
        Query query = entityManager.createQuery(parameterPrototype.getQueryString());

        boolean constraintsApplied = security.applyConstraints(query);
        if (constraintsApplied)
            log.debug("Constraints applied: " + printQuery(query.getQueryString()));

        query.setView(queryView);
        if (queryParams != null) {
            for (Map.Entry<String, Object> queryParamEntry : queryParams.entrySet()) {
                query.setParameter(queryParamEntry.getKey(), queryParamEntry.getValue());
            }
        }

        query.setFirstResult(parameterPrototype.getFirstResult());

        if (parameterPrototype.getMaxResults() != null && !parameterPrototype.getMaxResults().equals(0)) {
            query.setMaxResults(parameterPrototype.getMaxResults());
        } else {
            Configuration configuration = AppBeans.get(Configuration.NAME);
            ReportingConfig config = configuration.getConfig(ReportingConfig.class);
            query.setMaxResults(config.getParameterPrototypeQueryLimit());
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
        else
            return StringHelper.removeExtraSpaces(query.replace("\n", " "));
    }
}