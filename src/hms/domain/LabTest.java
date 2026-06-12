package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class LabTest {
    public int test_id;
    public int patient_id;
    public int doctor_id;
    public String test_name;
    public String test_date;
    public String status;
    public double test_fee;
    // For display
    public String patient_name;
    public String doctor_name;

    public LabTest() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO lab_test(patient_id,doctor_id,test_name,test_date,status,test_fee) VALUES(?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patient_id);
            ps.setInt(2, doctor_id);
            ps.setString(3, test_name);
            ps.setString(4, test_date);
            ps.setString(5, status);
            ps.setDouble(6, test_fee);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) test_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "LabTest.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean updateStatus(String new_status) {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE lab_test SET status=? WHERE test_id=?");
            ps.setString(1, new_status);
            ps.setInt(2, test_id);
            ps.executeUpdate();
            this.status = new_status;
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "LabTest.updateStatus", e.getMessage(), e);
            return false;
        }
    }

    public static ArrayList<LabTest> getAll() {
        ArrayList<LabTest> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT lt.*, p.full_name AS patient_name, d.full_name AS doctor_name " +
                         "FROM lab_test lt " +
                         "JOIN patient p ON lt.patient_id = p.patient_id " +
                         "JOIN doctor d  ON lt.doctor_id  = d.doctor_id " +
                         "ORDER BY lt.test_date DESC";
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                LabTest t = new LabTest();
                t.test_id      = rs.getInt("test_id");
                t.patient_id   = rs.getInt("patient_id");
                t.doctor_id    = rs.getInt("doctor_id");
                t.test_name    = rs.getString("test_name");
                t.test_date    = rs.getString("test_date");
                t.status       = rs.getString("status");
                t.test_fee     = rs.getDouble("test_fee");
                t.patient_name = rs.getString("patient_name");
                t.doctor_name  = rs.getString("doctor_name");
                list.add(t);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "LabTest.getAll", e.getMessage(), e);
        }
        return list;
    }
}
