package com.equant.csi.interfaces.cis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: szhu
 * Date: Jul 9, 2004
 * Time: 12:19:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class CISMappingLoader {

    /**
     * @param filePath
     * @param tableName
     * @return
     */
    private ResultSet loadFieldMappingCSVFileToResultSet(String filePath, String tableName) {

        ResultSet csvSet = null;
        try {
            Connection csvConn = getCSVJDBCConn(filePath);

            Statement st = csvConn.createStatement();
            String sql = "SELECT CISTABLENAME,CISCOLUMNNAME,CISSERVICE,CSISERVICE,CSISRVELMCLASS,CSISRVELMPROPNAME,CSISRVELMATTRNAME,DESCRIPTION,STATUS FROM "
                    + tableName;

            System.out.println("Query Statement: " + sql);
            csvSet = st.executeQuery(sql);

        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.toString());
        } catch (SQLException sqle) {
            System.out.println(sqle.toString());
        }

        return csvSet;
    }

    /**
     * @param filePath
     * @param valueMappingTableName
     * @return
     */
    private ResultSet loadValueMappingCSVFileToResultSet(String filePath, String valueMappingTableName) {

        ResultSet csvSet = null;
        try {
            Connection csvConn = getCSVJDBCConn(filePath);

            Statement st = csvConn.createStatement();
            String sql = "SELECT CSIPRODUCTNAME,CSIServcieElementClass,CSIAttributeName,CISValue,CSIValue,Description,Status FROM "
                    + valueMappingTableName;

            System.out.println("Query Statement: " + sql);
            csvSet = st.executeQuery(sql);

        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.toString());
        } catch (SQLException sqle) {
            System.out.println(sqle.toString());
        }

        return csvSet;
    }

    private Connection getCSVJDBCConn(String filePath) throws ClassNotFoundException, SQLException {
        System.out.println("CSV JDBC Driver Name: " + CISConstants.CIS_CSV_DRIVER);
        Class.forName(CISConstants.CIS_CSV_DRIVER);
        System.out.println("CSV JDBC URL " + getFileURL(filePath));
        Connection csvConn = DriverManager.getConnection(getFileURL(filePath), "na", "na");
        return csvConn;
    }

    /**
     * @param filePath
     * @return
     */
    private String getFileURL(String filePath) {
        String url = CISConstants.CIS_CSV_URL_PREFIX;
        url = url + filePath;
        return url;
    }

    /*
    private void handleCSVResultSet(ResultSet csvSet, String oracleURL, String oraUser, String oraPassword) {
        if (csvSet != null) {
            Connection oraConn = null;
            PreparedStatement ps = null;

            try {
                oraConn = getOracleJDBCConn(oracleURL, oraUser, oraPassword);
                ps = oraConn.prepareStatement("DELETE FROM Ccismapping");
                int i = ps.executeUpdate();
                System.out.println("rows deleted:" + i);

                ps = oraConn.prepareStatement("INSERT INTO Ccismapping ( " +
                        " CCISMAPPINGID         " +
                        ",CISTABLENAME          " +
                        ",CISCOLUMNNAME         " +
                        ",CISSERVICE            " +
                        ",CSISERVICE            " +
                        ",CSISRVELMCLASS        " +
                        ",CSISRVELMPROPNAME     " +
                        ",CSISRVELMATTRNAME     " +
                        ",DESCRIPTION           " +
                        ",STATUS                " +
                        ",CREATEDATE            " +
                        ",CREATEDBY             " +
                        ",LUPDDATE              " +
                        ",LUPDBY                ) " +
                        "VALUES (seqccismapping.nextval, ?,?,?,?,?,?,?,?,?, sysdate, '" + oraUser + "', sysdate, '" + oraUser + "' )");

                int rowAdded = 0;
                while (csvSet.next()) {
                    ps.setString(1, csvSet.getString("CISTABLENAME"));
                    ps.setString(2, csvSet.getString("CISCOLUMNNAME"));
                    ps.setString(3, csvSet.getString("CISSERVICE"));
                    ps.setString(4, csvSet.getString("CSISERVICE"));
                    ps.setString(5, csvSet.getString("CSISRVELMCLASS"));
                    ps.setString(6, csvSet.getString("CSISRVELMPROPNAME"));
                    ps.setString(7, csvSet.getString("CSISRVELMATTRNAME"));
                    ps.setString(8, csvSet.getString("DESCRIPTION"));
                    ps.setLong(9, csvSet.getLong("STATUS"));

                    ps.executeUpdate();
                    rowAdded++;
                }
                System.out.println("Rows Added: " + rowAdded);
                oraConn.commit();
            } catch (ClassNotFoundException cnfe) {
                System.out.println(cnfe.toString());
            } catch (SQLException sqle) {
                System.out.println(sqle.toString());
            } finally {
                try {
                    ps.close();
                    oraConn.rollback();
                } catch (Exception e) {
                    System.out.println("Error in exception:" + e.toString());
                }

            }

        }
    }
    */

    private Connection getOracleJDBCConn(String oracleURL, String oraUser, String oraPassword) throws ClassNotFoundException, SQLException {
        System.out.println("ORACLE JDBC Driver Name: " + CISConstants.CIS_ORACLE_DRIVER);
        Class.forName(CISConstants.CIS_ORACLE_DRIVER);
        String url = CISConstants.CIS_ORACLE_URL_PREFIX + oracleURL;
        System.out.println("ORACLE JDBC URL: " + url);
        Connection oraConn = DriverManager.getConnection(url, oraUser, oraPassword);
        oraConn.setAutoCommit(false);
        return oraConn;
    }

    /**
     * @param csvSet
     */
    private void convertFieldMappingCSVResultSettoSQl(ResultSet csvSet, String sqlFileName, String filePath, String productName) {
        if (csvSet != null) {
            String productToDelete = "";

            if (!"IP".equals(productName)) {
                productToDelete = " Where CSISERVICE IN ('ATM_PURPLE', 'FRAME_RELAY_PURPLE')";
            }else {
                productToDelete = " Where CSISERVICE NOT IN ('ATM_PURPLE', 'FRAME_RELAY_PURPLE')";
            }

            StringBuffer sb = new StringBuffer(20000);
            FileOutputStream outStream = null;
            String valueString = null;
            String sqlLinePrefix = "INSERT INTO Ccismapping ( " +
                    " CCISMAPPINGID         " +
                    ",CISTABLENAME          " +
                    ",CISCOLUMNNAME         " +
                    ",CISSERVICE            " +
                    ",CSISERVICE            " +
                    ",CSISRVELMCLASS        " +
                    ",CSISRVELMPROPNAME     " +
                    ",CSISRVELMATTRNAME     " +
                    ",DESCRIPTION           " +
                    ",STATUS                " +
                    ",CREATEDATE            " +
                    ",CREATEDBY             " +
                    ",LUPDDATE              " +
                    ",LUPDBY                ) " +
                    "VALUES (seqccismapping.nextval, ";

            String sqlLinePostFix = "sysdate, 'CSIADM', sysdate, 'CSIADM' );";
            int rowCount = 0;
            try {
                outStream = new FileOutputStream(filePath + "\\" + sqlFileName);
                //append some general information
                sb.append("spool generatefieldmapping.log \n");
                sb.append("set echo on \n");
                sb.append("DELETE FROM Ccismapping " + productToDelete + "; \n");
                //only for release 6.1
                sb.append("ALTER TABLE CCISMAPPING MODIFY (CISSERVICE VARCHAR2(100), CSISERVICE VARCHAR2(100)); \n");

                while (csvSet.next()) {
                    rowCount ++;
                    System.out.println(rowCount);
                    valueString = "'" + csvSet.getString("CISTABLENAME") + "', " +
                            "'" + csvSet.getString("CISCOLUMNNAME") + "', " +
                            "'" + csvSet.getString("CISSERVICE") + "', " +
                            "'" + csvSet.getString("CSISERVICE") + "', " +
                            "'" + csvSet.getString("CSISRVELMCLASS") + "', " +
                            "'" + csvSet.getString("CSISRVELMPROPNAME") + "', " +
                            "'" + csvSet.getString("CSISRVELMATTRNAME") + "', " +
                            "'" + csvSet.getString("DESCRIPTION") + "', " +
                            csvSet.getLong("STATUS") + ",";
                    sb.append(sqlLinePrefix + valueString + sqlLinePostFix);
                    sb.append("\n");
                }
                sb.append("COMMIT;\n");
                sb.append("set echo off\n");
                sb.append("spool off\n");
                System.out.println("Rows Added: " + rowCount);
                outStream.write(sb.toString().getBytes());
                outStream.close();
            } catch (Exception e) {
                System.out.println("Error in generate sql insert statement:" + e.toString());
            }
        }
    }

    /**
     * @param csvSet
     */
    private void convertValueMappingCSVResultSettoSQl(ResultSet csvSet, String sqlFileName, String filePath, String productName) {
        if (csvSet != null) {
            String productToDelete = "";
            /*
            if (!"IP".equals(productName)) {
                productToDelete = " Where CSISERVICE IN ('ATM_PURPLE', 'FRAME_RELAY_PURPLE')";
            }else {
                productToDelete = " Where CSISERVICE NOT IN ('ATM_PURPLE', 'FRAME_RELAY_PURPLE')";
            }
             */

            StringBuffer sb = new StringBuffer();
            FileOutputStream outStream = null;
            String valueString = null;
            String sqlLinePrefix = "INSERT INTO Ccisvaluemapping ( " +
                    " CCISVALUEMAPPINGID        " +
                    ",CSISERVICE                " +
                    ",CSISRVELMCLASS            " +
                    ",CSISRVELMATTRNAME         " +
                    ",CISVALUE                  " +
                    ",CSIVALUE                  " +
                    ",DESCRIPTION               " +
                    ",STATUS                    " +
                    ",CREATEDATE                " +
                    ",CREATEDBY                 " +
                    ",LUPDDATE                  " +
                    ",LUPDBY                    ) " +
                    "VALUES (seqccisvaluemapping.nextval, ";

            String sqlLinePostFix = "sysdate, 'CSIADM', sysdate, 'CSIADM' );";
            int rowCount = 0;
            try {
                outStream = new FileOutputStream(filePath + "\\" + sqlFileName);
                //append some general information
                sb.append("spool generatevaluemapping.log \n");
                sb.append("set echo on \n");
                sb.append("DELETE FROM Ccisvaluemapping " + productToDelete + ";\n");
                sb.append("ALTER TABLE Ccisvaluemapping MODIFY (CSISERVICE VARCHAR2(100));\n");

                while (csvSet.next()) {
                    rowCount ++;
                    valueString = "'" + csvSet.getString("CSIPRODUCTNAME").trim() + "', " +
                            "'" + csvSet.getString("CSISERVCIEELEMENTCLASS").trim() + "', " +
                            "'" + csvSet.getString("CSIATTRIBUTENAME").trim() + "', " +
                            "'" + csvSet.getString("CISVALUE").trim() + "', " +
                            "'" + csvSet.getString("CSIVALUE").trim() + "', " +
                            "'" + csvSet.getString("DESCRIPTION").trim() + "', " +
                            csvSet.getLong("STATUS") + ",";
                    sb.append(sqlLinePrefix + valueString + sqlLinePostFix);
                    sb.append("\n");
                }

                sb.append("COMMIT;\n");
                sb.append("set echo off\n");
                sb.append("spool off\n");
                System.out.println("Rows Added: " + rowCount);
                outStream.write(sb.toString().getBytes());
                outStream.close();
            } catch (Exception e) {
                System.out.println("Error in generate sql insert statement:" + e.toString());
            }
        }
    }

    /**
     * @param args example of args
     *             args[0]:  d:\temp
     *             args[1]:  CIS_CSI_MAPPING_TABLE
     *             args[2]:  d:\csi_csi_mapping_table.sql
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.exit(0);
        } else {
            System.out.println("Beginning to load data...");
            String productName = "";

            CISMappingLoader ml = new CISMappingLoader();
            String filePath = (String) args[0];
            String fieldMappingTableName = (String) args[1];
            if (fieldMappingTableName.indexOf("IP") > 0) {
                productName = "IP";
            } else {
                productName = "MTN";
            }
            String sqlFieldMappingFileName = (String) args[3];
            String valueMappingTableName = (String) args[2];
            String sqlValueMappingFileName = (String) args[4];

            ml.convertFieldMappingCSVResultSettoSQl(ml.loadFieldMappingCSVFileToResultSet(filePath, fieldMappingTableName), sqlFieldMappingFileName, filePath, productName);
            ml.convertValueMappingCSVResultSettoSQl(ml.loadValueMappingCSVFileToResultSet(filePath, valueMappingTableName), sqlValueMappingFileName, filePath, productName);
            System.out.println("End.");
        }
    }
}
