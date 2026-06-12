package hms.ui;

import hms.software.DatabaseConnection;
import hms.software.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LabForm extends JPanel {

    private JComboBox<String> cmbPatient, cmbDoctor, cmbStatus, cmbStaff;
    private JTextField txtTestName, txtTestFee, txtSearch;
    private JTextArea txtResult, txtRemarks;
    private com.toedter.calendar.JDateChooser dateTest, dateReport;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnSave, btnClear, btnSaveReport;
    private int selectedTestId = -1;

    public LabForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
        loadTable("");
    }

    private void buildUI() {
        // -- Test Order Form --
        JPanel orderPanel = new JPanel(new GridBagLayout());
        orderPanel.setBorder(new TitledBorder("Order Lab Test"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; orderPanel.add(new JLabel("Patient:*"), g);
        g.gridx = 1; cmbPatient = new JComboBox<>(); loadPatients(); orderPanel.add(cmbPatient, g);
        g.gridx = 2; orderPanel.add(new JLabel("Doctor:*"), g);
        g.gridx = 3; cmbDoctor = new JComboBox<>(); loadDoctors(); orderPanel.add(cmbDoctor, g);

        g.gridx = 0; g.gridy = 1; orderPanel.add(new JLabel("Test Name:*"), g);
        g.gridx = 1;
        JComboBox<String> cmbTestName = new JComboBox<>(new String[]{
            "Complete Blood Count", "Blood Sugar Fasting", "Blood Sugar Random",
            "HbA1c", "Lipid Profile", "Liver Function Test", "Kidney Function Test",
            "Thyroid Profile (T3/T4/TSH)", "Urinalysis", "Urine Culture",
            "X-Ray Chest", "ECG", "Echocardiography", "CT Scan", "MRI",
            "Ultrasound Abdomen", "HBsAg", "HIV Test", "COVID-19 PCR", "Other"
        });
        cmbTestName.setEditable(true);
        txtTestName = (JTextField) cmbTestName.getEditor().getEditorComponent();
        orderPanel.add(cmbTestName, g);
        g.gridx = 2; orderPanel.add(new JLabel("Test Fee:"), g);
        g.gridx = 3; txtTestFee = new JTextField("0.00"); orderPanel.add(txtTestFee, g);

        g.gridx = 0; g.gridy = 2; orderPanel.add(new JLabel("Test Date:*"), g);
        g.gridx = 1;
        dateTest = new com.toedter.calendar.JDateChooser();
        dateTest.setDateFormatString("yyyy-MM-dd");
        dateTest.setDate(new java.util.Date());
        orderPanel.add(dateTest, g);
        g.gridx = 2; orderPanel.add(new JLabel("Status:"), g);
        g.gridx = 3;
        cmbStatus = new JComboBox<>(new String[]{"pending", "in_progress", "completed", "cancelled"});
        orderPanel.add(cmbStatus, g);

        g.gridx = 1; g.gridy = 3;
        btnSave = new JButton("Order Test");
        btnSave.setBackground(new Color(0, 120, 215));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        orderPanel.add(btnSave, g);
        g.gridx = 2;
        btnClear = new JButton("Clear");
        btnClear.setFocusPainted(false);
        orderPanel.add(btnClear, g);

        // -- Report Form --
        JPanel reportPanel = new JPanel(new GridBagLayout());
        reportPanel.setBorder(new TitledBorder("Lab Report"));
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(5, 5, 5, 5);
        r.fill = GridBagConstraints.HORIZONTAL;

        r.gridx = 0; r.gridy = 0; reportPanel.add(new JLabel("Report Date:"), r);
        r.gridx = 1;
        dateReport = new com.toedter.calendar.JDateChooser();
        dateReport.setDateFormatString("yyyy-MM-dd");
        dateReport.setDate(new java.util.Date());
        reportPanel.add(dateReport, r);
        r.gridx = 2; reportPanel.add(new JLabel("Reported By:"), r);
        r.gridx = 3; cmbStaff = new JComboBox<>(); loadStaff(); reportPanel.add(cmbStaff, r);

        r.gridx = 0; r.gridy = 1; reportPanel.add(new JLabel("Result:*"), r);
        r.gridx = 1; r.gridwidth = 3; r.gridheight = 2;
        txtResult = new JTextArea(3, 30);
        txtResult.setLineWrap(true);
        reportPanel.add(new JScrollPane(txtResult), r);
        r.gridwidth = 1; r.gridheight = 1;

        r.gridx = 0; r.gridy = 3; reportPanel.add(new JLabel("Remarks:"), r);
        r.gridx = 1; r.gridwidth = 3; r.gridheight = 2;
        txtRemarks = new JTextArea(2, 30);
        txtRemarks.setLineWrap(true);
        reportPanel.add(new JScrollPane(txtRemarks), r);
        r.gridwidth = 1; r.gridheight = 1;

        r.gridx = 1; r.gridy = 5;
        btnSaveReport = new JButton("Save Report");
        btnSaveReport.setBackground(new Color(0, 160, 80));
        btnSaveReport.setForeground(Color.WHITE);
        btnSaveReport.setFocusPainted(false);
        reportPanel.add(btnSaveReport, r);

        // -- Top: order + report side by side --
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        topPanel.add(orderPanel);
        topPanel.add(reportPanel);

        // -- Bottom: table --
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(20);
        JButton btnSearch  = new JButton("Search");
        JButton btnRefresh = new JButton("Show All");
        JButton btnPending = new JButton("Pending");
        btnSearch.setFocusPainted(false); btnRefresh.setFocusPainted(false); btnPending.setFocusPainted(false);
        searchPanel.add(txtSearch); searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh); searchPanel.add(btnPending);
        bottomPanel.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"Test ID", "Patient", "Doctor", "Test Name", "Date", "Fee", "Status", "Report", "Edit"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) { return col == 8; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getColumnModel().getColumn(8).setMaxWidth(60);

        table.getColumn("Edit").setCellRenderer((t, val, sel, foc, row, col) -> {
            JButton b = new JButton("Edit"); b.setFont(new Font("SansSerif", Font.PLAIN, 11)); return b;
        });
        table.getColumn("Edit").setCellEditor(
            new PatientForm.ButtonEditor(new JCheckBox(), "Edit", row -> loadForEdit(row))
        );

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 200));
        bottomPanel.add(scroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        split.setDividerLocation(280);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        btnSave.addActionListener(e -> saveTest(cmbTestName));
        btnClear.addActionListener(e -> clearForm());
        btnSaveReport.addActionListener(e -> saveReport());
        btnSearch.addActionListener(e -> loadTable(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadTable(""); });
        btnPending.addActionListener(e -> loadByStatus("pending"));
    }

    private void loadPatients() {
        cmbPatient.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT patient_id, full_name FROM patient ORDER BY full_name").executeQuery();
            while (rs.next()) cmbPatient.addItem(rs.getInt("patient_id") + " - " + rs.getString("full_name"));
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.loadPatients", "Failed to load patient list", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDoctors() {
        cmbDoctor.removeAllItems();
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement("SELECT doctor_id, full_name FROM doctor ORDER BY full_name").executeQuery();
            while (rs.next()) cmbDoctor.addItem(rs.getInt("doctor_id") + " - " + rs.getString("full_name"));
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.loadDoctors", "Failed to load doctor list", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStaff() {
        cmbStaff.removeAllItems();
        cmbStaff.addItem("0 - N/A");
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT staff_id, full_name FROM staff WHERE role = 'lab_technician' ORDER BY full_name");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) cmbStaff.addItem(rs.getInt("staff_id") + " - " + rs.getString("full_name"));
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.loadStaff", "Failed to load lab technician list", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTest(JComboBox<String> cmbTestName) {
        if (cmbPatient.getSelectedItem() == null || cmbDoctor.getSelectedItem() == null || dateTest.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Patient, Doctor and Date are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String testName = txtTestName.getText().trim();
        if (testName.isEmpty()) { JOptionPane.showMessageDialog(this, "Test name is required.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

        int patientId = Integer.parseInt(((String) cmbPatient.getSelectedItem()).split(" - ")[0]);
        int doctorId  = Integer.parseInt(((String) cmbDoctor.getSelectedItem()).split(" - ")[0]);
        String testDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateTest.getDate());
        double fee = 0;
        try { fee = Double.parseDouble(txtTestFee.getText().trim()); } catch (NumberFormatException ignored) {}
        if (fee < 0) {
            JOptionPane.showMessageDialog(this, "Test fee cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isNew = selectedTestId == -1;
        String sql = isNew
            ? "INSERT INTO lab_test (patient_id, doctor_id, test_name, test_date, status, test_fee) VALUES (?,?,?,?,?,?)"
            : "UPDATE lab_test SET patient_id=?, doctor_id=?, test_name=?, test_date=?, status=?, test_fee=? WHERE test_id=?";
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, patientId); ps.setInt(2, doctorId);
            ps.setString(3, testName); ps.setString(4, testDate);
            ps.setString(5, (String) cmbStatus.getSelectedItem());
            ps.setDouble(6, fee);
            if (!isNew) ps.setInt(7, selectedTestId);
            ps.executeUpdate();
            if (isNew) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) selectedTestId = keys.getInt(1);
            }
            JOptionPane.showMessageDialog(this, isNew ? "Test ordered." : "Test updated.");
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.saveTest", "Failed to save lab test for patient_id=" + patientId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveReport() {
        if (selectedTestId == -1) {
            JOptionPane.showMessageDialog(this, "Select a test first by clicking Edit.", "No Test Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String result = txtResult.getText().trim();
        if (result.isEmpty()) { JOptionPane.showMessageDialog(this, "Result is required.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
        if (dateReport.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Report date is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String reportDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateReport.getDate());
        String staffStr = (String) cmbStaff.getSelectedItem();
        int staffId = (staffStr != null && !staffStr.startsWith("0")) ? Integer.parseInt(staffStr.split(" - ")[0]) : 0;

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            // check if report exists
            PreparedStatement check = con.prepareStatement("SELECT report_id FROM lab_report WHERE test_id = ?");
            check.setInt(1, selectedTestId);
            ResultSet rs = check.executeQuery();
            PreparedStatement ps;
            if (rs.next()) {
                ps = con.prepareStatement("UPDATE lab_report SET report_date=?, result=?, remarks=?, reported_by=? WHERE test_id=?");
                ps.setString(1, reportDate); ps.setString(2, result);
                ps.setString(3, txtRemarks.getText().trim());
                if (staffId > 0) ps.setInt(4, staffId); else ps.setNull(4, Types.INTEGER);
                ps.setInt(5, selectedTestId);
            } else {
                ps = con.prepareStatement("INSERT INTO lab_report (test_id, report_date, result, remarks, reported_by) VALUES (?,?,?,?,?)");
                ps.setInt(1, selectedTestId); ps.setString(2, reportDate); ps.setString(3, result);
                ps.setString(4, txtRemarks.getText().trim());
                if (staffId > 0) ps.setInt(5, staffId); else ps.setNull(5, Types.INTEGER);
            }
            ps.executeUpdate();
            // mark test as completed
            PreparedStatement upd = con.prepareStatement("UPDATE lab_test SET status='completed' WHERE test_id=?");
            upd.setInt(1, selectedTestId);
            upd.executeUpdate();
            JOptionPane.showMessageDialog(this, "Report saved. Test marked completed.");
            loadTable("");
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.saveReport", "Failed to save lab report for test_id=" + selectedTestId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTable(String search) {
        tableModel.setRowCount(0);
        String sql = search.isEmpty()
            ? "SELECT lt.test_id, p.full_name AS patient, doc.full_name AS doctor, lt.test_name, lt.test_date, lt.test_fee, lt.status, CASE WHEN lr.report_id IS NOT NULL THEN 'Yes' ELSE 'No' END AS has_report FROM lab_test lt JOIN patient p ON lt.patient_id = p.patient_id JOIN doctor doc ON lt.doctor_id = doc.doctor_id LEFT JOIN lab_report lr ON lt.test_id = lr.test_id ORDER BY lt.test_id DESC"
            : "SELECT lt.test_id, p.full_name AS patient, doc.full_name AS doctor, lt.test_name, lt.test_date, lt.test_fee, lt.status, CASE WHEN lr.report_id IS NOT NULL THEN 'Yes' ELSE 'No' END AS has_report FROM lab_test lt JOIN patient p ON lt.patient_id = p.patient_id JOIN doctor doc ON lt.doctor_id = doc.doctor_id LEFT JOIN lab_report lr ON lt.test_id = lr.test_id WHERE p.full_name LIKE ? OR lt.test_name LIKE ? OR lt.status LIKE ? ORDER BY lt.test_id DESC";
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
                    rs.getInt("test_id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getString("test_name"), rs.getString("test_date"),
                    String.format("%.2f", rs.getDouble("test_fee")),
                    rs.getString("status"), rs.getString("has_report"), "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.loadTable", "Failed to load lab test list", ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadByStatus(String status) {
        tableModel.setRowCount(0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT lt.test_id, p.full_name AS patient, doc.full_name AS doctor, lt.test_name, lt.test_date, lt.test_fee, lt.status, CASE WHEN lr.report_id IS NOT NULL THEN 'Yes' ELSE 'No' END AS has_report FROM lab_test lt JOIN patient p ON lt.patient_id = p.patient_id JOIN doctor doc ON lt.doctor_id = doc.doctor_id LEFT JOIN lab_report lr ON lt.test_id = lr.test_id WHERE lt.status = ? ORDER BY lt.test_date"
            );
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("test_id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getString("test_name"), rs.getString("test_date"),
                    String.format("%.2f", rs.getDouble("test_fee")),
                    rs.getString("status"), rs.getString("has_report"), "Edit"
                });
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.loadByStatus", "Failed to load tests by status=" + status, ex);
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForEdit(int row) {
        selectedTestId = (int) tableModel.getValueAt(row, 0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM lab_test WHERE test_id = ?");
            ps.setInt(1, selectedTestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int pid = rs.getInt("patient_id");
                for (int i = 0; i < cmbPatient.getItemCount(); i++) {
                    if (cmbPatient.getItemAt(i).startsWith(pid + " - ")) { cmbPatient.setSelectedIndex(i); break; }
                }
                int did = rs.getInt("doctor_id");
                for (int i = 0; i < cmbDoctor.getItemCount(); i++) {
                    if (cmbDoctor.getItemAt(i).startsWith(did + " - ")) { cmbDoctor.setSelectedIndex(i); break; }
                }
                txtTestName.setText(rs.getString("test_name"));
                txtTestFee.setText(String.format("%.2f", rs.getDouble("test_fee")));
                java.sql.Date td = rs.getDate("test_date");
                if (td != null) dateTest.setDate(new java.util.Date(td.getTime()));
                cmbStatus.setSelectedItem(rs.getString("status"));
                btnSave.setText("Update");
            }
            // load existing report if any
            PreparedStatement rps = con.prepareStatement("SELECT * FROM lab_report WHERE test_id = ?");
            rps.setInt(1, selectedTestId);
            ResultSet rrs = rps.executeQuery();
            if (rrs.next()) {
                txtResult.setText(rrs.getString("result"));
                txtRemarks.setText(rrs.getString("remarks") != null ? rrs.getString("remarks") : "");
                java.sql.Date rd = rrs.getDate("report_date");
                if (rd != null) dateReport.setDate(new java.util.Date(rd.getTime()));
                int sid = rrs.getInt("reported_by");
                for (int i = 0; i < cmbStaff.getItemCount(); i++) {
                    if (cmbStaff.getItemAt(i).startsWith(sid + " - ")) { cmbStaff.setSelectedIndex(i); break; }
                }
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "LabForm.loadForEdit", "Failed to load lab test id=" + selectedTestId, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        selectedTestId = -1;
        if (cmbPatient.getItemCount() > 0) cmbPatient.setSelectedIndex(0);
        if (cmbDoctor.getItemCount() > 0) cmbDoctor.setSelectedIndex(0);
        txtTestName.setText(""); txtTestFee.setText("0.00");
        dateTest.setDate(new java.util.Date());
        cmbStatus.setSelectedIndex(0);
        txtResult.setText(""); txtRemarks.setText("");
        dateReport.setDate(new java.util.Date());
        if (cmbStaff.getItemCount() > 0) cmbStaff.setSelectedIndex(0);
        btnSave.setText("Order Test");
    }
}
