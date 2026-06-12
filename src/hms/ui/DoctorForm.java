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

public class DoctorForm extends JPanel {

    private JTextField txtFullName, txtPhone, txtEmail, txtSearch;
    private JComboBox<String> cmbSpecialization, cmbDept, cmbStatus;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnSave, btnClear, btnDelete;
    private int selectedDoctorId = -1;

    public DoctorForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Doctor Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 – Full Name, Specialization (practical names)
        g.gridx = 0; g.gridy = 0; formPanel.add(new JLabel("Full Name:*"), g);
        g.gridx = 1; txtFullName = new JTextField(15); formPanel.add(txtFullName, g);
        g.gridx = 2; formPanel.add(new JLabel("Specialization:*"), g);
        g.gridx = 3;
        cmbSpecialization = new JComboBox<>(new String[]{
                "General Doctor",
                "Heart Doctor",
                "Brain Doctor",
                "Bone Doctor",
                "Children Doctor",
                "Women's Health Doctor",
                "Skin Doctor",
                "Ear / Nose / Throat Doctor",
                "Eye Doctor",
                "Mind Doctor",
                "X-Ray / Imaging Doctor",
                "Cancer Doctor"
        });
        formPanel.add(cmbSpecialization, g);

        // Row 1 – Phone, Email
        g.gridx = 0; g.gridy = 1; formPanel.add(new JLabel("Phone:*"), g);
        g.gridx = 1; txtPhone = new JTextField(15); formPanel.add(txtPhone, g);
        g.gridx = 2; formPanel.add(new JLabel("Email:"), g);
        g.gridx = 3; txtEmail = new JTextField(15); formPanel.add(txtEmail, g);

        // Row 2 – Department (with + button), Availability
        g.gridx = 0; g.gridy = 2; formPanel.add(new JLabel("Department:*"), g);
        g.gridx = 1;
        JPanel deptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cmbDept = new JComboBox<>();
        loadDepartments();
        deptPanel.add(cmbDept);
        JButton btnNewDept = new JButton("+");
        btnNewDept.setToolTipText("Add new department");
        btnNewDept.setFocusPainted(false);
        btnNewDept.setMargin(new Insets(0, 4, 0, 4));
        btnNewDept.addActionListener(e -> addNewDepartment());
        deptPanel.add(btnNewDept);
        formPanel.add(deptPanel, g);

        g.gridx = 2; formPanel.add(new JLabel("Availability:"), g);
        g.gridx = 3;
        cmbStatus = new JComboBox<>(new String[]{"Available", "Unavailable"});
        formPanel.add(cmbStatus, g);

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
        JButton btnSearch = new JButton("Search");
        btnSearch.setFocusPainted(false);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        JButton btnRefresh = new JButton("Show All");
        btnRefresh.setFocusPainted(false);
        searchPanel.add(btnRefresh);
        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Full Name", "Specialization", "Phone", "Email", "Department", "Available", "Edit"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 7; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(7).setMaxWidth(60);

