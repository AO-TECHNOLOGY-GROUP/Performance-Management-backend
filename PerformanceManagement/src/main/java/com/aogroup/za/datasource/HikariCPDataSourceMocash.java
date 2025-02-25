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
 * @author rkipkirui
 */
public class HikariCPDataSourceMocash {

    static final String MOCASH_DATABASE_IP = EntryPoint.MOCASH_DATABASE_IP;
    static final String DATABASE_PORT = EntryPoint.DATABASE_PORT;
    static final String MOCASH_DATABASE_NAME = EntryPoint.MOCASH_DATABASE_NAME;
    static final String DATABASE_SERVER_TIME_ZONE = "";
    static final String MOCASH_DATABASE_USER = EntryPoint.MOCASH_DATABASE_USER;
    static final String MOCASH_DATABASE_PASSWORD = EntryPoint.MOCASH_DATABASE_PASSWORD;
    static final String DATABASE_DRIVER_NAME = EntryPoint.DATABASE_DRIVER; 
    
    static final String URL = "jdbc:sqlserver://" + MOCASH_DATABASE_IP + ":" + DATABASE_PORT + ";DatabaseName=" + MOCASH_DATABASE_NAME + "";
    private static final HikariConfig CONFIG = new HikariConfig();
    private static final HikariDataSource DS;

    static final int MAX_POOL_SIZE = 1000;
    static final int MAX_IDLE_TIME = 600000;
    static final int MAX_LIFE_TIME = 28800000;
    static final int MIN_IDLE_TIME = 10000;
    static final int LEAK_DETECTION_THRESHOLD = 2000;
    static final int TIMEOUT_TIME = 25000;

    static {
        CONFIG.setJdbcUrl(URL);
        CONFIG.setDriverClassName(DATABASE_DRIVER_NAME);
        CONFIG.setUsername(MOCASH_DATABASE_USER);
        CONFIG.setPassword(MOCASH_DATABASE_PASSWORD);
        CONFIG.setMaximumPoolSize(MAX_POOL_SIZE);
        CONFIG.setIdleTimeout(MAX_IDLE_TIME);
        CONFIG.setMaxLifetime(MAX_LIFE_TIME);
       // CONFIG.setMinimumIdle(MIN_IDLE_TIME);
        CONFIG.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        DS = new HikariDataSource(CONFIG);
    }

    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    private HikariCPDataSourceMocash() {
    }
}
