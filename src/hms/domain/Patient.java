package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Patient {
    public int patient_id;
    public String full_name;
    public String dob;
    public String gender;
    public String blood_group;
    public String phone;
    public String email;
    public String address;
    public String emergency_contact;
    public String admission_date;
    public String discharge_date;
    public int room_id;
    public String status;

    public Patient() {}

    public Patient(int patient_id, String full_name, String dob, String gender,
                   String blood_group, String phone, String email,
                   String address, String emergency_contact, String status) {
        this.patient_id        = patient_id;
        this.full_name         = full_name;
        this.dob               = dob;
        this.gender            = gender;
        this.blood_group       = blood_group;
        this.phone             = phone;
        this.email             = email;
        this.address           = address;
        this.emergency_contact = emergency_contact;
        this.status            = status;
    }

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO patient(full_name,dob,gender,blood_group,phone,email,address,emergency_contact,status) VALUES(?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, full_name);
            ps.setString(2, dob);
            ps.setString(3, gender);
            ps.setString(4, blood_group);
            ps.setString(5, phone);
            ps.setString(6, email);
            ps.setString(7, address);
            ps.setString(8, emergency_contact);
            ps.setString(9, status);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) patient_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Patient.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean update() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE patient SET full_name=?,dob=?,gender=?,blood_group=?,phone=?,email=?,address=?,emergency_contact=?,status=? WHERE patient_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, full_name);
            ps.setString(2, dob);
            ps.setString(3, gender);
            ps.setString(4, blood_group);
            ps.setString(5, phone);
            ps.setString(6, email);
            ps.setString(7, address);
            ps.setString(8, emergency_contact);
            ps.setString(9, status);
            ps.setInt(10, patient_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Patient.update", e.getMessage(), e);
            return false;
        }
    }

    public boolean delete() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM patient WHERE patient_id=?");
            ps.setInt(1, patient_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Patient.delete", e.getMessage(), e);
            return false;
        }
    }

    public static Patient getById(int id) {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM patient WHERE patient_id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            Logger.log("ERROR", "Patient.getById", e.getMessage(), e);
        }
        return null;
    }

    public static ArrayList<Patient> getAll() {
        ArrayList<Patient> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM patient ORDER BY full_name");
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            Logger.log("ERROR", "Patient.getAll", e.getMessage(), e);
        }
        return list;
    }

    public static ArrayList<Patient> search(String keyword) {
        ArrayList<Patient> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT * FROM patient WHERE full_name LIKE ? OR phone LIKE ? OR patient_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, keyword.matches("\\d+") ? keyword : "0");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            Logger.log("ERROR", "Patient.search", e.getMessage(), e);
        }
        return list;
    }

    private static Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.patient_id        = rs.getInt("patient_id");
        p.full_name         = rs.getString("full_name");
        p.dob               = rs.getString("dob");
        p.gender            = rs.getString("gender");
        p.blood_group       = rs.getString("blood_group");
        p.phone             = rs.getString("phone");
        p.email             = rs.getString("email");
        p.address           = rs.getString("address");
        p.emergency_contact = rs.getString("emergency_contact");
        p.admission_date    = rs.getString("admission_date");
        p.discharge_date    = rs.getString("discharge_date");
        p.room_id           = rs.getInt("room_id");
        p.status            = rs.getString("status");
        return p;
    }
}
