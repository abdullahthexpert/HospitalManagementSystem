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
import java.text.SimpleDateFormat;

public class StaffForm extends JPanel {

    private JTextField txtFullName, txtPhone, txtEmail, txtSearch;
    private JComboBox<String> cmbRole, cmbDept;
    private com.toedter.calendar.JDateChooser dateHire;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnSave, btnClear, btnDelete;
    private JLabel lblTotal;
    private int selectedStaffId = -1;

    public StaffForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Staff Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 – Full Name, Role
        g.gridx = 0; g.gridy = 0; formPanel.add(new JLabel("Full Name:*"), g);
        g.gridx = 1; txtFullName = new JTextField(15); formPanel.add(txtFullName, g);
        g.gridx = 2; formPanel.add(new JLabel("Role:*"), g);
        g.gridx = 3;
        cmbRole = new JComboBox<>(new String[]{"nurse", "lab_technician", "receptionist", "pharmacist", "cleaner"});
        formPanel.add(cmbRole, g);

        // Row 1 – Phone, Email
        g.gridx = 0; g.gridy = 1; formPanel.add(new JLabel("Phone:*"), g);
        g.gridx = 1; txtPhone = new JTextField(15); formPanel.add(txtPhone, g);
        g.gridx = 2; formPanel.add(new JLabel("Email:"), g);
        g.gridx = 3; txtEmail = new JTextField(15); formPanel.add(txtEmail, g);

        // Row 2 – Department, Hire Date
        g.gridx = 0; g.gridy = 2; formPanel.add(new JLabel("Department:*"), g);
        g.gridx = 1; cmbDept = new JComboBox<>(); loadDepartments(); formPanel.add(cmbDept, g);
        g.gridx = 2; formPanel.add(new JLabel("Hire Date:*"), g);
        g.gridx = 3;
        dateHire = new com.toedter.calendar.JDateChooser();
        dateHire.setDateFormatString("yyyy-MM-dd");
        dateHire.setDate(new java.util.Date());
        formPanel.add(dateHire, g);

        // Row 3 – Buttons
        g.gridx = 1; g.gridy = 3;
        btnSave = new JButton("Save");
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

