package hms.ui;

import hms.software.DatabaseConnection;
import hms.software.Validator;
import hms.software.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class BillingForm extends JPanel {

    private JComboBox<String> cmbPatient, cmbStatus;
    private JTextField txtTotalAmount, txtPaidAmount, txtSearch;
    private com.toedter.calendar.JDateChooser dateBill;
    private JTable tableBills, tableItems;
    private DefaultTableModel billsModel, itemsModel;
    private JButton btnSave, btnClear, btnDelete, btnAddItem, btnDischarge;
    private int selectedBillId = -1;

    public BillingForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Bill Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 – Patient, Bill Date
        g.gridx = 0; g.gridy = 0; formPanel.add(new JLabel("Patient:*"), g);
        g.gridx = 1; cmbPatient = new JComboBox<>(); loadPatients(); formPanel.add(cmbPatient, g);
        g.gridx = 2; formPanel.add(new JLabel("Bill Date:*"), g);
        g.gridx = 3;
        dateBill = new com.toedter.calendar.JDateChooser();
        dateBill.setDateFormatString("yyyy-MM-dd");
        dateBill.setDate(new java.util.Date());
        formPanel.add(dateBill, g);

        // Row 1 – Total Amount, Paid Amount
        g.gridx = 0; g.gridy = 1; formPanel.add(new JLabel("Total Amount:"), g);
        g.gridx = 1; txtTotalAmount = new JTextField("0.00"); txtTotalAmount.setEditable(false);
        formPanel.add(txtTotalAmount, g);
        g.gridx = 2; formPanel.add(new JLabel("Paid Amount:"), g);
        g.gridx = 3; txtPaidAmount = new JTextField("0.00"); formPanel.add(txtPaidAmount, g);

        // Row 2 – Status
        g.gridx = 0; g.gridy = 2; formPanel.add(new JLabel("Status:"), g);
        g.gridx = 1;
        cmbStatus = new JComboBox<>(new String[]{"pending", "partial", "paid", "waived"});
        formPanel.add(cmbStatus, g);

        // Row 2 – Discharge button
        g.gridx = 2;
        btnDischarge = new JButton("Discharge Patient");
        btnDischarge.setBackground(new Color(255, 140, 0));
        btnDischarge.setForeground(Color.WHITE);
        btnDischarge.setFocusPainted(false);
        formPanel.add(btnDischarge, g);

        // Row 3 – Buttons
        g.gridx = 1; g.gridy = 3;
        btnSave = new JButton("Save Bill");
        btnSave.setBackground(new Color(0, 120, 215));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        formPanel.add(btnSave, g);

        g.gridx = 2;
        btnClear = new JButton("Clear");
        btnClear.setFocusPainted(false);
        formPanel.add(btnClear, g);

        g.gridx = 3;
        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(200, 50, 50));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        formPanel.add(btnDelete, g);

        // Bill items sub-panel
        JPanel itemsPanel = new JPanel(new BorderLayout(4, 4));
        itemsPanel.setBorder(new TitledBorder("Bill Items"));

        JPanel itemFormPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JTextField txtDesc     = new JTextField(14);
        JSpinner   spnQty      = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JTextField txtUnitPrice= new JTextField(8);
        JComboBox<String> cmbItemType = new JComboBox<>(new String[]{
            "consultation", "medicine", "lab_test", "room", "procedure", "other"
        });
        btnAddItem = new JButton("Add Item");
        btnAddItem.setFocusPainted(false);
        itemFormPanel.add(new JLabel("Desc:")); itemFormPanel.add(txtDesc);
        itemFormPanel.add(new JLabel("Qty:"));  itemFormPanel.add(spnQty);
        itemFormPanel.add(new JLabel("Price:")); itemFormPanel.add(txtUnitPrice);
        itemFormPanel.add(new JLabel("Type:"));  itemFormPanel.add(cmbItemType);
        itemFormPanel.add(btnAddItem);
        itemsPanel.add(itemFormPanel, BorderLayout.NORTH);

        String[] itemCols = {"#", "Description", "Qty", "Unit Price", "Amount", "Type"};
        itemsModel = new DefaultTableModel(itemCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tableItems = new JTable(itemsModel);
        tableItems.setRowHeight(24);
        tableItems.getColumnModel().getColumn(0).setMaxWidth(40);
        itemsPanel.add(new JScrollPane(tableItems), BorderLayout.CENTER);

        // Top half: form + items in a split
        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, itemsPanel);
        topSplit.setDividerLocation(180);
        topSplit.setResizeWeight(0.4);

        // Bottom: bills table
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(20);
        JButton btnSearch  = new JButton("Search");
        JButton btnRefresh = new JButton("Show All");
        btnSearch.setFocusPainted(false); btnRefresh.setFocusPainted(false);
        searchPanel.add(txtSearch); searchPanel.add(btnSearch); searchPanel.add(btnRefresh);
        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        String[] billCols = {"ID", "Patient", "Bill Date", "Total", "Paid", "Balance", "Status", "Edit"};
        billsModel = new DefaultTableModel(billCols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 7; }
        };
        tableBills = new JTable(billsModel);
        tableBills.setRowHeight(28);
        tableBills.getColumnModel().getColumn(0).setMaxWidth(40);
        tableBills.getColumnModel().getColumn(7).setMaxWidth(60);

        tableBills.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit"); b.setFont(new Font("SansSerif", Font.PLAIN, 11)); return b;
        });
        tableBills.getColumn("Edit").setCellEditor(
            new PatientForm.ButtonEditor(new JCheckBox(), "Edit", row -> loadForEdit(row))
        );

        JScrollPane scrollBills = new JScrollPane(tableBills);
        scrollBills.setPreferredSize(new Dimension(700, 180));
        bottomPanel.add(scrollBills, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, bottomPanel);
        mainSplit.setDividerLocation(420);
        mainSplit.setResizeWeight(0.6);
        add(mainSplit, BorderLayout.CENTER);

        // Listeners
        btnSave.addActionListener(e -> saveBill());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> deleteBill());
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadTable(""); });
        btnDischarge.addActionListener(e -> dischargePatient());
        btnAddItem.addActionListener(e -> {
            String desc = txtDesc.getText().trim();
            if (desc.isEmpty() || txtUnitPrice.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Description and Unit Price required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedBillId == -1) {
                JOptionPane.showMessageDialog(this, "Save the bill first before adding items.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                double price = Double.parseDouble(txtUnitPrice.getText().trim());
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Unit price cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int qty = (int) spnQty.getValue();
                double amount = qty * price;
                Connection con = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bill_item (bill_id, description, quantity, unit_price, amount, item_type) VALUES (?,?,?,?,?,?)"
                );
                ps.setInt(1, selectedBillId);
                ps.setString(2, desc);
                ps.setInt(3, qty);
                ps.setDouble(4, price);
                ps.setDouble(5, amount);
                ps.setString(6, (String) cmbItemType.getSelectedItem());
                ps.executeUpdate();
                txtDesc.setText(""); txtUnitPrice.setText(""); spnQty.setValue(1);
                loadBillItems(selectedBillId);
                refreshTotalFromDB();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Unit price must be a number.", "Validation", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                Logger.log("ERROR", "BillingForm.addItem", "Failed to add bill item to bill_id=" + selectedBillId, ex);
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void loadPatients() {
        cmbPatient.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(
                "SELECT patient_id, full_name FROM patient ORDER BY full_name"
            ).executeQuery();
            while (rs.next()) {
                cmbPatient.addItem(rs.getInt("patient_id") + " - " + rs.getString("full_name"));
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.loadPatients", "Failed to load patient list", ex);
            JOptionPane.showMessageDialog(this, "Patient load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveBill() {
        if (cmbPatient.getSelectedItem() == null || dateBill.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Patient and Bill Date are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int patientId = Integer.parseInt(((String) cmbPatient.getSelectedItem()).split(" - ")[0]);
        String billDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateBill.getDate());
        double paid = 0;
        try { paid = Double.parseDouble(txtPaidAmount.getText().trim()); }
        catch (NumberFormatException ignored) {}

        boolean isNew = selectedBillId == -1;

        // A new bill always starts with total_amount = 0 (DB default). The
        // chk_paid_lte_total constraint requires paid_amount <= total_amount,
        // so a non-zero paid amount on a brand-new bill (before any items
        // are added) would violate it. Clamp to 0 here and let the user
        // record payment after adding items.
        if (isNew && paid > 0) {
            JOptionPane.showMessageDialog(this,
                "This is a new bill with no items yet, so the paid amount has been reset to 0.\n" +
                "Add bill items first, then update the paid amount.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            paid = 0;
            txtPaidAmount.setText("0.00");
        }

        if (!isNew) {
            double currentTotal = 0;
            try { currentTotal = Double.parseDouble(txtTotalAmount.getText().trim()); }
            catch (NumberFormatException ignored) {}
            if (paid > currentTotal) {
                JOptionPane.showMessageDialog(this,
                    "Paid amount (" + String.format("%.2f", paid) + ") cannot exceed the bill total (" +
                    String.format("%.2f", currentTotal) + ").",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String sql = isNew
            ? "INSERT INTO bill (patient_id, bill_date, paid_amount, status) VALUES (?,?,?,?)"
            : "UPDATE bill SET patient_id=?, bill_date=?, paid_amount=?, status=? WHERE bill_id=?";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patientId);
            ps.setString(2, billDate);
            ps.setDouble(3, paid);
            ps.setString(4, (String) cmbStatus.getSelectedItem());
            if (!isNew) ps.setInt(5, selectedBillId);
            ps.executeUpdate();
            if (isNew) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) selectedBillId = keys.getInt(1);
            }
            JOptionPane.showMessageDialog(this, isNew ? "Bill created." : "Bill updated.");
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.saveBill", "Failed to save bill for patient_id=" + patientId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteBill() {
        if (selectedBillId == -1) {
            JOptionPane.showMessageDialog(this, "Select a bill to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this bill and all its items?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM bill WHERE bill_id = ?");
            ps.setInt(1, selectedBillId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Bill deleted.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.deleteBill", "Failed to delete bill id=" + selectedBillId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void dischargePatient() {
        if (cmbPatient.getSelectedItem() == null) { return; }
        int patientId = Integer.parseInt(((String) cmbPatient.getSelectedItem()).split(" - ")[0]);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            CallableStatement cs = con.prepareCall("{CALL sp_discharge_patient(?, ?)}");
            cs.setInt(1, patientId);
            cs.setString(2, today);
            cs.execute();
            JOptionPane.showMessageDialog(this, "Patient discharged. Bill generated.");
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.dischargePatient", "Failed to discharge patient_id=" + patientId, ex);
            JOptionPane.showMessageDialog(this, "Discharge error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        billsModel.setRowCount(0);
        String sql = search.isEmpty()
            ? "SELECT b.bill_id, p.full_name, b.bill_date, b.total_amount, b.paid_amount, (b.total_amount - b.paid_amount) AS balance, b.status FROM bill b JOIN patient p ON b.patient_id = p.patient_id ORDER BY b.bill_id DESC"
            : "SELECT b.bill_id, p.full_name, b.bill_date, b.total_amount, b.paid_amount, (b.total_amount - b.paid_amount) AS balance, b.status FROM bill b JOIN patient p ON b.patient_id = p.patient_id WHERE p.full_name LIKE ? OR b.status LIKE ? ORDER BY b.bill_id DESC";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            if (!search.isEmpty()) {
                String q = "%" + search + "%";
                ps.setString(1, q); ps.setString(2, q);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                billsModel.addRow(new Object[]{
                    rs.getInt("bill_id"),
                    rs.getString("full_name"),
                    rs.getString("bill_date"),
                    String.format("%.2f", rs.getDouble("total_amount")),
                    String.format("%.2f", rs.getDouble("paid_amount")),
                    String.format("%.2f", rs.getDouble("balance")),
                    rs.getString("status"),
                    "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.loadTable", "Failed to load bill list", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBillItems(int billId) {
        itemsModel.setRowCount(0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT item_id, description, quantity, unit_price, amount, item_type FROM bill_item WHERE bill_id = ?"
            );
            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                itemsModel.addRow(new Object[]{
                    rs.getInt("item_id"),
                    rs.getString("description"),
                    rs.getInt("quantity"),
                    String.format("%.2f", rs.getDouble("unit_price")),
                    String.format("%.2f", rs.getDouble("amount")),
                    rs.getString("item_type")
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.loadBillItems", "Failed to load bill items for bill_id=" + billId, ex);
            JOptionPane.showMessageDialog(this, "Items load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTotalFromDB() {
        if (selectedBillId == -1) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT total_amount FROM bill WHERE bill_id = ?");
            ps.setInt(1, selectedBillId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) txtTotalAmount.setText(String.format("%.2f", rs.getDouble("total_amount")));
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.refreshTotalFromDB", "Failed to refresh total for bill_id=" + selectedBillId, ex);
        }
    }

    private void loadForEdit(int row) {
        selectedBillId = (int) billsModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM bill WHERE bill_id = ?");
            ps.setInt(1, selectedBillId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int pid = rs.getInt("patient_id");
                for (int i = 0; i < cmbPatient.getItemCount(); i++) {
                    if (cmbPatient.getItemAt(i).startsWith(pid + " - ")) { cmbPatient.setSelectedIndex(i); break; }
                }
                java.sql.Date bd = rs.getDate("bill_date");
                if (bd != null) dateBill.setDate(new java.util.Date(bd.getTime()));
                txtTotalAmount.setText(String.format("%.2f", rs.getDouble("total_amount")));
                txtPaidAmount.setText(String.format("%.2f", rs.getDouble("paid_amount")));
                cmbStatus.setSelectedItem(rs.getString("status"));
                btnSave.setText("Update");
                loadBillItems(selectedBillId);
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "BillingForm.loadForEdit", "Failed to load bill id=" + selectedBillId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedBillId = -1;
        if (cmbPatient.getItemCount() > 0) cmbPatient.setSelectedIndex(0);
        dateBill.setDate(new java.util.Date());
        txtTotalAmount.setText("0.00");
        txtPaidAmount.setText("0.00");
        cmbStatus.setSelectedIndex(0);
        itemsModel.setRowCount(0);
        btnSave.setText("Save Bill");
    }
}
