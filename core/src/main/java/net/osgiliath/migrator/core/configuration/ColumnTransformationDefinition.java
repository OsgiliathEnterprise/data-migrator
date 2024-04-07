package net.osgiliath.migrator.core.configuration;

/*-
 * #%L
 * data-migrator-core
 * %%
 * Copyright (C) 2024 Osgiliath Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

public class ColumnTransformationDefinition {
    private String columnName;

    // will use the same key each time
    private Boolean consistentKey = Boolean.FALSE;

    private Map<String, String> options = new HashMap<>();

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Map<String, String> getOptions() {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            ret.put(key.replaceFirst("\\d+\\.", ""), entry.getValue());
        }
        return ret;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public Boolean getConsistentKey() {
        return consistentKey;
    }

    public void setConsistentKey(Boolean consistentKey) {
        this.consistentKey = consistentKey;
    }
}
