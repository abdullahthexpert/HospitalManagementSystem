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
import java.util.function.IntConsumer;

public class PatientForm extends JPanel {

    private JTextField txtFullName, txtPhone, txtEmail, txtAddress, txtEmergencyContact, txtSearch;
    private JRadioButton rbMale, rbFemale, rbOther;
    private JComboBox<String> cmbBloodGroup, cmbStatus;
    private JSpinner spnDOB;
    private JTextArea txtNotes;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnSave, btnClear, btnDelete, btnAdmit;
    private int selectedPatientId = -1;

    public PatientForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Patient Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 – Full Name
        g.gridx = 0; g.gridy = 0; formPanel.add(new JLabel("Full Name:*"), g);
        g.gridx = 1; g.gridwidth = 3; txtFullName = new JTextField(); formPanel.add(txtFullName, g);
        g.gridwidth = 1;

        // Row 1 – Phone, Email
        g.gridx = 0; g.gridy = 1; formPanel.add(new JLabel("Phone:*"), g);
        g.gridx = 1; txtPhone = new JTextField(15); formPanel.add(txtPhone, g);
        g.gridx = 2; formPanel.add(new JLabel("Email:"), g);
        g.gridx = 3; txtEmail = new JTextField(15); formPanel.add(txtEmail, g);

