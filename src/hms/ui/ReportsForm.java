package hms.ui;

import hms.software.DatabaseConnection;
import hms.software.ReportGenerator;
import hms.software.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ReportsForm extends JPanel {

    public ReportsForm() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Admitted Patients",  buildViewTab("vw_admitted_patients",
            new String[]{"Patient ID","Full Name","Gender","Blood Group","Phone","Admitted","Room","Room Type","Ward","Department"},
            "SELECT * FROM vw_admitted_patients"));

        tabs.addTab("Today's Appointments", buildViewTab("vw_todays_appointments",
            new String[]{"Appt ID","Date & Time","Status","Reason","Patient","Phone","Doctor","Department"},
            "SELECT * FROM vw_todays_appointments"));

        tabs.addTab("Medicine Stock", buildViewTab("vw_medicine_stock",
            new String[]{"Med ID","Medicine","Type","Unit Price","Total Stock","Nearest Expiry","Stock Status"},
            "SELECT * FROM vw_medicine_stock"));

        tabs.addTab("Pending Bills", buildViewTab("vw_pending_bills",
            new String[]{"Bill ID","Bill Date","Total","Paid","Balance Due","Status","Patient","Phone"},
            "SELECT * FROM vw_pending_bills"));

        tabs.addTab("Doctor Workload", buildViewTab("vw_doctor_workload",
            new String[]{"Doc ID","Full Name","Specialization","Department","Total Appts","Completed","Upcoming","Cancelled"},
            "SELECT * FROM vw_doctor_workload"));

        tabs.addTab("Bill Items Detail", buildViewTab("bill_items_detail",
            new String[]{"Item ID","Bill ID","Patient","Description","Qty","Unit Price","Amount","Type"},
            "SELECT bi.item_id, bi.bill_id, p.full_name, bi.description, bi.quantity, bi.unit_price, bi.amount, bi.item_type " +
            "FROM bill_item bi JOIN bill b ON bi.bill_id = b.bill_id JOIN patient p ON b.patient_id = p.patient_id ORDER BY bi.bill_id DESC"));

        tabs.addTab("Lab Test Summary", buildViewTab("lab_test_summary",
            new String[]{"Test ID","Patient","Doctor","Test Name","Test Date","Fee","Status","Has Report"},
            "SELECT lt.test_id, p.full_name, doc.full_name, lt.test_name, lt.test_date, lt.test_fee, lt.status, " +
            "CASE WHEN lr.report_id IS NOT NULL THEN 'Yes' ELSE 'No' END " +
            "FROM lab_test lt JOIN patient p ON lt.patient_id = p.patient_id JOIN doctor doc ON lt.doctor_id = doc.doctor_id " +
            "LEFT JOIN lab_report lr ON lt.test_id = lr.test_id ORDER BY lt.test_id DESC"));

        tabs.addTab("Staff Directory", buildViewTab("staff_directory",
            new String[]{"Staff ID","Full Name","Role","Phone","Email","Department","Hire Date"},
            "SELECT s.staff_id, s.full_name, s.role, s.phone, s.email, d.dept_name, s.hire_date " +
            "FROM staff s JOIN department d ON s.dept_id = d.dept_id ORDER BY s.full_name"));

        tabs.addTab("Revenue Summary", buildRevenueTab());

        tabs.addTab("Patient History", buildPatientHistoryTab());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildViewTab(String viewName, String[] columns, String baseSql) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnLoad   = new JButton("Load / Refresh");
        JButton btnExport = new JButton("Export to CSV");
        JButton btnExportPdf = new JButton("Export to PDF");
        btnLoad.setBackground(new Color(0, 120, 215));
        btnLoad.setForeground(Color.WHITE);
        btnLoad.setFocusPainted(false);
        btnExport.setFocusPainted(false);
        btnExportPdf.setBackground(new Color(180, 50, 50));
        btnExportPdf.setForeground(Color.WHITE);
        btnExportPdf.setFocusPainted(false);
        topBar.add(btnLoad); topBar.add(btnExport); topBar.add(btnExportPdf);

        // optional filter for stock tab
        if (viewName.equals("vw_medicine_stock")) {
            JCheckBox chkLow = new JCheckBox("Low Stock Only");
            topBar.add(chkLow);
            btnLoad.addActionListener(e -> loadViewIntoTable(
                chkLow.isSelected() ? baseSql + " WHERE stock_status = 'LOW'" : baseSql,
                columns, (DefaultTableModel) ((JTable) ((JScrollPane) panel.getComponent(1)).getViewport().getView()).getModel()
            ));
        } else {
            btnLoad.addActionListener(e -> loadViewIntoTable(
                baseSql, columns,
                (DefaultTableModel) ((JTable) ((JScrollPane) panel.getComponent(1)).getViewport().getView()).getModel()
            ));
        }

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scroll = new JScrollPane(table);

        btnExport.addActionListener(e -> exportToCSV(table, viewName));
        btnExportPdf.addActionListener(e -> exportToPDF(table, viewName));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);


        // row count label
        JLabel lblCount = new JLabel("  Rows: 0");
        lblCount.setFont(new Font("SansSerif", Font.ITALIC, 11));
        panel.add(lblCount, BorderLayout.SOUTH);
        model.addTableModelListener(ev -> lblCount.setText("  Rows: " + model.getRowCount()));

        return panel;
    }

    private void loadViewIntoTable(String sql, String[] columns, DefaultTableModel model) {
        model.setRowCount(0);
        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            ResultSet rs = con.prepareStatement(sql).executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 0; i < colCount; i++) row[i] = rs.getObject(i + 1);
                model.addRow(row);
            }
        } catch (SQLException ex) {
            Logger.log("ERROR", "ReportsForm.loadViewIntoTable", "Failed to load view data", ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel buildRevenueTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // date range filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterPanel.add(new JLabel("From:"));
        com.toedter.calendar.JDateChooser dateFrom = new com.toedter.calendar.JDateChooser();
        dateFrom.setDateFormatString("yyyy-MM-dd");
        dateFrom.setPreferredSize(new Dimension(120, 26));
        filterPanel.add(dateFrom);
        filterPanel.add(new JLabel("To:"));
        com.toedter.calendar.JDateChooser dateTo = new com.toedter.calendar.JDateChooser();
        dateTo.setDateFormatString("yyyy-MM-dd");
        dateTo.setDate(new java.util.Date());
        dateTo.setPreferredSize(new Dimension(120, 26));
        filterPanel.add(dateTo);
        JButton btnRun = new JButton("Run Report");
        btnRun.setBackground(new Color(0, 120, 215));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFocusPainted(false);
        filterPanel.add(btnRun);

        JButton btnExportPdf = new JButton("Export to PDF");
        btnExportPdf.setBackground(new Color(180, 50, 50));
        btnExportPdf.setForeground(Color.WHITE);
        btnExportPdf.setFocusPainted(false);
        filterPanel.add(btnExportPdf);

        // Summary labels
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        summaryPanel.setBorder(new TitledBorder("Summary"));
        JLabel lblTotalBilled   = new JLabel("Total Billed: -", SwingConstants.CENTER);
        JLabel lblTotalCollected= new JLabel("Total Collected: -", SwingConstants.CENTER);
        JLabel lblPending       = new JLabel("Pending Balance: -", SwingConstants.CENTER);
        JLabel lblBillCount     = new JLabel("Bills Count: -", SwingConstants.CENTER);
        for (JLabel l : new JLabel[]{lblTotalBilled, lblTotalCollected, lblPending, lblBillCount}) {
            l.setFont(new Font("SansSerif", Font.BOLD, 13));
            summaryPanel.add(l);
        }

        String[] cols = {"Bill ID", "Patient", "Bill Date", "Total", "Paid", "Balance", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);

        JPanel topSection = new JPanel(new BorderLayout(4, 4));
        topSection.add(filterPanel, BorderLayout.NORTH);
        topSection.add(summaryPanel, BorderLayout.CENTER);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        btnRun.addActionListener(e -> {
            if (dateFrom.getDate() == null || dateTo.getDate() == null) {
                JOptionPane.showMessageDialog(panel, "Select both dates.", "Validation", JOptionPane.WARNING_MESSAGE); return;
            }
            String from = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateFrom.getDate());
            String to   = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateTo.getDate());
            model.setRowCount(0);
            try {
                Connection con = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(
                    "SELECT b.bill_id, p.full_name, b.bill_date, b.total_amount, b.paid_amount, (b.total_amount - b.paid_amount) AS balance, b.status FROM bill b JOIN patient p ON b.patient_id = p.patient_id WHERE b.bill_date BETWEEN ? AND ? ORDER BY b.bill_date DESC"
                );
                ps.setString(1, from); ps.setString(2, to);
                ResultSet rs = ps.executeQuery();
                double sumTotal = 0, sumPaid = 0;
                int count = 0;
                while (rs.next()) {
                    double tot = rs.getDouble("total_amount");
                    double paid = rs.getDouble("paid_amount");
                    sumTotal += tot; sumPaid += paid; count++;
                    model.addRow(new Object[]{
                        rs.getInt("bill_id"), rs.getString("full_name"),
                        rs.getString("bill_date"),
                        String.format("%.2f", tot), String.format("%.2f", paid),
                        String.format("%.2f", tot - paid), rs.getString("status")
                    });
                }
                lblTotalBilled.setText("Total Billed: " + String.format("Rs %.2f", sumTotal));
                lblTotalCollected.setText("Collected: " + String.format("Rs %.2f", sumPaid));
                lblPending.setText("Pending: " + String.format("Rs %.2f", sumTotal - sumPaid));
                lblBillCount.setText("Bills: " + count);
            } catch (SQLException ex) {
                Logger.log("ERROR", "ReportsForm.buildRevenueTab", "Failed to run revenue report", ex);
                JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnExportPdf.addActionListener(e -> exportToPDF(table, "Revenue Summary"));

        return panel;
    }

    private JPanel buildPatientHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.add(new JLabel("Patient ID:"));
        JTextField txtPatientId = new JTextField(8);
        JButton btnFetch = new JButton("Fetch History");
        btnFetch.setBackground(new Color(0, 120, 215));
        btnFetch.setForeground(Color.WHITE);
        btnFetch.setFocusPainted(false);
        JButton btnExportPdf = new JButton("Export to PDF");
        btnExportPdf.setBackground(new Color(180, 50, 50));
        btnExportPdf.setForeground(Color.WHITE);
        btnExportPdf.setFocusPainted(false);
        searchBar.add(txtPatientId); searchBar.add(btnFetch); searchBar.add(btnExportPdf);

        JTextArea txtOutput = new JTextArea();
        txtOutput.setEditable(false);
        txtOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtOutput.setLineWrap(true);

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtOutput), BorderLayout.CENTER);

        btnFetch.addActionListener(e -> {
            String idStr = txtPatientId.getText().trim();
            if (idStr.isEmpty()) { JOptionPane.showMessageDialog(panel, "Enter a Patient ID.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
            int pid;
            try { pid = Integer.parseInt(idStr); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(panel, "Patient ID must be numeric.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

            try {
                Connection con = DatabaseConnection.getInstance().getConnection();
                CallableStatement cs = con.prepareCall("{CALL sp_patient_history(?)}");
                cs.setInt(1, pid);
                StringBuilder sb = new StringBuilder();

                boolean hasResult = cs.execute();
                while (hasResult) {
                    ResultSet rs = cs.getResultSet();
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    sb.append("──────────────────────────────────────\n");
                    for (int i = 1; i <= cols; i++) sb.append(String.format("%-20s", meta.getColumnLabel(i)));
                    sb.append("\n");
                    while (rs.next()) {
                        for (int i = 1; i <= cols; i++) {
                            Object val = rs.getObject(i);
                            sb.append(String.format("%-20s", val != null ? val.toString() : "null"));
                        }
                        sb.append("\n");
                    }
                    hasResult = cs.getMoreResults();
                }
                txtOutput.setText(sb.toString());
            } catch (SQLException ex) {
                Logger.log("ERROR", "ReportsForm.buildPatientHistoryTab", "Failed to fetch history for patient_id=" + pid, ex);
                JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnExportPdf.addActionListener(e -> {
            String text = txtOutput.getText();
            if (text == null || text.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nothing to export. Fetch a patient's history first.", "No Data", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JFileChooser fc = new JFileChooser();
            String idStr = txtPatientId.getText().trim();
            fc.setSelectedFile(new java.io.File("Patient_History_" + (idStr.isEmpty() ? "report" : idStr) + ".pdf"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            String[] headers = {"Patient History Detail"};
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            for (String line : text.split("\n")) {
                if (!line.trim().isEmpty()) rows.add(new String[]{line});
            }
            String filePath = fc.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) filePath += ".pdf";

            ReportGenerator.generateTableReport(filePath, "Patient History" + (idStr.isEmpty() ? "" : " - Patient #" + idStr), headers, rows);
            JOptionPane.showMessageDialog(this, "PDF report saved to " + filePath);
        });

        return panel;
    }

    private void exportToCSV(JTable table, String filename) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(filename + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            // header
            StringBuilder header = new StringBuilder();
            for (int i = 0; i < model.getColumnCount(); i++) {
                header.append(model.getColumnName(i));
                if (i < model.getColumnCount() - 1) header.append(",");
            }
            pw.println(header);
            // rows
            for (int row = 0; row < model.getRowCount(); row++) {
                StringBuilder line = new StringBuilder();
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object val = model.getValueAt(row, col);
                    line.append(val != null ? val.toString().replace(",", ";") : "");
                    if (col < model.getColumnCount() - 1) line.append(",");
                }
                pw.println(line);
            }
            JOptionPane.showMessageDialog(this, "Exported to " + fc.getSelectedFile().getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Exports the currently loaded table data to a PDF using ReportGenerator
    // (iText). Satisfies the "PDF reports" requirement — used across every
    // report tab in this form.
    private void exportToPDF(JTable table, String reportTitle) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nothing to export. Click 'Load / Refresh' first.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(reportTitle + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String[] headers = new String[model.getColumnCount()];
        for (int i = 0; i < headers.length; i++) headers[i] = model.getColumnName(i);

        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String[] row = new String[model.getColumnCount()];
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object val = model.getValueAt(r, c);
                row[c] = val != null ? val.toString() : "";
            }
            rows.add(row);
        }

        String filePath = fc.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".pdf")) filePath += ".pdf";

        ReportGenerator.generateTableReport(filePath, prettifyTitle(reportTitle), headers, rows);
        JOptionPane.showMessageDialog(this, "PDF report saved to " + filePath);
    }

    // Turns a view name like "vw_pending_bills" into "Pending Bills" for the
    // PDF title, falling back to the title as given if it doesn't match the
    // vw_ naming convention.
    private String prettifyTitle(String name) {
        String base = name.startsWith("vw_") ? name.substring(3) : name;
        String[] words = base.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
