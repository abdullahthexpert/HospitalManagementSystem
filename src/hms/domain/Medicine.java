package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Medicine {
    public int medicine_id;
    public String name;
    public String type;
    public String manufacturer;
    public double unit_price;
    public boolean requires_presc;

    public Medicine() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO medicine(name,type,manufacturer,unit_price,requires_presc) VALUES(?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, manufacturer);
            ps.setDouble(4, unit_price);
            ps.setBoolean(5, requires_presc);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) medicine_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Medicine.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean update() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE medicine SET name=?,type=?,manufacturer=?,unit_price=?,requires_presc=? WHERE medicine_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, manufacturer);
            ps.setDouble(4, unit_price);
            ps.setBoolean(5, requires_presc);
            ps.setInt(6, medicine_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Medicine.update", e.getMessage(), e);
            return false;
        }
    }

    public boolean delete() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM medicine WHERE medicine_id=?");
            ps.setInt(1, medicine_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Medicine.delete", e.getMessage(), e);
            return false;
        }
    }

    public static ArrayList<Medicine> getAll() {
        ArrayList<Medicine> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM medicine ORDER BY name");
            while (rs.next()) {
                Medicine m = new Medicine();
                m.medicine_id   = rs.getInt("medicine_id");
                m.name          = rs.getString("name");
                m.type          = rs.getString("type");
                m.manufacturer  = rs.getString("manufacturer");
                m.unit_price    = rs.getDouble("unit_price");
                m.requires_presc = rs.getBoolean("requires_presc");
                list.add(m);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Medicine.getAll", e.getMessage(), e);
        }
        return list;
    }

    public static ArrayList<Medicine> search(String keyword) {
        ArrayList<Medicine> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM medicine WHERE name LIKE ? OR manufacturer LIKE ?");
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Medicine m = new Medicine();
                m.medicine_id  = rs.getInt("medicine_id");
                m.name         = rs.getString("name");
                m.type         = rs.getString("type");
                m.unit_price   = rs.getDouble("unit_price");
                list.add(m);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Medicine.search", e.getMessage(), e);
        }
        return list;
    }
}
