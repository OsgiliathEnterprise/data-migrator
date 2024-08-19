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

public class PerDSJpaProperties {


    /**
     * Additional native properties to set on the JPA provider.
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Name of the target database to operate on, auto-detected by default. Can be
     * alternatively set using the "Database" enum.
     */
    private String databasePlatform;
    /**
     * Whether to initialize the schema on startup.
     */
    private boolean generateDdl = false;

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getDatabasePlatform() {
        return databasePlatform;
    }

    public void setDatabasePlatform(String databasePlatform) {
        this.databasePlatform = databasePlatform;
    }

    public boolean isGenerateDdl() {
        return generateDdl;
    }

    public void setGenerateDdl(boolean generateDdl) {
        this.generateDdl = generateDdl;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> ret = this.properties;
        if (this.generateDdl) {
            ret.put("hibernate.hbm2ddl.auto", "create");
        } else {
            ret.put("hibernate.hbm2ddl.auto", "update");
        }
        return this.properties;
    }


}
