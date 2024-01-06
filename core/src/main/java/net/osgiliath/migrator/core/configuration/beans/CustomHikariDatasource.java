package net.osgiliath.migrator.core.configuration.beans;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.XAConnection;
import javax.sql.XAConnectionBuilder;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class CustomHikariDatasource extends HikariDataSource {

    public CustomHikariDatasource() {
        super();
    }

    public CustomHikariDatasource(HikariConfig configuration) {
        super(configuration);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return super.getConnection();
    }

}