        // Bottom – search + table
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(20);
        JButton btnSearch  = new JButton("Search");
        JButton btnRefresh = new JButton("Show All");
        JComboBox<String> cmbFilterRole = new JComboBox<>(new String[]{"All Roles", "nurse", "lab_technician", "receptionist", "pharmacist", "cleaner"});
        JButton btnFilterRole = new JButton("Filter");
        btnSearch.setFocusPainted(false); btnRefresh.setFocusPainted(false); btnFilterRole.setFocusPainted(false);
        searchPanel.add(txtSearch); searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);
        searchPanel.add(new JLabel("Role:"));
        searchPanel.add(cmbFilterRole); searchPanel.add(btnFilterRole);
        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Full Name", "Role", "Phone", "Email", "Department", "Hire Date", "Edit"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 7; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(7).setMaxWidth(60);

        table.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit"); b.setFont(new Font("SansSerif", Font.PLAIN, 11)); return b;
        });
        table.getColumn("Edit").setCellEditor(
            new PatientForm.ButtonEditor(new JCheckBox(), "Edit", row -> loadForEdit(row))
        );

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 220));
        bottomPanel.add(scroll, BorderLayout.CENTER);

        // Stats bar
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        statsPanel.setBorder(BorderFactory.createEtchedBorder());
        lblTotal = new JLabel("Total Staff: ...");
        lblTotal.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statsPanel.add(lblTotal);
        bottomPanel.add(statsPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, bottomPanel);
        split.setDividerLocation(220);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        btnSave.addActionListener(e -> saveStaff());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> deleteStaff());
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadTable(""); });
        btnFilterRole.addActionListener(e -> {
            String role = (String) cmbFilterRole.getSelectedItem();
            if ("All Roles".equals(role)) loadTable(""); else loadByRole(role);
        });

        refreshStats();
    }

    // Refreshes the "Total Staff" counter shown at the bottom of the form.
    // Called on load and after any add/delete so the count stays accurate.
    private void refreshStats() {
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT COUNT(*) FROM staff").executeQuery();
            if (rs.next()) lblTotal.setText("Total Staff: " + rs.getInt(1));
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.refreshStats", "Failed to refresh staff count", ex);
        }
    }

    private void loadDepartments() {
        cmbDept.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT dept_id, dept_name FROM department ORDER BY dept_name").executeQuery();
            while (rs.next()) cmbDept.addItem(rs.getInt("dept_id") + " - " + rs.getString("dept_name"));
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.loadDepartments", "Failed to load departments", ex);
            JOptionPane.showMessageDialog(this, "Dept load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveStaff() {
        String fullName = txtFullName.getText().trim();
        String phone    = txtPhone.getText().trim();
        if (fullName.isEmpty() || phone.isEmpty() || dateHire.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Full Name, Phone and Hire Date are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Validator.isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Phone must be 10-15 digits, numbers only.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String emailVal = txtEmail.getText().trim();
        if (!emailVal.isEmpty() && !Validator.isValidEmail(emailVal)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String deptStr = (String) cmbDept.getSelectedItem();
        if (deptStr == null) { JOptionPane.showMessageDialog(this, "No department found.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        int deptId = Integer.parseInt(deptStr.split(" - ")[0]);
        String hireDate = new SimpleDateFormat("yyyy-MM-dd").format(dateHire.getDate());

        String sql = selectedStaffId == -1
            ? "INSERT INTO staff (full_name, role, phone, email, dept_id, hire_date) VALUES (?,?,?,?,?,?)"
            : "UPDATE staff SET full_name=?, role=?, phone=?, email=?, dept_id=?, hire_date=? WHERE staff_id=?";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fullName);
            ps.setString(2, (String) cmbRole.getSelectedItem());
            ps.setString(3, phone);
            ps.setString(4, emailVal.isEmpty() ? null : emailVal);
            ps.setInt(5, deptId);
            ps.setString(6, hireDate);
            if (selectedStaffId != -1) ps.setInt(7, selectedStaffId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, selectedStaffId == -1 ? "Staff added." : "Staff updated.");
            clearForm();
            loadTable("");
            refreshStats();
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.saveStaff", "Failed to save staff: " + fullName, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteStaff() {
        if (selectedStaffId == -1) { JOptionPane.showMessageDialog(this, "Select a staff member to delete.", "No Selection", JOptionPane.WARNING_MESSAGE); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this staff member?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM staff WHERE staff_id = ?");
            ps.setInt(1, selectedStaffId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Staff deleted.");
            clearForm();
            loadTable("");
            refreshStats();
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.deleteStaff", "Failed to delete staff id=" + selectedStaffId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = search.isEmpty()
            ? "SELECT s.staff_id, s.full_name, s.role, s.phone, s.email, d.dept_name, s.hire_date FROM staff s JOIN department d ON s.dept_id = d.dept_id ORDER BY s.staff_id DESC"
            : "SELECT s.staff_id, s.full_name, s.role, s.phone, s.email, d.dept_name, s.hire_date FROM staff s JOIN department d ON s.dept_id = d.dept_id WHERE s.full_name LIKE ? OR s.role LIKE ? OR s.phone LIKE ? ORDER BY s.staff_id DESC";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            if (!search.isEmpty()) {
                String q = "%" + search + "%";
                ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("staff_id"), rs.getString("full_name"), rs.getString("role"),
                    rs.getString("phone"), rs.getString("email"),
                    rs.getString("dept_name"), rs.getString("hire_date"), "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.loadTable", "Failed to load staff list", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadByRole(String role) {
        tableModel.setRowCount(0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT s.staff_id, s.full_name, s.role, s.phone, s.email, d.dept_name, s.hire_date FROM staff s JOIN department d ON s.dept_id = d.dept_id WHERE s.role = ? ORDER BY s.full_name"
            );
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("staff_id"), rs.getString("full_name"), rs.getString("role"),
                    rs.getString("phone"), rs.getString("email"),
                    rs.getString("dept_name"), rs.getString("hire_date"), "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.loadByRole", "Failed to load staff by role=" + role, ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForEdit(int row) {
        selectedStaffId = (int) tableModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM staff WHERE staff_id = ?");
            ps.setInt(1, selectedStaffId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtFullName.setText(rs.getString("full_name"));
                cmbRole.setSelectedItem(rs.getString("role"));
                txtPhone.setText(rs.getString("phone"));
                txtEmail.setText(rs.getString("email") != null ? rs.getString("email") : "");
                int deptId = rs.getInt("dept_id");
                for (int i = 0; i < cmbDept.getItemCount(); i++) {
                    if (cmbDept.getItemAt(i).startsWith(deptId + " - ")) { cmbDept.setSelectedIndex(i); break; }
                }
                java.sql.Date hd = rs.getDate("hire_date");
                if (hd != null) dateHire.setDate(new java.util.Date(hd.getTime()));
                btnSave.setText("Update");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "StaffForm.loadForEdit", "Failed to load staff id=" + selectedStaffId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedStaffId = -1;
        txtFullName.setText(""); txtPhone.setText(""); txtEmail.setText("");
        cmbRole.setSelectedIndex(0);
        if (cmbDept.getItemCount() > 0) cmbDept.setSelectedIndex(0);
        dateHire.setDate(new java.util.Date());
        btnSave.setText("Save");
    }
}