        table.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit");
            b.setFont(new Font("SansSerif", Font.PLAIN, 11));
            return b;
        });
        table.getColumn("Edit").setCellEditor(
                new PatientForm.ButtonEditor(new JCheckBox(), "Edit", row -> loadForEdit(row))
        );

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 220));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, bottomPanel);
        split.setDividerLocation(230);
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);

        btnSave.addActionListener(e -> saveDoctor());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> deleteDoctor());
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadTable(""); });
    }

    // Load departments alphabetically, no duplicates
    private void loadDepartments() {
        cmbDept.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            // Use GROUP BY to guarantee no duplicates, ORDER BY name
            PreparedStatement ps = con.prepareStatement(
                    "SELECT MIN(dept_id) as dept_id, dept_name FROM department GROUP BY dept_name ORDER BY dept_name ASC"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cmbDept.addItem(rs.getInt("dept_id") + " - " + rs.getString("dept_name"));
            }
            if (cmbDept.getItemCount() == 0) {
                cmbDept.addItem("0 - No departments. Click '+' to add.");
                cmbDept.setEnabled(false);
            } else {
                cmbDept.setEnabled(true);
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "DoctorForm.loadDepartments", "Failed to load departments", ex);
            JOptionPane.showMessageDialog(this, "Dept load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Add a new department dynamically
    private void addNewDepartment() {
        String newDept = JOptionPane.showInputDialog(this, "Enter new department name:", "Add Department", JOptionPane.QUESTION_MESSAGE);
        if (newDept == null || newDept.trim().isEmpty()) return;

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            // Check if already exists (case-insensitive)
            PreparedStatement check = con.prepareStatement("SELECT dept_id FROM department WHERE LOWER(dept_name) = LOWER(?)");
            check.setString(1, newDept.trim());
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Department already exists!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            PreparedStatement insert = con.prepareStatement("INSERT INTO department (dept_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            insert.setString(1, newDept.trim());
            insert.executeUpdate();
            ResultSet keys = insert.getGeneratedKeys();
            int newId = 0;
            if (keys.next()) newId = keys.getInt(1);

            // Reload departments and select the new one
            loadDepartments();
            for (int i = 0; i < cmbDept.getItemCount(); i++) {
                String item = cmbDept.getItemAt(i);
                if (item.startsWith(newId + " - ")) {
                    cmbDept.setSelectedIndex(i);
                    break;
                }
            }
            JOptionPane.showMessageDialog(this, "Department added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            Logger.log("ERROR", "DoctorForm.addNewDepartment", "Failed to add department", ex);
            JOptionPane.showMessageDialog(this, "Error adding department: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveDoctor() {
        String fullName = txtFullName.getText().trim();
        String phone    = txtPhone.getText().trim();
        if (fullName.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full Name and Phone are required.", "Validation", JOptionPane.WARNING_MESSAGE);
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

        if (cmbDept.getItemCount() == 0 || !cmbDept.isEnabled()) {
            JOptionPane.showMessageDialog(this, "Please add a department first (click '+' button).", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String deptStr = (String) cmbDept.getSelectedItem();
        if (deptStr == null || deptStr.contains("No departments")) {
            JOptionPane.showMessageDialog(this, "Please select a valid department.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int deptId = Integer.parseInt(deptStr.split(" - ")[0]);
        boolean isAvail = "Available".equals(cmbStatus.getSelectedItem());

        String sql = selectedDoctorId == -1
                ? "INSERT INTO doctor (full_name, specialization, phone, email, dept_id, is_available) VALUES (?,?,?,?,?,?)"
                : "UPDATE doctor SET full_name=?, specialization=?, phone=?, email=?, dept_id=?, is_available=? WHERE doctor_id=?";

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fullName);
            ps.setString(2, (String) cmbSpecialization.getSelectedItem());
            ps.setString(3, phone);
            ps.setString(4, emailVal.isEmpty() ? null : emailVal);
            ps.setInt(5, deptId);
            ps.setBoolean(6, isAvail);
            if (selectedDoctorId != -1) ps.setInt(7, selectedDoctorId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, selectedDoctorId == -1 ? "Doctor added." : "Doctor updated.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "DoctorForm.saveDoctor", "Failed to save doctor: " + fullName, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteDoctor() {
        if (selectedDoctorId == -1) {
            JOptionPane.showMessageDialog(this, "Select a doctor to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this doctor?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM doctor WHERE doctor_id = ?");
            ps.setInt(1, selectedDoctorId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Doctor deleted.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "DoctorForm.deleteDoctor", "Failed to delete doctor id=" + selectedDoctorId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = search.isEmpty()
                ? "SELECT d.doctor_id, d.full_name, d.specialization, d.phone, d.email, dep.dept_name, d.is_available FROM doctor d JOIN department dep ON d.dept_id = dep.dept_id ORDER BY d.doctor_id DESC"
                : "SELECT d.doctor_id, d.full_name, d.specialization, d.phone, d.email, dep.dept_name, d.is_available FROM doctor d JOIN department dep ON d.dept_id = dep.dept_id WHERE d.full_name LIKE ? OR d.specialization LIKE ? OR d.phone LIKE ? ORDER BY d.doctor_id DESC";
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
                        rs.getInt("doctor_id"),
                        rs.getString("full_name"),
                        rs.getString("specialization"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("dept_name"),
                        rs.getBoolean("is_available") ? "Yes" : "No",
                        "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "DoctorForm.loadTable", "Failed to load doctor list", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForEdit(int row) {
        selectedDoctorId = (int) tableModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM doctor WHERE doctor_id = ?");
            ps.setInt(1, selectedDoctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtFullName.setText(rs.getString("full_name"));
                cmbSpecialization.setSelectedItem(rs.getString("specialization"));
                txtPhone.setText(rs.getString("phone"));
                txtEmail.setText(rs.getString("email") != null ? rs.getString("email") : "");
                cmbStatus.setSelectedItem(rs.getBoolean("is_available") ? "Available" : "Unavailable");
                int deptId = rs.getInt("dept_id");
                for (int i = 0; i < cmbDept.getItemCount(); i++) {
                    if (cmbDept.getItemAt(i).startsWith(deptId + " - ")) {
                        cmbDept.setSelectedIndex(i); break;
                    }
                }
                btnSave.setText("Update");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "DoctorForm.loadForEdit", "Failed to load doctor id=" + selectedDoctorId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedDoctorId = -1;
        txtFullName.setText(""); txtPhone.setText(""); txtEmail.setText("");
        cmbSpecialization.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        if (cmbDept.getItemCount() > 0 && cmbDept.isEnabled()) cmbDept.setSelectedIndex(0);
        btnSave.setText("Save");
    }
}