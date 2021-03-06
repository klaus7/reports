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
package com.haulmont.reports.app;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.View;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class EntityMap implements Map<String, Object> {
    private static final Logger log = LoggerFactory.getLogger(EntityMap.class);

    public static final String INSTANCE_NAME_KEY = "_instanceName";

    protected Instance instance;
    protected View view;
    protected HashMap<String, Object> explicitData;

    protected boolean loaded = false;

    public EntityMap(Entity entity) {
        this.instance = entity;
        this.explicitData = new HashMap<>();
    }

    public EntityMap(Entity entity, View loadedAttributes) {
        this(entity);
        view = loadedAttributes;
    }

    @Override
    public int size() {
        return explicitData.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        if (explicitData.containsKey(key)) {
            return true;
        } else {
            MetaClass metaClass = instance.getMetaClass();
            for (MetaProperty property : metaClass.getProperties()) {
                if (Objects.equals(property.getName(), key))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        loadAllProperties();
        return explicitData.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        Object value = getValue(instance, key);

        if (value != null) return value;

        return explicitData.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return explicitData.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return explicitData.remove(key);
    }

    @Override
    public void putAll(@Nonnull Map<? extends String, ?> m) {
        explicitData.putAll(m);
    }

    @Override
    public void clear() {
        explicitData.clear();
    }

    @Nonnull
    @Override
    public Set<String> keySet() {
        loadAllProperties();
        return explicitData.keySet();
    }

    @Nonnull
    @Override
    public Collection<Object> values() {
        loadAllProperties();
        return explicitData.values();
    }

    @Nonnull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        loadAllProperties();
        return explicitData.entrySet();
    }

    protected void loadAllProperties() {
        Metadata metadata = AppBeans.get(Metadata.class);

        if (!loaded) {
            MetaClass metaClass = instance.getMetaClass();
            String pkName = metadata.getTools().getPrimaryKeyName(metaClass);
            for (MetaProperty property : metaClass.getProperties()) {
                if (view != null && view.getProperty(property.getName()) != null) {
                    explicitData.put(property.getName(), getValue(instance, property.getName()));
                } else if (view != null && Objects.equals(pkName, property.getName())) {
                    explicitData.put(property.getName(), getValue(instance, property.getName()));
                } else if (view == null) {
                    explicitData.put(property.getName(), getValue(instance, property.getName()));
                }
            }

            if (instanceNameAvailable(metaClass))
                explicitData.put(INSTANCE_NAME_KEY, metadata.getTools().getInstanceName(instance));
            else
                explicitData.put(INSTANCE_NAME_KEY, null);

            loaded = true;
        }
    }

    protected boolean instanceNameAvailable(MetaClass metaClass) {
        MetadataTools metadataTools = AppBeans.get(MetadataTools.NAME);
        MetadataTools.NamePatternRec namePattern = metadataTools.parseNamePattern(metaClass);
        String[] fields = new String[0];
        if (namePattern != null) {
            fields = namePattern.fields;
        }

        for (String field : fields) {
            Object value = getValue(instance, field);
            if (value == null)
                return false;
        }

        return true;
    }

    protected Object getValue(Instance instance, Object key) {
        if (key == null) {
            return null;
        }

        MetadataTools metadataTools = AppBeans.get(MetadataTools.NAME);

        String path = String.valueOf(key);
        if (path.endsWith(INSTANCE_NAME_KEY)) {
            if (StringUtils.isNotBlank(path.replace(INSTANCE_NAME_KEY, ""))) {
                Object value = getValue(instance, path.replace("." + INSTANCE_NAME_KEY, ""));
                if (value instanceof Instance) {
                    return metadataTools.getInstanceName((Instance) value);
                }
            } else {
                try {
                    return metadataTools.getInstanceName(instance);
                } catch (Exception e) {
                    log.trace("Suppressed error from underlying EntityMap instance.getInstanceName", e);
                    return null;
                }
            }
        }

        Object value = null;
        try {
            value = instance.getValue(path);
        } catch (Exception e) {
            log.trace("Suppressed error from underlying EntityMap instance.getValue", e);
        }

        if (value == null) {
            try {
                value = instance.getValueEx(path);
            } catch (Exception e) {
                log.trace("Suppressed error from underlying EntityMap instance.getValue", e);
            }
        }
        return value;
    }

    public Instance getInstance() {
        return instance;
    }

    public View getView() {
        return view;
    }
}