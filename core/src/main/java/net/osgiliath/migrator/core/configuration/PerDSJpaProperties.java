package net.osgiliath.migrator.core.configuration;

import java.util.HashMap;
import java.util.Map;

public class PerDSJpaProperties {


	/**
	 * Additional native properties to set on the JPA provider.
	 */
	private Map<String, String> properties = new HashMap<>();

    	/**
	 * Name of the target database to operate on, auto-detected by default. Can be
	 * alternatively set using the "Database" enum.
	 */
	private String databasePlatform;
    	/**
	 * Whether to initialize the schema on startup.
	 */
	private boolean generateDdl = false;

    public void setProperties(Map<String, String> properties) {
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

    public Map<String, String> getProperties() {

        Map<String, String> ret = this.properties;
        if (this.generateDdl) {
            ret.put("hibernate.hbm2ddl.auto", "create");
        }
        return this.properties;
	}


}