        // Row 2 – DOB, Blood Group
        g.gridx = 0; g.gridy = 2; formPanel.add(new JLabel("Date of Birth:*"), g);
        g.gridx = 1;
        SpinnerDateModel dateModel = new SpinnerDateModel();
        spnDOB = new JSpinner(dateModel);
        spnDOB.setEditor(new JSpinner.DateEditor(spnDOB, "yyyy-MM-dd"));
        formPanel.add(spnDOB, g);
        g.gridx = 2; formPanel.add(new JLabel("Blood Group:"), g);
        g.gridx = 3; cmbBloodGroup = new JComboBox<>(new String[]{"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        formPanel.add(cmbBloodGroup, g);

        // Row 3 – Gender, Status
        g.gridx = 0; g.gridy = 3; formPanel.add(new JLabel("Gender:*"), g);
        g.gridx = 1;
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ButtonGroup bg = new ButtonGroup();
        rbMale   = new JRadioButton("Male");
        rbFemale = new JRadioButton("Female");
        rbOther  = new JRadioButton("Other");
        rbMale.setSelected(true);
        bg.add(rbMale); bg.add(rbFemale); bg.add(rbOther);
        genderPanel.add(rbMale); genderPanel.add(rbFemale); genderPanel.add(rbOther);
        formPanel.add(genderPanel, g);
        g.gridx = 2; formPanel.add(new JLabel("Status:"), g);
        g.gridx = 3; cmbStatus = new JComboBox<>(new String[]{"outpatient", "admitted", "discharged"});
        formPanel.add(cmbStatus, g);

        // Row 4 – Address
        g.gridx = 0; g.gridy = 4; formPanel.add(new JLabel("Address:"), g);
        g.gridx = 1; g.gridwidth = 3; txtAddress = new JTextField(); formPanel.add(txtAddress, g);
        g.gridwidth = 1;

        // Row 5 – Emergency Contact
        g.gridx = 0; g.gridy = 5; formPanel.add(new JLabel("Emergency Contact:"), g);
        g.gridx = 1; txtEmergencyContact = new JTextField(15); formPanel.add(txtEmergencyContact, g);

        // Row 6 – Buttons
        g.gridx = 1; g.gridy = 6;
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

        g.gridx = 4;
        btnAdmit = new JButton("Admit to Room");
        btnAdmit.setBackground(new Color(0, 160, 80));
        btnAdmit.setForeground(Color.WHITE);
        btnAdmit.setFocusPainted(false);
        formPanel.add(btnAdmit, g);

        // Bottom: search + table
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(20);
        JButton btnSearch  = new JButton("Search");
        JButton btnRefresh = new JButton("Show All");
        btnSearch.setFocusPainted(false);
        btnRefresh.setFocusPainted(false);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);
        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Full Name", "DOB", "Phone", "Gender", "Blood Group", "Status", "Edit"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 7; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(7).setMaxWidth(60);

        table.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit");
            b.setFont(new Font("SansSerif", Font.PLAIN, 11));
            return b;
        });
        table.getColumn("Edit").setCellEditor(new ButtonEditor(new JCheckBox(), "Edit", this::loadForEdit));

        bottomPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, bottomPanel);
        split.setDividerLocation(290);
        add(split, BorderLayout.CENTER);

        btnSave.addActionListener(e    -> savePatient());
        btnClear.addActionListener(e   -> clearForm());
        btnDelete.addActionListener(e  -> deletePatient());
        btnAdmit.addActionListener(e   -> admitPatient());
        btnSearch.addActionListener(e  -> loadTable(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadTable(""); });
    }

    private void savePatient() {
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

        String emergencyVal = txtEmergencyContact.getText().trim();
        if (!emergencyVal.isEmpty() && !Validator.isValidPhone(emergencyVal)) {
            JOptionPane.showMessageDialog(this, "Emergency contact must be 10-15 digits, numbers only.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Date dobDate = (java.util.Date) spnDOB.getValue();
        java.sql.Date dob = new java.sql.Date(dobDate.getTime());
        if (dob.after(new java.sql.Date(System.currentTimeMillis()))) {
            JOptionPane.showMessageDialog(this, "Date of Birth cannot be in the future.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String gender = rbMale.isSelected() ? "male" : rbFemale.isSelected() ? "female" : "other";

        String sql = selectedPatientId == -1
                ? "INSERT INTO patient (full_name, dob, gender, blood_group, phone, email, address, emergency_contact, status) VALUES (?,?,?,?,?,?,?,?,?)"
                : "UPDATE patient SET full_name=?, dob=?, gender=?, blood_group=?, phone=?, email=?, address=?, emergency_contact=?, status=? WHERE patient_id=?";

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fullName);
            ps.setDate(2, dob);
            ps.setString(3, gender);
            String bg = (String) cmbBloodGroup.getSelectedItem();
            ps.setString(4, bg == null || bg.isEmpty() ? null : bg);
            ps.setString(5, phone);
            ps.setString(6, emailVal.isEmpty() ? null : emailVal);
            ps.setString(7, txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
            ps.setString(8, emergencyVal.isEmpty() ? null : emergencyVal);
            ps.setString(9, (String) cmbStatus.getSelectedItem());
            if (selectedPatientId != -1) ps.setInt(10, selectedPatientId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, selectedPatientId == -1 ? "Patient added successfully." : "Patient updated successfully.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "PatientForm.savePatient", "Failed to save patient: " + fullName, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePatient() {
        if (selectedPatientId == -1) {
            JOptionPane.showMessageDialog(this, "Click Edit on a row first to select a patient.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this patient?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM patient WHERE patient_id = ?");
            ps.setInt(1, selectedPatientId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Patient deleted.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "PatientForm.deletePatient", "Failed to delete patient id=" + selectedPatientId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Uses stored procedure sp_admit_patient — assigns an available room
    // and marks the patient as admitted in a single transaction.
    private void admitPatient() {
        if (selectedPatientId == -1) {
            JOptionPane.showMessageDialog(this, "Click Edit on a row first to select a patient.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Load available rooms
        java.util.List<String> roomItems = new java.util.ArrayList<>();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT r.room_id, r.room_number, r.room_type, w.ward_name, r.daily_rate " +
                "FROM room r JOIN ward w ON r.ward_id = w.ward_id WHERE r.status = 'available' ORDER BY r.room_number"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roomItems.add(rs.getInt("room_id") + " - " + rs.getString("room_number") +
                        " (" + rs.getString("room_type") + ", " + rs.getString("ward_name") +
                        ", Rs " + rs.getDouble("daily_rate") + "/day)");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PatientForm.admitPatient", "Failed to load available rooms", ex);
            JOptionPane.showMessageDialog(this, "Error loading rooms: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (roomItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No rooms are currently available.", "Admit Patient", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> cmbRoom = new JComboBox<>(roomItems.toArray(new String[0]));
        com.toedter.calendar.JDateChooser dateAdmit = new com.toedter.calendar.JDateChooser();
        dateAdmit.setDateFormatString("yyyy-MM-dd");
        dateAdmit.setDate(new java.util.Date());

        JPanel dialogPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        dialogPanel.add(new JLabel("Room:"));
        dialogPanel.add(cmbRoom);
        dialogPanel.add(new JLabel("Admission Date:"));
        dialogPanel.add(dateAdmit);

        int result = JOptionPane.showConfirmDialog(this, dialogPanel, "Admit Patient to Room",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (dateAdmit.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Admission date is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomId = Integer.parseInt(((String) cmbRoom.getSelectedItem()).split(" - ")[0]);
        String admitDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateAdmit.getDate());

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            CallableStatement cs = con.prepareCall("{CALL sp_admit_patient(?, ?, ?)}");
            cs.setInt(1, selectedPatientId);
            cs.setInt(2, roomId);
            cs.setString(3, admitDate);
            cs.execute();
            JOptionPane.showMessageDialog(this, "Patient admitted successfully.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "PatientForm.admitPatient", "Failed to admit patient id=" + selectedPatientId, ex);
            JOptionPane.showMessageDialog(this, "Admit error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = search.isEmpty()
                ? "SELECT patient_id, full_name, dob, phone, gender, blood_group, status FROM patient ORDER BY patient_id DESC"
                : "SELECT patient_id, full_name, dob, phone, gender, blood_group, status FROM patient WHERE full_name LIKE ? OR phone LIKE ? ORDER BY patient_id DESC";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            if (!search.isEmpty()) { String q = "%" + search + "%"; ps.setString(1, q); ps.setString(2, q); }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("patient_id"),
                        rs.getString("full_name"),
                        rs.getDate("dob"),
                        rs.getString("phone"),
                        rs.getString("gender"),
                        rs.getString("blood_group"),
                        rs.getString("status"),
                        "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PatientForm.loadTable", "Failed to load patient list", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForEdit(int row) {
        selectedPatientId = (int) tableModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM patient WHERE patient_id = ?");
            ps.setInt(1, selectedPatientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                txtFullName.setText(rs.getString("full_name"));
                txtPhone.setText(rs.getString("phone"));
                txtEmail.setText(rs.getString("email") != null ? rs.getString("email") : "");
                txtAddress.setText(rs.getString("address") != null ? rs.getString("address") : "");
                txtEmergencyContact.setText(rs.getString("emergency_contact") != null ? rs.getString("emergency_contact") : "");
                if (rs.getDate("dob") != null) spnDOB.setValue(rs.getDate("dob"));
                String bg = rs.getString("blood_group");
                cmbBloodGroup.setSelectedItem(bg != null ? bg : "");
                String g = rs.getString("gender");
                if ("female".equals(g)) rbFemale.setSelected(true);
                else if ("other".equals(g)) rbOther.setSelected(true);
                else rbMale.setSelected(true);
                cmbStatus.setSelectedItem(rs.getString("status"));
                btnSave.setText("Update");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "PatientForm.loadForEdit", "Failed to load patient id=" + selectedPatientId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedPatientId = -1;
        txtFullName.setText(""); txtPhone.setText("");
        txtEmail.setText(""); txtAddress.setText("");
        txtEmergencyContact.setText("");
        cmbBloodGroup.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        rbMale.setSelected(true);
        btnSave.setText("Save");
    }

    static class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int currentRow;
        private IntConsumer onClick;

        ButtonEditor(JCheckBox cb, String label, IntConsumer onClick) {
            super(cb);
            this.onClick = onClick;
            button = new JButton(label);
            button.setFont(new Font("SansSerif", Font.PLAIN, 11));
            button.addActionListener(e -> { fireEditingStopped(); onClick.accept(currentRow); });
        }

        public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            currentRow = row;
            return button;
        }

        public Object getCellEditorValue() { return "Edit"; }
    }
}