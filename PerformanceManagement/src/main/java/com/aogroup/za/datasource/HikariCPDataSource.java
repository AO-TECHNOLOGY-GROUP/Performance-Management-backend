/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.datasource;

import com.co.ke.main.EntryPoint;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author nathan
 */
public class HikariCPDataSource {

    static final String DATABASE_IP = EntryPoint.DATABASE_IP;
    static final String DATABASE_PORT = EntryPoint.DATABASE_PORT;
    static final String DATABASE_NAME = EntryPoint.DATABASE_NAME;
    static final String DATABASE_SERVER_TIME_ZONE = "";
    static final String DATABASE_USER = EntryPoint.DATABASE_USER;
    static final String DATABASE_PASSWORD = EntryPoint.DATABASE_PASSWORD;
    static final String DATABASE_DRIVER_NAME = EntryPoint.DATABASE_DRIVER; 
    
    static final String URL = "jdbc:sqlserver://" + DATABASE_IP + ":" + DATABASE_PORT + ";DatabaseName=" + DATABASE_NAME + "";
    private static final HikariConfig CONFIG = new HikariConfig();
    private static final HikariDataSource DS;

    static final int MAX_POOL_SIZE = 1000;
    static final int MAX_IDLE_TIME = 60000;
    static final int MAX_LIFE_TIME = 1800000;
    static final int MIN_IDLE_TIME = 10000;
    static final int LEAK_DETECTION_THRESHOLD = 5000;
    static final int TIMEOUT_TIME = 30000;

    static {
        CONFIG.setJdbcUrl(URL);
        CONFIG.setDriverClassName(DATABASE_DRIVER_NAME);
        CONFIG.setUsername(DATABASE_USER);
        CONFIG.setPassword(DATABASE_PASSWORD);
        CONFIG.setMaximumPoolSize(MAX_POOL_SIZE);
        CONFIG.setIdleTimeout(MAX_IDLE_TIME);
        CONFIG.setMaxLifetime(MAX_LIFE_TIME);
        CONFIG.setMinimumIdle(5);
        CONFIG.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        DS = new HikariDataSource(CONFIG);
    }

    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }
    
    public static HikariDataSource getDataSource() {
        return DS;
    }

    private HikariCPDataSource() {
    }
}
