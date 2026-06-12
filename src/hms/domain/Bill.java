package hms.domain;

import java.sql.*;
import java.util.ArrayList;
import hms.software.DatabaseConnection;
import hms.software.Logger;

public class Bill {
    public int bill_id;
    public int patient_id;
    public String bill_date;
    public double total_amount;
    public double paid_amount;
    public String status;
    // For display
    public String patient_name;

    public Bill() {}

    public boolean save() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO bill(patient_id,bill_date,total_amount,paid_amount,status) VALUES(?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patient_id);
            ps.setString(2, bill_date);
            ps.setDouble(3, total_amount);
            ps.setDouble(4, paid_amount);
            ps.setString(5, status);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) bill_id = rs.getInt(1);
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Bill.save", e.getMessage(), e);
            return false;
        }
    }

    public boolean updatePayment(double amount_paid) {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            con.setAutoCommit(false);
            try {
                PreparedStatement ps = con.prepareStatement(
                    "UPDATE bill SET paid_amount = paid_amount + ?, status = CASE WHEN paid_amount + ? >= total_amount THEN 'paid' ELSE 'partial' END WHERE bill_id=?");
                ps.setDouble(1, amount_paid);
                ps.setDouble(2, amount_paid);
                ps.setInt(3, bill_id);
                ps.executeUpdate();
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Bill.updatePayment", e.getMessage(), e);
            return false;
        }
    }

    public static ArrayList<Bill> getAll() {
        ArrayList<Bill> list = new ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT b.*, p.full_name AS patient_name FROM bill b JOIN patient p ON b.patient_id=p.patient_id ORDER BY b.bill_date DESC";
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                Bill b = new Bill();
                b.bill_id      = rs.getInt("bill_id");
                b.patient_id   = rs.getInt("patient_id");
                b.bill_date    = rs.getString("bill_date");
                b.total_amount = rs.getDouble("total_amount");
                b.paid_amount  = rs.getDouble("paid_amount");
                b.status       = rs.getString("status");
                b.patient_name = rs.getString("patient_name");
                list.add(b);
            }
        } catch (SQLException e) {
            Logger.log("ERROR", "Bill.getAll", e.getMessage(), e);
        }
        return list;
    }

    public boolean addItem(String description, int quantity, double unit_price, String item_type) {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "INSERT INTO bill_item(bill_id,description,quantity,unit_price,amount,item_type) VALUES(?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, bill_id);
            ps.setString(2, description);
            ps.setInt(3, quantity);
            ps.setDouble(4, unit_price);
            ps.setDouble(5, quantity * unit_price);
            ps.setString(6, item_type);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.log("ERROR", "Bill.addItem", e.getMessage(), e);
            return false;
        }
    }
}
