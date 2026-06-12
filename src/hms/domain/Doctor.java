package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Doctor {
    public int doctor_id;
    public String full_name;
    public String specialization;
    public String phone;
    public String email;
    public int dept_id;
    public int user_id;
    public boolean is_available;

    public Doctor() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO doctor(full_name,specialization,phone,email,dept_id,user_id,is_available) VALUES(?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, full_name);
            ps.setString(2, specialization);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setInt(5, dept_id);
            ps.setInt(6, user_id);
            ps.setBoolean(7, is_available);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) doctor_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Doctor.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean update() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE doctor SET full_name=?,specialization=?,phone=?,email=?,dept_id=?,is_available=? WHERE doctor_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, full_name);
            ps.setString(2, specialization);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setInt(5, dept_id);
            ps.setBoolean(6, is_available);
            ps.setInt(7, doctor_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Doctor.update", e.getMessage(), e);
            return false;
        }
    }

    public boolean delete() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM doctor WHERE doctor_id=?");
            ps.setInt(1, doctor_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Doctor.delete", e.getMessage(), e);
            return false;
        }
    }

    public static ArrayList<Doctor> getAll() {
        ArrayList<Doctor> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM doctor ORDER BY full_name");
            while (rs.next()) {
                Doctor d = new Doctor();
                d.doctor_id      = rs.getInt("doctor_id");
                d.full_name      = rs.getString("full_name");
                d.specialization = rs.getString("specialization");
                d.phone          = rs.getString("phone");
                d.email          = rs.getString("email");
                d.dept_id        = rs.getInt("dept_id");
                d.user_id        = rs.getInt("user_id");
                d.is_available   = rs.getBoolean("is_available");
                list.add(d);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Doctor.getAll", e.getMessage(), e);
        }
        return list;
    }

    public static ArrayList<Doctor> getByDepartment(int dept_id) {
        ArrayList<Doctor> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM doctor WHERE dept_id=? AND is_available=1");
            ps.setInt(1, dept_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Doctor d = new Doctor();
                d.doctor_id      = rs.getInt("doctor_id");
                d.full_name      = rs.getString("full_name");
                d.specialization = rs.getString("specialization");
                d.dept_id        = dept_id;
                d.is_available   = true;
                list.add(d);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Doctor.getByDepartment", e.getMessage(), e);
        }
        return list;
    }
}
