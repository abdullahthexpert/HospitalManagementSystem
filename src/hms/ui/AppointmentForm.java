package hms.ui;

import hms.software.DatabaseConnection;
import hms.software.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AppointmentForm extends JPanel {

    private JComboBox<String> cmbPatient, cmbDoctor, cmbStatus;
    private JTextField txtReason, txtSearch;
    private JTextArea txtNotes;
    private com.toedter.calendar.JDateChooser dateAppt;
    private JSpinner spnHour, spnMin;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnSave, btnClear, btnDelete;
    private int selectedApptId = -1;

    public AppointmentForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new TitledBorder("Appointment Details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 – Patient, Doctor
        g.gridx = 0; g.gridy = 0; formPanel.add(new JLabel("Patient:*"), g);
        g.gridx = 1; cmbPatient = new JComboBox<>(); loadPatients(); formPanel.add(cmbPatient, g);
        g.gridx = 2; formPanel.add(new JLabel("Doctor:*"), g);
        g.gridx = 3; cmbDoctor = new JComboBox<>(); loadDoctors(); formPanel.add(cmbDoctor, g);

        // Row 1 – Date, Time
        g.gridx = 0; g.gridy = 1; formPanel.add(new JLabel("Date:*"), g);
        g.gridx = 1;
        dateAppt = new com.toedter.calendar.JDateChooser();
        dateAppt.setDateFormatString("yyyy-MM-dd");
        formPanel.add(dateAppt, g);
        g.gridx = 2; formPanel.add(new JLabel("Time (HH:MM):*"), g);
        g.gridx = 3;
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        spnHour = new JSpinner(new SpinnerNumberModel(9, 0, 23, 1));
        spnMin  = new JSpinner(new SpinnerNumberModel(0, 0, 59, 5));
        ((JSpinner.DefaultEditor) spnHour.getEditor()).getTextField().setColumns(2);
        ((JSpinner.DefaultEditor) spnMin.getEditor()).getTextField().setColumns(2);
        timePanel.add(spnHour); timePanel.add(new JLabel(":")); timePanel.add(spnMin);
        formPanel.add(timePanel, g);

        // Row 2 – Reason, Status
        g.gridx = 0; g.gridy = 2; formPanel.add(new JLabel("Reason:"), g);
        g.gridx = 1; txtReason = new JTextField(15); formPanel.add(txtReason, g);
        g.gridx = 2; formPanel.add(new JLabel("Status:"), g);
        g.gridx = 3;
        cmbStatus = new JComboBox<>(new String[]{"scheduled", "completed", "cancelled", "no_show"});
        formPanel.add(cmbStatus, g);

        // Row 3 – Notes
        g.gridx = 0; g.gridy = 3; formPanel.add(new JLabel("Notes:"), g);
        g.gridx = 1; g.gridwidth = 3; g.gridheight = 2;
        txtNotes = new JTextArea(3, 30);
        txtNotes.setLineWrap(true);
        formPanel.add(new JScrollPane(txtNotes), g);
        g.gridwidth = 1; g.gridheight = 1;

        // Row 5 – Buttons
        g.gridx = 1; g.gridy = 5;
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
        JButton btnToday   = new JButton("Today");
        btnSearch.setFocusPainted(false);
        btnRefresh.setFocusPainted(false);
        btnToday.setFocusPainted(false);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);
        searchPanel.add(btnToday);
        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Patient", "Doctor", "Date & Time", "Reason", "Status", "Edit"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 6; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(6).setMaxWidth(60);

        table.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit"); b.setFont(new Font("SansSerif", Font.PLAIN, 11)); return b;
        });
        table.getColumn("Edit").setCellEditor(
            new PatientForm.ButtonEditor(new JCheckBox(), "Edit", row -> loadForEdit(row))
        );

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, bottomPanel);
        split.setDividerLocation(290);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        btnSave.addActionListener(e -> saveAppointment());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> deleteAppointment());
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadTable(""); });
        btnToday.addActionListener(e -> loadTodayAppointments());
    }

    private void loadPatients() {
        cmbPatient.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(
                "SELECT patient_id, full_name FROM patient WHERE status != 'discharged' ORDER BY full_name"
            ).executeQuery();
            while (rs.next()) {
                cmbPatient.addItem(rs.getInt("patient_id") + " - " + rs.getString("full_name"));
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.loadPatients", "Failed to load patient list", ex);
            JOptionPane.showMessageDialog(this, "Patient load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDoctors() {
        cmbDoctor.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(
                "SELECT doctor_id, full_name, specialization FROM doctor WHERE is_available = 1 ORDER BY full_name"
            ).executeQuery();
            while (rs.next()) {
                cmbDoctor.addItem(rs.getInt("doctor_id") + " - " + rs.getString("full_name") + " (" + rs.getString("specialization") + ")");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.loadDoctors", "Failed to load doctor list", ex);
            JOptionPane.showMessageDialog(this, "Doctor load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAppointment() {
        if (cmbPatient.getSelectedItem() == null || cmbDoctor.getSelectedItem() == null || dateAppt.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Patient, Doctor and Date are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int patientId = Integer.parseInt(((String) cmbPatient.getSelectedItem()).split(" - ")[0]);
        int doctorId  = Integer.parseInt(((String) cmbDoctor.getSelectedItem()).split(" - ")[0]);
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateAppt.getDate());
        String timeStr = String.format("%02d:%02d:00", (int) spnHour.getValue(), (int) spnMin.getValue());
        String apptDatetime = dateStr + " " + timeStr;

        if (selectedApptId == -1) {
            try {
                java.text.SimpleDateFormat full = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (full.parse(apptDatetime).before(new java.util.Date())) {
                    JOptionPane.showMessageDialog(this, "Appointment date/time cannot be in the past.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (java.text.ParseException ignored) {}
        }

        String sql = selectedApptId == -1
            ? "INSERT INTO appointment (patient_id, doctor_id, appt_datetime, status, reason, notes) VALUES (?,?,?,?,?,?)"
            : "UPDATE appointment SET patient_id=?, doctor_id=?, appt_datetime=?, status=?, reason=?, notes=? WHERE appt_id=?";

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setString(3, apptDatetime);
            ps.setString(4, (String) cmbStatus.getSelectedItem());
            ps.setString(5, txtReason.getText().trim());
            ps.setString(6, txtNotes.getText().trim());
            if (selectedApptId != -1) ps.setInt(7, selectedApptId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, selectedApptId == -1 ? "Appointment booked." : "Appointment updated.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.saveAppointment", "Failed to save appointment", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAppointment() {
        if (selectedApptId == -1) {
            JOptionPane.showMessageDialog(this, "Select an appointment to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this appointment?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM appointment WHERE appt_id = ?");
            ps.setInt(1, selectedApptId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Appointment deleted.");
            clearForm();
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.deleteAppointment", "Failed to delete appointment id=" + selectedApptId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = search.isEmpty()
            ? "SELECT a.appt_id, p.full_name AS patient, doc.full_name AS doctor, a.appt_datetime, a.reason, a.status FROM appointment a JOIN patient p ON a.patient_id = p.patient_id JOIN doctor doc ON a.doctor_id = doc.doctor_id ORDER BY a.appt_datetime DESC"
            : "SELECT a.appt_id, p.full_name AS patient, doc.full_name AS doctor, a.appt_datetime, a.reason, a.status FROM appointment a JOIN patient p ON a.patient_id = p.patient_id JOIN doctor doc ON a.doctor_id = doc.doctor_id WHERE p.full_name LIKE ? OR doc.full_name LIKE ? OR a.status LIKE ? ORDER BY a.appt_datetime DESC";
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
                    rs.getInt("appt_id"),
                    rs.getString("patient"),
                    rs.getString("doctor"),
                    rs.getString("appt_datetime"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.loadTable", "Failed to load appointment list", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTodayAppointments() {
        tableModel.setRowCount(0);
        String sql = "SELECT a.appt_id, p.full_name AS patient, doc.full_name AS doctor, a.appt_datetime, a.reason, a.status FROM appointment a JOIN patient p ON a.patient_id = p.patient_id JOIN doctor doc ON a.doctor_id = doc.doctor_id WHERE DATE(a.appt_datetime) = CURRENT_DATE ORDER BY a.appt_datetime";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("appt_id"),
                    rs.getString("patient"),
                    rs.getString("doctor"),
                    rs.getString("appt_datetime"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.loadTodayAppointments", "Failed to load today's appointments", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForEdit(int row) {
        selectedApptId = (int) tableModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM appointment WHERE appt_id = ?");
            ps.setInt(1, selectedApptId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // set patient
                int pid = rs.getInt("patient_id");
                for (int i = 0; i < cmbPatient.getItemCount(); i++) {
                    if (cmbPatient.getItemAt(i).startsWith(pid + " - ")) { cmbPatient.setSelectedIndex(i); break; }
                }
                // set doctor
                int did = rs.getInt("doctor_id");
                for (int i = 0; i < cmbDoctor.getItemCount(); i++) {
                    if (cmbDoctor.getItemAt(i).startsWith(did + " - ")) { cmbDoctor.setSelectedIndex(i); break; }
                }
                Timestamp ts = rs.getTimestamp("appt_datetime");
                if (ts != null) {
                    dateAppt.setDate(new java.util.Date(ts.getTime()));
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(ts);
                    spnHour.setValue(cal.get(java.util.Calendar.HOUR_OF_DAY));
                    spnMin.setValue(cal.get(java.util.Calendar.MINUTE));
                }
                txtReason.setText(rs.getString("reason") != null ? rs.getString("reason") : "");
                txtNotes.setText(rs.getString("notes") != null ? rs.getString("notes") : "");
                cmbStatus.setSelectedItem(rs.getString("status"));
                btnSave.setText("Update");
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "AppointmentForm.loadForEdit", "Failed to load appointment id=" + selectedApptId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedApptId = -1;
        if (cmbPatient.getItemCount() > 0) cmbPatient.setSelectedIndex(0);
        if (cmbDoctor.getItemCount() > 0) cmbDoctor.setSelectedIndex(0);
        dateAppt.setDate(null);
        spnHour.setValue(9); spnMin.setValue(0);
        txtReason.setText(""); txtNotes.setText("");
        cmbStatus.setSelectedIndex(0);
        btnSave.setText("Save");
    }
}
