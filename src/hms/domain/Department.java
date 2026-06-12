package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Department {
    public int dept_id;
    public String dept_name;
    public String description;
    public int head_doctor_id;

    public Department() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO department(dept_name,description) VALUES(?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dept_name);
            ps.setString(2, description);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) dept_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Department.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean update() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "UPDATE department SET dept_name=?,description=?,head_doctor_id=? WHERE dept_id=?");
            ps.setString(1, dept_name);
            ps.setString(2, description);
            ps.setInt(3, head_doctor_id);
            ps.setInt(4, dept_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Department.update", e.getMessage(), e);
            return false;
        }
    }

    public boolean delete() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM department WHERE dept_id=?");
            ps.setInt(1, dept_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Department.delete", e.getMessage(), e);
            return false;
        }
    }

    public static ArrayList<Department> getAll() {
        ArrayList<Department> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM department ORDER BY dept_name");
            while (rs.next()) {
                Department d = new Department();
                d.dept_id       = rs.getInt("dept_id");
                d.dept_name     = rs.getString("dept_name");
                d.description   = rs.getString("description");
                d.head_doctor_id = rs.getInt("head_doctor_id");
                list.add(d);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Department.getAll", e.getMessage(), e);
        }
        return list;
    }
}
