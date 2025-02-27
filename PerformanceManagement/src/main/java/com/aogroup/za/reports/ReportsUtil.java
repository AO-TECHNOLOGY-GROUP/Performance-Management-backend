/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.reports;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import log.Logging;

/**
 *
 * @author demo
 */
public class ReportsUtil {
    Logging logger;
    
    public ReportsUtil () {
        logger = new Logging();
    }
    
    public int createReportCategory(JsonObject data) {
        int i = 0;
        String name = data.getString("name");
        String userId = data.getString("user");
        
         String sql = "INSERT INTO ReportCategories ([Name],[CreatedBy]) "
                + "VALUES (?,?)";
        
        DBConnection conn = new DBConnection();
        Connection connection = conn.getConnection();
        
        try (PreparedStatement prInsertReportCategory = connection.prepareStatement(sql)) {
            prInsertReportCategory.setString(1, name);
            prInsertReportCategory.setString(2, userId);

            i = prInsertReportCategory.executeUpdate();
            
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } 
            conn.closeConn();
        }
        
        return i;
    }
    
    public int createReport (JsonObject data) {
        int i = 0;
        String name = data.getString("name");
        String path = data.getString("path");
        int depth = Integer.parseInt(data.getString("depth"));
        int category = Integer.parseInt(data.getString("category"));
        String reportCategoryId = data.getString("reportCategoryId");
        String userId = data.getString("user");
        
        
        String sql = "INSERT INTO Reports ([Name],[Path],[Depth],[Category],[CreatedBy],[ReportCategoryId]) "
                + "VALUES (?,?,?,?,?,?)";
        
        DBConnection conn = new DBConnection();
        Connection connection = conn.getConnection();
        
        try (PreparedStatement prInsertReport = connection.prepareStatement(sql)) {
            prInsertReport.setString(1, name);
            prInsertReport.setString(2, path);
            prInsertReport.setInt(3, depth);
            prInsertReport.setInt(4, category);
            prInsertReport.setString(5, userId);
            prInsertReport.setString(6, reportCategoryId);
            i = prInsertReport.executeUpdate();
            
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }  
            conn.closeConn();
        }
        
        return i;
    }
    
    public JsonObject fetchReportCategoryById(String id) {
        JsonObject result = new JsonObject();
        
        String sql = "SELECT * FROM ReportCategories WHERE Id = '"+ id +"'";
        
        DBConnection conn = new DBConnection();
        
        try {
            ResultSet rs = conn.query_all(sql);
            while (rs.next()) {
                result
                        .put("id", rs.getString("Id"))
                        .put("name", rs.getString("Name"))
                        .put("createdDate", rs.getString("CreatedDate"))
                        .put("createdBy", rs.getString("CreatedBy"));
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        
        return result;
    }
    
    public JsonArray fetchReportCategories() {
        JsonArray results = new JsonArray();
        
        String sql = "SELECT * FROM ReportCategories";
        
        DBConnection conn = new DBConnection();
        
        try {
            ResultSet rs = conn.query_all(sql);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo
                        .put("id", rs.getString("Id"))
                        .put("name", rs.getString("Name"))
                        .put("createdDate", rs.getString("CreatedDate"))
                        .put("createdBy", rs.getString("CreatedBy"));
                        results.add(jo);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        
        return results;
    }
    
    public JsonObject fetchReportById(String id) {
        JsonObject result = new JsonObject();
        
        String sql = "SELECT * FROM Reports WHERE Id = '"+ id +"'";
        
        DBConnection conn = new DBConnection();
        
        try {
            ResultSet rs = conn.query_all(sql);
            while (rs.next()) {
                result
                        .put("id", rs.getString("Id"))
                        .put("name", rs.getString("Name"))
                        .put("path", rs.getString("Path"))
                        .put("depth", String.valueOf(rs.getInt("Depth")))
                        .put("category", String.valueOf(rs.getInt("Category")))
                        .put("createdDate", rs.getString("CreatedDate"))
                        .put("createdBy", rs.getString("CreatedBy"))
                        .put("reportCategoryId", rs.getString("ReportCategoryId"));
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        
        return result;
    }
    
    public JsonArray fetchReports() {
        JsonArray results = new JsonArray();
        
        String sql = "SELECT * FROM Reports";
        
        DBConnection conn = new DBConnection();
        
        try {
            ResultSet rs = conn.query_all(sql);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo
                        .put("id", rs.getString("Id"))
                        .put("name", rs.getString("Name"))
                        .put("path", rs.getString("Path"))
                        .put("depth", String.valueOf(rs.getInt("Depth")))
                        .put("category", String.valueOf(rs.getInt("Category")))
                        .put("createdDate", rs.getString("CreatedDate"))
                        .put("createdBy", rs.getString("CreatedBy"))
                        .put("reportCategoryId", rs.getString("ReportCategoryId"));
                results.add(jo);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        
        return results;
    }
    
    public JsonArray getOutstandingLoanBalancesReport(String branchId, String overdueDays) {
        JsonArray data = new JsonArray();
        String execStoredProcedure = "{ call sp_OutstandingLoanBalancesReport(?,?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try ( CallableStatement callableStatement = connection.prepareCall(execStoredProcedure); ) {
            callableStatement.setString(1, !branchId.isEmpty() ? branchId : "1000");
            callableStatement.setInt(2, !overdueDays.isEmpty() ? Integer.parseInt(overdueDays) : null);
//            callableStatement.registerOutParameter(3, Types.DOUBLE); // Account Balance == Loan Balnace
//            callableStatement.registerOutParameter(4, Types.DOUBLE);
//            callableStatement.registerOutParameter(5, Types.DOUBLE);
//            callableStatement.registerOutParameter(6, Types.DOUBLE);
//            callableStatement.registerOutParameter(7, Types.DOUBLE);
//            callableStatement.registerOutParameter(8, Types.DOUBLE);
//            callableStatement.registerOutParameter(9, Types.DOUBLE);
//            callableStatement.registerOutParameter(10, Types.DOUBLE);
//            callableStatement.registerOutParameter(11, Types.DOUBLE);
//            callableStatement.registerOutParameter(12, Types.DOUBLE);
//            callableStatement.registerOutParameter(13, Types.DOUBLE);
//            callableStatement.registerOutParameter(14, Types.DOUBLE);
//            callableStatement.registerOutParameter(15, Types.DOUBLE);
//            callableStatement.registerOutParameter(16, Types.DOUBLE);
//            callableStatement.registerOutParameter(17, Types.DOUBLE);
//            callableStatement.registerOutParameter(18, Types.DOUBLE);
//            callableStatement.registerOutParameter(19, Types.VARCHAR); // Next Payment Date
//            callableStatement.registerOutParameter(20, Types.VARCHAR); // Loan End Date
//            callableStatement.registerOutParameter(21, Types.INTEGER); // Elapsed LoanSchedules
//            callableStatement.registerOutParameter(22, Types.DOUBLE); // RollOver Fee
//            callableStatement.registerOutParameter(23, Types.DOUBLE);  // RollOverFee Due
//            callableStatement.registerOutParameter(24, Types.DOUBLE); // RollOverFeePaid
//            int i = callableStatement.executeUpdate();
            ResultSet rs = callableStatement.executeQuery();
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo
                        .put("successIndicator",true)
                        .put("CustomerFirstName",rs.getString("CustomerFirstName"))
                        .put("CustomerMiddleName",rs.getString("CustomerMiddleName"))
                        .put("CustomerLastName",rs.getString("CustomerLastName"))
                        .put("CustomerName",rs.getString("CustomerName"))
                        .put("CustomerIdNumber",rs.getString("CustomerIdNumber"))
                        .put("PhoneNumber",rs.getString("PhoneNumber"))
                        .put("CustomerDOB",rs.getString("CustomerDOB"))
                        .put("BranchName",rs.getString("BranchName"))
                        .put("BranchCode",rs.getString("BranchCode"))
                        .put("ProductName",rs.getString("ProductName"))
                        .put("LoanOfficer",rs.getString("LoanOfficer"))
                        .put("LoanStartDate",rs.getString("LoanStartDate"))
                        .put("LoanEndDate",rs.getString("LoanEndDate"))
                        .put("NextPaymentDate",rs.getString("NextPaymentDate"))
                        .put("LoanAmount",String.valueOf(rs.getDouble("LoanAmount")))
                        .put("InterestAmount",String.valueOf(rs.getDouble("InterestAmount")))
                        .put("PrincipalPerInstallment",String.valueOf(rs.getDouble("PrincipalPerInstallment")))
                        .put("InterestPerInstallment",String.valueOf(rs.getDouble("InterestPerInstallment")))
                        .put("TotalInstallmentAmount",String.valueOf(rs.getDouble("TotalInstallmentAmount")))
                        .put("ArrearsAmount",String.valueOf(rs.getDouble("ArrearsAmount")))
                        .put("PenaltyChargedToday",String.valueOf(rs.getDouble("PenaltyChargedToday")))
                        .put("PenaltyPaidToday",String.valueOf(rs.getDouble("PenaltyPaidToday")))
                        .put("RollOverFeeDue",String.valueOf(rs.getDouble("RollOverFeeDue")))
                        .put("PenaltyDueToday",String.valueOf(rs.getDouble("PenaltyDueToday")))
                        .put("ExpectedPrincipalInstallment",String.valueOf(rs.getDouble("ExpectedPrincipalInstallment")))
                        .put("ExpectedInterestInstallment",String.valueOf(rs.getDouble("ExpectedInterestInstallment")))
                        .put("PrincipalPaid",String.valueOf(rs.getDouble("PrincipalPaid")))
                        .put("InterestPaid",String.valueOf(rs.getDouble("InterestPaid")))
                        .put("TotalPaidAmount",String.valueOf(rs.getDouble("TotalPaidAmount")))
                        .put("TotalAmountDue",String.valueOf(rs.getDouble("TotalAmountDue")))
                        .put("ElapsedSchedules",String.valueOf(rs.getDouble("ElapsedSchedules")))
                        .put("OverDueDays",String.valueOf(rs.getInt("OverDueDays")))
                        .put("LoanRequestID",rs.getString("LoanRequestID"))
                        .put("PercentagePaid",String.valueOf(rs.getDouble("PercentagePaid")))
                        .put("Industry",rs.getString("Industry"))
                        .put("DealingIn",rs.getString("DealingIn"))
                        .put("CreatedDate",rs.getString("CreatedDate"));
                
                data.add(jo);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
        return data;
    }
    
    public JsonArray getLoanArrears(String branchId, String rManagerId) {
        JsonArray data = new JsonArray();
        String execStoredProcedure = "{ call sp_LoanArrears(?,?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try ( CallableStatement callableStatement = connection.prepareCall(execStoredProcedure); ) {
            callableStatement.setString(1, !branchId.isEmpty() ? branchId : "1000");
            callableStatement.setString(2, !rManagerId.isEmpty() ? rManagerId : null);
//            callableStatement.registerOutParameter(3, Types.DOUBLE); // Account Balance == Loan Balnace
//            callableStatement.registerOutParameter(4, Types.DOUBLE);
//            callableStatement.registerOutParameter(5, Types.DOUBLE);
//            callableStatement.registerOutParameter(6, Types.DOUBLE);
//            callableStatement.registerOutParameter(7, Types.DOUBLE);
//            callableStatement.registerOutParameter(8, Types.DOUBLE);
//            callableStatement.registerOutParameter(9, Types.DOUBLE);
//            callableStatement.registerOutParameter(10, Types.DOUBLE);
//            callableStatement.registerOutParameter(11, Types.DOUBLE);
//            callableStatement.registerOutParameter(12, Types.DOUBLE);
//            callableStatement.registerOutParameter(13, Types.DOUBLE);
//            callableStatement.registerOutParameter(14, Types.DOUBLE);
//            callableStatement.registerOutParameter(15, Types.DOUBLE);
//            callableStatement.registerOutParameter(16, Types.DOUBLE);
//            callableStatement.registerOutParameter(17, Types.DOUBLE);
//            callableStatement.registerOutParameter(18, Types.DOUBLE);
//            callableStatement.registerOutParameter(19, Types.VARCHAR); // Next Payment Date
//            callableStatement.registerOutParameter(20, Types.VARCHAR); // Loan End Date
//            callableStatement.registerOutParameter(21, Types.INTEGER); // Elapsed LoanSchedules
//            callableStatement.registerOutParameter(22, Types.DOUBLE); // RollOver Fee
//            callableStatement.registerOutParameter(23, Types.DOUBLE);  // RollOverFee Due
//            callableStatement.registerOutParameter(24, Types.DOUBLE); // RollOverFeePaid
//            int i = callableStatement.executeUpdate();
            ResultSet rs = callableStatement.executeQuery();
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo
                        
                        .put("successIndicator",true)
                        .put("CustomerFirstName",rs.getString("CustomerFirstName"))
                        .put("CustomerMiddleName",rs.getString("CustomerMiddleName"))
                        .put("CustomerLastName",rs.getString("CustomerLastName"))
                        .put("CustomerName",rs.getString("CustomerName"))
                        .put("CustomerIdNumber",rs.getString("CustomerIdNumber"))
                        .put("PhoneNumber",rs.getString("PhoneNumber"))
                        .put("CustomerDOB",rs.getString("CustomerDOB"))
                        .put("BranchName",rs.getString("BranchName"))
                        .put("BranchCode",rs.getString("BranchCode"))
                        .put("ProductName",rs.getString("ProductName"))
                        .put("LoanOfficer",rs.getString("LoanOfficer"))
                        .put("LoanStartDate",rs.getString("LoanStartDate"))
                        .put("LoanEndDate",rs.getString("LoanEndDate"))
                        .put("NextPaymentDate",rs.getString("NextPaymentDate"))
                        .put("LoanAmount",String.valueOf(rs.getDouble("LoanAmount")))
                        .put("PrincipalPerInstallment",String.valueOf(rs.getDouble("PrincipalPerInstallment")))
                        .put("InterestPerInstallment",String.valueOf(rs.getDouble("InterestPerInstallment")))
                        .put("TotalInstallmentAmount",String.valueOf(rs.getDouble("TotalInstallmentAmount")))
                        .put("PrincipalArrearsAmount", String.valueOf(rs.getDouble("PrincipalArrearsAmount")))
                        .put("InterestArrearsAmount", String.valueOf(rs.getDouble("InterestArrearsAmount")))
                        .put("ArrearsAmount",String.valueOf(rs.getDouble("ArrearsAmount")))
                        .put("PenaltyChargedToday",String.valueOf(rs.getDouble("PenaltyChargedToday")))
                        .put("PenaltyPaidToday",String.valueOf(rs.getDouble("PenaltyPaidToday")))
                        .put("PenaltyDueToday",String.valueOf(rs.getDouble("PenaltyDueToday")))
                        .put("ExpectedPrincipalInstallment",String.valueOf(rs.getDouble("ExpectedPrincipalInstallment")))
                        .put("ExpectedInterestInstallment",String.valueOf(rs.getDouble("ExpectedInterestInstallment")))
                        .put("PrincipalPaid",String.valueOf(rs.getDouble("PrincipalPaid")))
                        .put("InterestPaid",String.valueOf(rs.getDouble("InterestPaid")))
                        .put("TotalPaidAmount",String.valueOf(rs.getDouble("TotalPaidAmount")))
                        .put("TotalAmountDue",String.valueOf(rs.getDouble("TotalAmountDue")))
                        .put("ElapsedSchedules",String.valueOf(rs.getInt("ElapsedSchedules")))
                        .put("OverDueDays",String.valueOf(rs.getInt("OverDueDays")))
                        .put("Loancount", rs.getString("Loancount"))
                        .put("NoofLoans", String.valueOf(rs.getInt("NoofLoans")))
                        .put("LoanRequestID",rs.getString("LoanRequestID"))
                        .put("RolloverFee", String.valueOf(rs.getDouble("RolloverFee")))
                        .put("Industry",rs.getString("Industry"))
                        .put("DealingIn",rs.getString("DealingIn"))
                        .put("BranchGUID", rs.getString("BranchGUID"))
                        .put("ROGUID", rs.getString("ROGUID"))
                        .put("GuarantorName", rs.getString("GuarantorName"))
                        .put("GuarantorPhone", rs.getString("GuarantorPhone"));
                
                data.add(jo);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
        return data;
    }
    
    public JsonObject getBRDashboard(String branchId) {
        JsonObject result = new JsonObject();
//        //System.out.println("BRANCH ID::::::" + branchId);

        String execStoredProcedure = "{ call sp_BRDashBoard(?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = null;

        try {
            connection = dbConnection.getConnection();
            try (CallableStatement callableStatement = connection.prepareCall(execStoredProcedure)) {
                if (!branchId.equals("NULL")) {
                    callableStatement.setString(1, branchId);
                } else {
                    callableStatement.setNull(1, Types.NULL);
                }

                try (ResultSet rs = callableStatement.executeQuery()) {
                    while (rs.next()) {
                        result
                            .put("ActiveCustomers", String.valueOf(rs.getInt("ActiveCustomers")))
                            .put("IncativeCustomers", String.valueOf(rs.getInt("IncativeCustomers")))
                            .put("LimitsPendingApproval", String.valueOf(rs.getInt("LimitsPendingApproval")))
                            .put("OLBCount", String.valueOf(rs.getInt("OLBCount")))
                            .put("OLBAmount", String.valueOf(rs.getDouble("OLBAmount")))
                            .put("ArreasCount", String.valueOf(rs.getInt("ArreasCount")))
                            .put("MonthlyArreasCount", String.valueOf(rs.getInt("MonthlyArreasCount")))
                            .put("ArrearsAmount", String.valueOf(rs.getDouble("ArrearsAmount")))
                            .put("MonthlyArrears", String.valueOf(rs.getDouble("MonthlyArrears")))
                            .put("DisbursedAmount", String.valueOf(rs.getDouble("DisbursedAmount")))
                            .put("Recruitments", String.valueOf(rs.getInt("Recruitments")))
                            .put("Collections", String.valueOf(rs.getDouble("Collections")))
                            .put("PAR", String.valueOf(rs.getFloat("PAR")))
                            .put("BranchName", rs.getString("BranchName"))
                            .put("DisbursementCount", String.valueOf(rs.getInt("DisbursementCount")))
                            .put("LoansDueToday", String.valueOf(rs.getInt("LoansDueToday")))
                            .put("LoansDueTodayAmount", String.valueOf(rs.getDouble("LoansDueTodayAmount")))
                            .put("Funded", String.valueOf(rs.getDouble("Funded")))
                            .put("LoansPendingApproval", String.valueOf(rs.getInt("LoansPendingApproval")))
                            .put("LoansPendingApprovalAmount", String.valueOf(rs.getDouble("LoansPendingApprovalAmount")))
                            .put("LoansPendingDisbursement", String.valueOf(rs.getInt("LoansPendingDisbursement")))
                            .put("LoansPendingDisbursementAmount", String.valueOf(rs.getDouble("LoansPendingDisbursementAmount")))
                            .put("ArrearsCollected", String.valueOf(rs.getDouble("ArrearsCollected")));
                    }
                }
            }
        } catch (SQLException e) {
            //e.printStackTrace();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException se) {
                    //e.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
        
        return result;
    }
    
    public JsonArray getDuesReportByDate(String startDate, String endDate ,String branchId) {
        JsonArray data = new JsonArray();
        String execStoredProcedure = "{ call sp_DuesReportByDate(?,?,?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try ( CallableStatement callableStatement = connection.prepareCall(execStoredProcedure); ) {
            callableStatement.setString(1, !startDate.isEmpty() ? startDate : null);
            callableStatement.setString(2, !endDate.isEmpty() ? endDate : null);
            callableStatement.setString(3, !branchId.isEmpty() ? branchId : "1000");
            ResultSet rs = callableStatement.executeQuery();
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo
                        .put("successIndicator",true)
                        .put("OtherNames",rs.getString("OtherNames"))
                        .put("FirstName",rs.getString("FirstName"))
                        .put("PhoneNumber",rs.getString("PhoneNumber"))
                        .put("RelationshipOfficer",rs.getString("RelationshipOfficer"))
                        .put("BranchName",rs.getString("BranchName"))
                        .put("LoanProduct",rs.getString("LoanProduct"))
                        .put("DisbursedAmount",String.valueOf(rs.getDouble("DisbursedAmount")))
                        .put("LoanReferenceNumber",String.valueOf(rs.getInt("LoanReferenceNumber")))
                        .put("DisbursementDate",rs.getString("DisbursementDate"))
                        .put("ExpectedDueDate",rs.getString("ExpectedDueDate"))
                        .put("PrincipalDue",String.valueOf(rs.getDouble("PrincipalDue")))
                        .put("InterestDue",String.valueOf(rs.getDouble("InterestDue")))
                        .put("TotalDue",String.valueOf(rs.getDouble("TotalDue")))
                        .put("AmountPaid",String.valueOf(rs.getDouble("AmountPaid")))
                        .put("PrincipalArrears",String.valueOf(rs.getDouble("PrincipalArrears")))
                        .put("InterestArrears",String.valueOf(rs.getDouble("InterestArrears")))
                        .put("Arrears",String.valueOf(rs.getDouble("Arrears")))
                        .put("LstatusDes",String.valueOf(rs.getDouble("LstatusDes")))
                        .put("ServicedLoans",String.valueOf(rs.getInt("ServicedLoans")))
                        .put("GuarantorName",rs.getString("GuarantorName"))
                        .put("GuarantorPhone",rs.getString("GuarantorPhone"))
                        .put("OLB",String.valueOf(rs.getDouble("OLB")))
                        .put("InterestAmount",String.valueOf(rs.getDouble("InterestAmount")));
                
                data.add(jo);
            }
            if (callableStatement != null) {
                try {
                    callableStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            try {
                connection.close();
            } catch (SQLException e) {
                //e.printStackTrace();
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            dbConnection.closeConn();
        }
        return data;
    }
    
    public JsonArray getCustomerStatement(String customerPhone, String startDate, String endDate) {
        JsonArray results = new JsonArray();
        
        String execStoredProcedure = "{ call sp_CustomerAccountStatementbyMobileNumber2(?,?,?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        
        try {
            connection = dbConnection.getConnection();
            try (CallableStatement callableStatement = connection.prepareCall(execStoredProcedure);) {
                callableStatement.setString(1, customerPhone);

                // Assuming startDate and endDate are Strings, convert them to java.sql.Date
                java.sql.Date sqlStartDate = java.sql.Date.valueOf(startDate);
                java.sql.Date sqlEndDate = java.sql.Date.valueOf(endDate);

                callableStatement.setDate(2, sqlStartDate);
                callableStatement.setDate(3, sqlEndDate);

                ResultSet rs = callableStatement.executeQuery();

                while (rs.next()) {
                    JsonObject jo = new JsonObject();
                    jo
                            .put("AccountName",rs.getString("AccountName"))
                            .put("Product",rs.getString("Product"))
                            .put("id",rs.getString("id"))
                            .put("TrxDate",rs.getString("TrxDate"))
                            .put("PrimaryDescription",rs.getString("PrimaryDescription"))
                            .put("reference",rs.getString("JId"))
                            .put("debit",String.valueOf(rs.getDouble("Debit")))
                            .put("credit",String.valueOf(rs.getDouble("credit")))
                            .put("RunningTotal",String.valueOf(rs.getDouble("RunningTotal")))
                            .put("InterestDR",String.valueOf(rs.getDouble("InterestDR")))
                            .put("InterestCR",String.valueOf(rs.getDouble("InterestCR")))
                            .put("IntBalance",String.valueOf(rs.getDouble("IntBalance")));

                    results.add(jo);
                }
                if (callableStatement != null) {
                    try {
                        callableStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SQLException throwables) {
//            throwables.printStackTrace();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            dbConnection.closeConn();
        }
        
        return results;
    }
    
    public JsonObject getRODashboard(String relationshipOfficerId) {
        JsonObject result = new JsonObject();
//        //System.out.println("BRANCH ID::::::" + relationshipOfficerId);
        
        String execStoredProcedure = "{ call sp_ROdashboard(?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        
        try (CallableStatement callableStatement = connection.prepareCall(execStoredProcedure);) {
            callableStatement.setString(1, relationshipOfficerId);
            
            ResultSet rs = callableStatement.executeQuery();
            
            while (rs.next()) {
                result
                        .put("NumberOfCustomers",String.valueOf(rs.getInt("NumberOfCustomers")))
                        .put("InactiveCustomers",String.valueOf(rs.getInt("InactiveCustomers")))
                        .put("ActiveCustomers",String.valueOf(rs.getInt("ActiveCustomers")))
                        .put("ActiveLoanCount",String.valueOf(rs.getInt("ActiveLoanCount")))
                        .put("TotalLoanAmount",String.valueOf(rs.getDouble("TotalLoanAmount")))
                        .put("ArreasCount",String.valueOf(rs.getInt("ArreasCount")))
                        .put("ArrearsAmount",String.valueOf(rs.getDouble("ArrearsAmount")))
                        .put("MonthlyArrears",String.valueOf(rs.getDouble("MonthlyArrears")))
                        .put("MonthlyArrearsCount",String.valueOf(rs.getInt("MonthlyArrearsCount")))
                        .put("LoansDueToday",String.valueOf(rs.getInt("LoansDueToday")))
                        .put("LoansDueTodayAmount",String.valueOf(rs.getDouble("LoansDueTodayAmount")))
                        .put("DibursedCount",String.valueOf(rs.getInt("DibursedCount")))
                        .put("RecruitedCount",String.valueOf(rs.getInt("RecruitedCount")))
                        .put("PLCount",String.valueOf(rs.getInt("PLCount")))
                        .put("PLAmount",String.valueOf(rs.getDouble("PLAmount")))
                        .put("NPLCount",String.valueOf(rs.getInt("NPLCount")))
                        .put("NPLAmount",String.valueOf(rs.getDouble("NPLAmount")))
                        .put("DisbursedAmount",String.valueOf(rs.getDouble("DisbursedAmount")))
                        .put("Collections",String.valueOf(rs.getDouble("Collections")))
                        .put("Prepaids",String.valueOf(rs.getDouble("Prepaids")))
                        .put("ArrearsCollected", String.valueOf(rs.getDouble("ArrearsCollected")));
            }
            if (callableStatement != null) {
                try {
                    callableStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
        
        return result;
    } 
    
    public JsonObject getCallcenterDashboard(String AgentId) {
        JsonObject result = new JsonObject();
        String execStoredProcedure = "{ call Sp_CallCenterDashboard(?) }";
        DBConnection dbConnection = new DBConnection();
        Connection connection = null;

        try {
            connection = dbConnection.getConnection();
            try (CallableStatement callableStatement = connection.prepareCall(execStoredProcedure)) {

                // Pass the specific UUID for admin or provided AgentId
                if (AgentId == null || AgentId.isEmpty()) {
                    callableStatement.setString(1, "D7222098-90EB-436E-AC20-F0A856743146"); // Admin UUID
                } else {
                    callableStatement.setString(1, AgentId);
                }

                // Execute the stored procedure
                try (ResultSet rs = callableStatement.executeQuery()) {
                    if (rs.next()) {
                        result
                            .put("AllocatedBranchesCount", rs.getInt("AllocatedBranchesCount"))
                            .put("AllocatedCustomersCount", rs.getInt("AllocatedCustomersCount"))
                            .put("PTPThisMonthCount", rs.getInt("PTPThisMonthCount"))
                            .put("PTPPaidThisMonthCount", rs.getInt("PTPPaidThisMonthCount"))
                            .put("PTPDueTodayCount", rs.getInt("PTPDueTodayCount"))
                            .put("PTPPaidTodayCount", rs.getInt("PTPPaidTodayCount"))
                            .put("TotalAllocatedAmount", rs.getDouble("TotalAllocatedAmount"))
                            .put("AmountPaidThisMonth", rs.getDouble("AmountPaidThisMonth"));
                    } else {
                        result.put("message", "No data available for the provided AgentId.");
                    }
                }
            }
            
        } catch (SQLException e) {
            //e.printStackTrace();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException se) {
                    //e.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
                
        return result;
    }

}
