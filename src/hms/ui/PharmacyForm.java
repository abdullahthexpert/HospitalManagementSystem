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

public class PharmacyForm extends JPanel {

    // Medicine fields
    private JTextField txtMedName, txtManufacturer, txtUnitPrice, txtSearch;
    private JComboBox<String> cmbMedType;
    private JCheckBox chkRequiresPresc;
    private JTable tableMed;
    private DefaultTableModel medModel;
    private JButton btnSaveMed, btnClearMed, btnDeleteMed, btnDispense;
    private int selectedMedId = -1;

    // Stock fields
    private JComboBox<String> cmbMedStock, cmbSupplier;
    private JTextField txtQty, txtBatchNo;
    private com.toedter.calendar.JDateChooser dateReceived, dateExpiry;
    private JTable tableStock;
    private DefaultTableModel stockModel;
    private JButton btnSaveStock, btnClearStock;

    public PharmacyForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadMedTable("");
        loadStockTable();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        // ── Tab 1: Medicine Catalogue ─────────────────────────────────
        JPanel medTab = new JPanel(new BorderLayout(8, 8));
        medTab.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel medForm = new JPanel(new GridBagLayout());
        medForm.setBorder(new TitledBorder("Medicine Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; medForm.add(new JLabel("Medicine Name:*"), g);
        g.gridx = 1; txtMedName = new JTextField(15); medForm.add(txtMedName, g);
        g.gridx = 2; medForm.add(new JLabel("Type:*"), g);
        g.gridx = 3;
        cmbMedType = new JComboBox<>(new String[]{"tablet", "syrup", "injection", "capsule", "ointment", "drops"});
        medForm.add(cmbMedType, g);

        g.gridx = 0; g.gridy = 1; medForm.add(new JLabel("Manufacturer:"), g);
        g.gridx = 1; txtManufacturer = new JTextField(15); medForm.add(txtManufacturer, g);
        g.gridx = 2; medForm.add(new JLabel("Unit Price (Rs):*"), g);
        g.gridx = 3; txtUnitPrice = new JTextField(10); medForm.add(txtUnitPrice, g);

        g.gridx = 0; g.gridy = 2; medForm.add(new JLabel("Requires Prescription:"), g);
        g.gridx = 1;
        chkRequiresPresc = new JCheckBox("Yes", true);
        medForm.add(chkRequiresPresc, g);

        g.gridx = 1; g.gridy = 3;
        btnSaveMed = new JButton("Save");
        btnSaveMed.setBackground(new Color(0, 120, 215));
        btnSaveMed.setForeground(Color.WHITE);
        btnSaveMed.setFocusPainted(false);
        medForm.add(btnSaveMed, g);
        g.gridx = 2;
        btnClearMed = new JButton("Clear");
        btnClearMed.setFocusPainted(false);
        medForm.add(btnClearMed, g);
        g.gridx = 3;
        btnDeleteMed = new JButton("Delete");
        btnDeleteMed.setBackground(new Color(200, 50, 50));
        btnDeleteMed.setForeground(Color.WHITE);
        btnDeleteMed.setFocusPainted(false);
        medForm.add(btnDeleteMed, g);

        g.gridx = 1; g.gridy = 4;
        btnDispense = new JButton("Dispense Medicine");
        btnDispense.setBackground(new Color(0, 160, 80));
        btnDispense.setForeground(Color.WHITE);
        btnDispense.setFocusPainted(false);
        medForm.add(btnDispense, g);

        JPanel medSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        medSearchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(20);
        JButton btnSearchMed  = new JButton("Search");
        JButton btnRefreshMed = new JButton("Show All");
        JButton btnLowStock   = new JButton("Low Stock Alert");
        btnSearchMed.setFocusPainted(false); btnRefreshMed.setFocusPainted(false); btnLowStock.setFocusPainted(false);
        medSearchPanel.add(txtSearch); medSearchPanel.add(btnSearchMed);
        medSearchPanel.add(btnRefreshMed); medSearchPanel.add(btnLowStock);

        String[] medCols = {"ID", "Name", "Type", "Manufacturer", "Price", "Presc Required", "Stock", "Status", "Edit"};
        medModel = new DefaultTableModel(medCols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 8; }
        };
        tableMed = new JTable(medModel);
        tableMed.setRowHeight(28);
        tableMed.getColumnModel().getColumn(0).setMaxWidth(40);
        tableMed.getColumnModel().getColumn(8).setMaxWidth(60);

        tableMed.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit"); b.setFont(new Font("SansSerif", Font.PLAIN, 11)); return b;
        });
        tableMed.getColumn("Edit").setCellEditor(
            new PatientForm.ButtonEditor(new JCheckBox(), "Edit", row -> loadMedForEdit(row))
        );

        JPanel medBottom = new JPanel(new BorderLayout(4, 4));
        medBottom.add(medSearchPanel, BorderLayout.NORTH);
        medBottom.add(new JScrollPane(tableMed), BorderLayout.CENTER);

        JSplitPane medSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, medForm, medBottom);
        medSplit.setDividerLocation(220);
        medTab.add(medSplit, BorderLayout.CENTER);

        // ── Tab 2: Medicine Stock ─────────────────────────────────────
        JPanel stockTab = new JPanel(new BorderLayout(8, 8));
        stockTab.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel stockForm = new JPanel(new GridBagLayout());
        stockForm.setBorder(new TitledBorder("Add Stock Entry"));
        GridBagConstraints s = new GridBagConstraints();
        s.insets = new Insets(5, 5, 5, 5);
        s.fill = GridBagConstraints.HORIZONTAL;

        s.gridx = 0; s.gridy = 0; stockForm.add(new JLabel("Medicine:*"), s);
        s.gridx = 1; cmbMedStock = new JComboBox<>(); loadMedicineCombo(); stockForm.add(cmbMedStock, s);
        s.gridx = 2; stockForm.add(new JLabel("Supplier:*"), s);
        s.gridx = 3; cmbSupplier = new JComboBox<>(); loadSuppliers(); stockForm.add(cmbSupplier, s);

        s.gridx = 0; s.gridy = 1; stockForm.add(new JLabel("Quantity:*"), s);
        s.gridx = 1; txtQty = new JTextField(); stockForm.add(txtQty, s);
        s.gridx = 2; stockForm.add(new JLabel("Batch No:"), s);
        s.gridx = 3; txtBatchNo = new JTextField(); stockForm.add(txtBatchNo, s);

        s.gridx = 0; s.gridy = 2; stockForm.add(new JLabel("Received Date:*"), s);
        s.gridx = 1;
        dateReceived = new com.toedter.calendar.JDateChooser();
        dateReceived.setDateFormatString("yyyy-MM-dd");
        dateReceived.setDate(new java.util.Date());
        stockForm.add(dateReceived, s);
        s.gridx = 2; stockForm.add(new JLabel("Expiry Date:*"), s);
        s.gridx = 3;
        dateExpiry = new com.toedter.calendar.JDateChooser();
        dateExpiry.setDateFormatString("yyyy-MM-dd");
        stockForm.add(dateExpiry, s);

        s.gridx = 1; s.gridy = 3;
        btnSaveStock = new JButton("Add Stock");
        btnSaveStock.setBackground(new Color(0, 160, 80));
        btnSaveStock.setForeground(Color.WHITE);
        btnSaveStock.setFocusPainted(false);
        stockForm.add(btnSaveStock, s);
        s.gridx = 2;
        btnClearStock = new JButton("Clear");
        btnClearStock.setFocusPainted(false);
        stockForm.add(btnClearStock, s);

        String[] stockCols = {"Stock ID", "Medicine", "Supplier", "Qty", "Expiry Date", "Received", "Batch"};
        stockModel = new DefaultTableModel(stockCols, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tableStock = new JTable(stockModel);
        tableStock.setRowHeight(26);

        JSplitPane stockSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stockForm, new JScrollPane(tableStock));
        stockSplit.setDividerLocation(220);
        stockTab.add(stockSplit, BorderLayout.CENTER);

        tabs.addTab("Medicine Catalogue", medTab);
        tabs.addTab("Stock Management", stockTab);
        add(tabs, BorderLayout.CENTER);

        // Listeners – Medicine
        btnSaveMed.addActionListener(e -> saveMedicine());
        btnClearMed.addActionListener(e -> clearMedForm());
        btnDeleteMed.addActionListener(e -> deleteMedicine());
        btnDispense.addActionListener(e -> dispenseMedicine());
        btnSearchMed.addActionListener(e -> loadMedTable(txtSearch.getText().trim()));
        btnRefreshMed.addActionListener(e -> { txtSearch.setText(""); loadMedTable(""); });
        btnLowStock.addActionListener(e -> loadLowStock());

        // Listeners – Stock
        btnSaveStock.addActionListener(e -> saveStock());
        btnClearStock.addActionListener(e -> clearStockForm());
    }

    // Dispenses a medicine for a prescription: deducts the quantity from the
    // earliest-expiring stock batch(es) and decreases medicine_stock.quantity
    // within a single transaction. If stock is insufficient, the whole
    // operation is rolled back. This is the 3rd transaction example
    // (alongside sp_admit_patient and sp_discharge_patient).
    private void dispenseMedicine() {
        if (selectedMedId == -1) {
            JOptionPane.showMessageDialog(this, "Click Edit on a medicine row first to select it.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this, "Quantity to dispense:", "Dispense Medicine", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;
        int qtyToDispense;
        try {
            qtyToDispense = Integer.parseInt(qtyStr.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be an integer.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (qtyToDispense <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getInstance().getConnection();
            con.setAutoCommit(false);

            // Lock and fetch stock batches for this medicine, earliest-expiry first (FEFO)
            PreparedStatement lockPs = con.prepareStatement(
                "SELECT stock_id, quantity FROM medicine_stock WHERE medicine_id = ? AND quantity > 0 ORDER BY expiry_date ASC FOR UPDATE"
            );
            lockPs.setInt(1, selectedMedId);
            ResultSet rs = lockPs.executeQuery();

            int remaining = qtyToDispense;
            java.util.List<int[]> updates = new java.util.ArrayList<>(); // [stock_id, newQty]
            while (rs.next() && remaining > 0) {
                int stockId = rs.getInt("stock_id");
                int available = rs.getInt("quantity");
                int take = Math.min(available, remaining);
                updates.add(new int[]{stockId, available - take});
                remaining -= take;
            }

            if (remaining > 0) {
                // Not enough stock available — roll back, nothing is changed
                con.rollback();
                JOptionPane.showMessageDialog(this,
                    "Insufficient stock. Could not dispense " + qtyToDispense + " unit(s); short by " + remaining + ".",
                    "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }

            PreparedStatement updatePs = con.prepareStatement("UPDATE medicine_stock SET quantity = ? WHERE stock_id = ?");
            for (int[] u : updates) {
                updatePs.setInt(1, u[1]);
                updatePs.setInt(2, u[0]);
                updatePs.executeUpdate();
            }

            con.commit();
            JOptionPane.showMessageDialog(this, "Dispensed " + qtyToDispense + " unit(s) successfully.");
            loadMedTable("");
            loadStockTable();
        } catch (SQLException ex) {
            try { if (con != null) con.rollback(); } catch (SQLException ignored) {}
            Logger.log("ERROR", "PharmacyForm.dispenseMedicine", "Failed to dispense medicine_id=" + selectedMedId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private void loadMedicineCombo() {
        cmbMedStock.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT medicine_id, name FROM medicine ORDER BY name").executeQuery();
            while (rs.next()) cmbMedStock.addItem(rs.getInt("medicine_id") + " - " + rs.getString("name"));
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.loadMedicineCombo", "Failed to load medicine list", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSuppliers() {
        cmbSupplier.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT supplier_id, name FROM supplier ORDER BY name").executeQuery();
            while (rs.next()) cmbSupplier.addItem(rs.getInt("supplier_id") + " - " + rs.getString("name"));
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.loadSuppliers", "Failed to load supplier list", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveMedicine() {
        String name = txtMedName.getText().trim();
        if (name.isEmpty() || txtUnitPrice.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Unit Price are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double price;
        try { price = Double.parseDouble(txtUnitPrice.getText().trim()); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Unit price must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        if (price < 0) {
            JOptionPane.showMessageDialog(this, "Unit price cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = selectedMedId == -1
            ? "INSERT INTO medicine (name, type, manufacturer, unit_price, requires_presc) VALUES (?,?,?,?,?)"
            : "UPDATE medicine SET name=?, type=?, manufacturer=?, unit_price=?, requires_presc=? WHERE medicine_id=?";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, (String) cmbMedType.getSelectedItem());
            ps.setString(3, txtManufacturer.getText().trim());
            ps.setDouble(4, price);
            ps.setBoolean(5, chkRequiresPresc.isSelected());
            if (selectedMedId != -1) ps.setInt(6, selectedMedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, selectedMedId == -1 ? "Medicine added." : "Medicine updated.");
            clearMedForm();
            loadMedTable("");
            loadMedicineCombo();
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.saveMedicine", "Failed to save medicine: " + name, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteMedicine() {
        if (selectedMedId == -1) { JOptionPane.showMessageDialog(this, "Select a medicine to delete.", "No Selection", JOptionPane.WARNING_MESSAGE); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this medicine?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM medicine WHERE medicine_id = ?");
            ps.setInt(1, selectedMedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Medicine deleted.");
            clearMedForm();
            loadMedTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.deleteMedicine", "Failed to delete medicine id=" + selectedMedId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveStock() {
        if (cmbMedStock.getSelectedItem() == null || cmbSupplier.getSelectedItem() == null || dateExpiry.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Medicine, Supplier and Expiry Date are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dateReceived.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Received Date is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int qty;
        try { qty = Integer.parseInt(txtQty.getText().trim()); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Quantity must be an integer.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        if (qty < 0) {
            JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // chk_expiry CHECK (expiry_date > received_date) — validate client-side
        // so the user gets a friendly message instead of a raw SQL error.
        if (!dateExpiry.getDate().after(dateReceived.getDate())) {
            JOptionPane.showMessageDialog(this, "Expiry date must be after the received date.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int medId  = Integer.parseInt(((String) cmbMedStock.getSelectedItem()).split(" - ")[0]);
        int supId  = Integer.parseInt(((String) cmbSupplier.getSelectedItem()).split(" - ")[0]);
        String recvDate  = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateReceived.getDate());
        String expiryDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateExpiry.getDate());

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO medicine_stock (medicine_id, supplier_id, quantity, expiry_date, received_date, batch_no) VALUES (?,?,?,?,?,?)"
            );
            ps.setInt(1, medId); ps.setInt(2, supId);
            ps.setInt(3, qty);
            ps.setString(4, expiryDate);
            ps.setString(5, recvDate);
            ps.setString(6, txtBatchNo.getText().trim());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Stock entry added.");
            clearStockForm();
            loadStockTable();
            loadMedTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.saveStock", "Failed to add stock entry for medicine_id=" + medId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadMedTable(String search) {
        medModel.setRowCount(0);
        String sql = search.isEmpty()
            ? "SELECT m.medicine_id, m.name, m.type, m.manufacturer, m.unit_price, m.requires_presc, COALESCE(SUM(ms.quantity),0) AS total_stock, CASE WHEN COALESCE(SUM(ms.quantity),0) < 20 THEN 'LOW' ELSE 'OK' END AS stock_status FROM medicine m LEFT JOIN medicine_stock ms ON m.medicine_id = ms.medicine_id GROUP BY m.medicine_id ORDER BY m.name"
            : "SELECT m.medicine_id, m.name, m.type, m.manufacturer, m.unit_price, m.requires_presc, COALESCE(SUM(ms.quantity),0) AS total_stock, CASE WHEN COALESCE(SUM(ms.quantity),0) < 20 THEN 'LOW' ELSE 'OK' END AS stock_status FROM medicine m LEFT JOIN medicine_stock ms ON m.medicine_id = ms.medicine_id WHERE m.name LIKE ? OR m.type LIKE ? OR m.manufacturer LIKE ? GROUP BY m.medicine_id ORDER BY m.name";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            if (!search.isEmpty()) {
                String q = "%" + search + "%";
                ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                medModel.addRow(new Object[]{
                    rs.getInt("medicine_id"), rs.getString("name"), rs.getString("type"),
                    rs.getString("manufacturer"), String.format("%.2f", rs.getDouble("unit_price")),
                    rs.getBoolean("requires_presc") ? "Yes" : "No",
                    rs.getInt("total_stock"), rs.getString("stock_status"), "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.loadMedTable", "Failed to load medicine catalogue", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLowStock() {
        medModel.setRowCount(0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(
                "SELECT m.medicine_id, m.name, m.type, m.manufacturer, m.unit_price, m.requires_presc, COALESCE(SUM(ms.quantity),0) AS total_stock, 'LOW' AS stock_status FROM medicine m LEFT JOIN medicine_stock ms ON m.medicine_id = ms.medicine_id GROUP BY m.medicine_id HAVING total_stock < 20 ORDER BY total_stock"
            ).executeQuery();
            while (rs.next()) {
                medModel.addRow(new Object[]{
                    rs.getInt("medicine_id"), rs.getString("name"), rs.getString("type"),
                    rs.getString("manufacturer"), String.format("%.2f", rs.getDouble("unit_price")),
                    rs.getBoolean("requires_presc") ? "Yes" : "No",
                    rs.getInt("total_stock"), "LOW", "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.loadLowStock", "Failed to load low-stock medicines", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStockTable() {
        stockModel.setRowCount(0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(
                "SELECT ms.stock_id, m.name AS medicine, s.name AS supplier, ms.quantity, ms.expiry_date, ms.received_date, ms.batch_no FROM medicine_stock ms JOIN medicine m ON ms.medicine_id = m.medicine_id JOIN supplier s ON ms.supplier_id = s.supplier_id ORDER BY ms.expiry_date"
            ).executeQuery();
            while (rs.next()) {
                stockModel.addRow(new Object[]{
                    rs.getInt("stock_id"), rs.getString("medicine"), rs.getString("supplier"),
                    rs.getInt("quantity"), rs.getString("expiry_date"),
                    rs.getString("received_date"), rs.getString("batch_no")
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.loadStockTable", "Failed to load stock entries", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadMedForEdit(int row) {
        selectedMedId = (int) medModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM medicine WHERE medicine_id = ?");
            ps.setInt(1, selectedMedId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtMedName.setText(rs.getString("name"));
                cmbMedType.setSelectedItem(rs.getString("type"));
                txtManufacturer.setText(rs.getString("manufacturer") != null ? rs.getString("manufacturer") : "");
                txtUnitPrice.setText(String.format("%.2f", rs.getDouble("unit_price")));
                chkRequiresPresc.setSelected(rs.getBoolean("requires_presc"));
                btnSaveMed.setText("Update");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PharmacyForm.loadMedForEdit", "Failed to load medicine id=" + selectedMedId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearMedForm() {
        selectedMedId = -1;
        txtMedName.setText(""); txtManufacturer.setText(""); txtUnitPrice.setText("");
        cmbMedType.setSelectedIndex(0); chkRequiresPresc.setSelected(true);
        btnSaveMed.setText("Save");
    }

    private void clearStockForm() {
        if (cmbMedStock.getItemCount() > 0) cmbMedStock.setSelectedIndex(0);
        if (cmbSupplier.getItemCount() > 0) cmbSupplier.setSelectedIndex(0);
        txtQty.setText(""); txtBatchNo.setText("");
        dateReceived.setDate(new java.util.Date());
        dateExpiry.setDate(null);
    }
}
