/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datasource.agency;

/**
 *
 * @author rkipkirui
 */
import com.co.ke.main.EntryPoint;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import log.Logging;

/**
 *
 * @author rkipkirui
 */
public final class AgencyDBConnection {

    String DATABASE_IP = "";
    String DATABASE_PORT = "";
    String DATABASE_NAME = "";
    String DATABASE_SERVER_TIME_ZONE = "";

    String url = "";
    String driverName = EntryPoint.DATABASE_DRIVER;

    Statement stmt;
    ResultSet rs;

    private Logging logger;

    Connection con = null;
    // private static Logging logger;

    public AgencyDBConnection() {
        logger = new Logging();
        stmt = null;
        rs = null;
        url = "jdbc:sqlserver://" + DATABASE_IP + ":" + DATABASE_PORT + ";DatabaseName=" + DATABASE_NAME + ";encrypt=false; trustServerCertificate=false;";
    }

    public Connection getConnection() {
        try {
            con = AgencyHikariCPDataSource.getConnection();
        } catch (SQLException ex) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        }
        return con;
    }

    public ResultSet query_all(final String query) {
        try {
            con = getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        }
        return rs;
    }

    public int rowCount(final String query) {
        int count = 0;

        rs = query_all(query);
        try {
            while (rs.next()) {
                ++count;
            }
        } catch (SQLException ex) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        }

        return count;
    }

    public int update_db(final String query) {
        int i = 0;
        try {
            con = getConnection();
            stmt = con.createStatement();
            i = stmt.executeUpdate(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
        } finally {
            try {
                stmt.close();
                con.close();
            } catch (SQLException ex) {
                logger.applicationLog(logger.logPreString() + "DB Exception  - " + ex.getMessage() + "\n\n", "", 9);
            }
        }

        return i;
    }

    public void closeConn() {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "DB Exception  - " + e.getMessage() + "\n\n", "", 9);
        }
    }
}
