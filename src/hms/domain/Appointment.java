package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Appointment {
    public int appt_id;
    public int patient_id;
    public int doctor_id;
    public String appt_datetime;
    public String status;
    public String reason;
    public String notes;
    // For display
    public String patient_name;
    public String doctor_name;

    public Appointment() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO appointment(patient_id,doctor_id,appt_datetime,status,reason,notes) VALUES(?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patient_id);
            ps.setInt(2, doctor_id);
            ps.setString(3, appt_datetime);
            ps.setString(4, status);
            ps.setString(5, reason);
            ps.setString(6, notes);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) appt_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Appointment.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean update() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE appointment SET patient_id=?,doctor_id=?,appt_datetime=?,status=?,reason=?,notes=? WHERE appt_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, patient_id);
            ps.setInt(2, doctor_id);
            ps.setString(3, appt_datetime);
            ps.setString(4, status);
            ps.setString(5, reason);
            ps.setString(6, notes);
            ps.setInt(7, appt_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Appointment.update", e.getMessage(), e);
            return false;
        }
    }

    public static ArrayList<Appointment> getAll() {
        ArrayList<Appointment> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.*, p.full_name AS patient_name, d.full_name AS doctor_name " +
                         "FROM appointment a " +
                         "JOIN patient p ON a.patient_id = p.patient_id " +
                         "JOIN doctor d  ON a.doctor_id  = d.doctor_id " +
                         "ORDER BY a.appt_datetime DESC";
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            Logger.log("ERROR", "Appointment.getAll", e.getMessage(), e);
        }
        return list;
    }

    public static ArrayList<Appointment> getByPatient(int patient_id) {
        ArrayList<Appointment> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT a.*, p.full_name AS patient_name, d.full_name AS doctor_name " +
                         "FROM appointment a " +
                         "JOIN patient p ON a.patient_id = p.patient_id " +
                         "JOIN doctor d  ON a.doctor_id  = d.doctor_id " +
                         "WHERE a.patient_id=? ORDER BY a.appt_datetime DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, patient_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            Logger.log("ERROR", "Appointment.getByPatient", e.getMessage(), e);
        }
        return list;
    }

    private static Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.appt_id      = rs.getInt("appt_id");
        a.patient_id   = rs.getInt("patient_id");
        a.doctor_id    = rs.getInt("doctor_id");
        a.appt_datetime = rs.getString("appt_datetime");
        a.status       = rs.getString("status");
        a.reason       = rs.getString("reason");
        a.notes        = rs.getString("notes");
        a.patient_name = rs.getString("patient_name");
        a.doctor_name  = rs.getString("doctor_name");
        return a;
    }
}
