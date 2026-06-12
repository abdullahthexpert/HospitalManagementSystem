package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Prescription {
    public int presc_id;
    public int appt_id;
    public String issue_date;
    public String diagnosis;
    public String instructions;

    public Prescription() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO prescription(appt_id,issue_date,diagnosis,instructions) VALUES(?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, appt_id);
            ps.setString(2, issue_date);
            ps.setString(3, diagnosis);
            ps.setString(4, instructions);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) presc_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Prescription.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean update() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "UPDATE prescription SET diagnosis=?,instructions=? WHERE presc_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, diagnosis);
            ps.setString(2, instructions);
            ps.setInt(3, presc_id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Prescription.update", e.getMessage(), e);
            return false;
        }
    }

    public static Prescription getByAppointment(int appt_id) {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM prescription WHERE appt_id=?");
            ps.setInt(1, appt_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Prescription p = new Prescription();
                p.presc_id    = rs.getInt("presc_id");
                p.appt_id     = rs.getInt("appt_id");
                p.issue_date  = rs.getString("issue_date");
                p.diagnosis   = rs.getString("diagnosis");
                p.instructions = rs.getString("instructions");
                return p;
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Prescription.getByAppointment", e.getMessage(), e);
        }
        return null;
    }
}
