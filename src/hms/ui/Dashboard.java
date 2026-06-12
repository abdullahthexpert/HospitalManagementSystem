package hms.ui;

import hms.software.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Dashboard extends JFrame {

    private JPanel contentArea;

    public Dashboard() {
        setTitle("HMS - Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        JMenuItem exitItem   = new JMenuItem("Exit");
        logoutItem.addActionListener(e -> {
            SessionManager.getInstance().clear();
            dispose();
            new LoginForm().setVisible(true);
        });
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.setBackground(new Color(0, 120, 215));
        topBar.setPreferredSize(new Dimension(1100, 36));
        JLabel userLabel = new JLabel(
            "Logged in: " + SessionManager.getInstance().getUsername() +
            "  |  Role: " + SessionManager.getInstance().getRole() + "   "
        );
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        topBar.add(userLabel);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 30, 60));
        sidebar.setPreferredSize(new Dimension(190, 700));
        sidebar.setBorder(new EmptyBorder(16, 8, 16, 8));

        JLabel appTitle = new JLabel("HMS");
        appTitle.setForeground(Color.WHITE);
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(appTitle);
        sidebar.add(Box.createVerticalStrut(20));

        contentArea = new JPanel(new BorderLayout());
        JLabel placeholder = new JLabel("Select a module from the sidebar", SwingConstants.CENTER);
        placeholder.setFont(new Font("SansSerif", Font.PLAIN, 15));
        placeholder.setForeground(Color.GRAY);
        contentArea.add(placeholder, BorderLayout.CENTER);

        String[] modules = {"Patients", "Doctors", "Appointments", "Billing", "Lab", "Pharmacy", "Staff", "Reports"};

        for (String module : modules) {
            JButton btn = new JButton(module);
            btn.setMaximumSize(new Dimension(174, 38));
            btn.setMinimumSize(new Dimension(174, 38));
            btn.setPreferredSize(new Dimension(174, 38));
            btn.setBackground(new Color(50, 50, 100));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
            btn.addActionListener(e -> openModule(module));
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(6));
        }

        add(topBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);
    }

    private void openModule(String module) {
        contentArea.removeAll();
        JPanel panel;
        switch (module) {
            case "Patients":     panel = new PatientForm();     break;
            case "Doctors":      panel = new DoctorForm();      break;
            case "Appointments": panel = new AppointmentForm(); break;
            case "Billing":      panel = new BillingForm();     break;
            case "Lab":          panel = new LabForm();         break;
            case "Pharmacy":     panel = new PharmacyForm();    break;
            case "Staff":        panel = new StaffForm();       break;
            case "Reports":      panel = new ReportsForm();     break;
            default:
                JLabel lbl = new JLabel(module + " — coming soon", SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 16));
                lbl.setForeground(Color.GRAY);
                contentArea.add(lbl, BorderLayout.CENTER);
                contentArea.revalidate();
                contentArea.repaint();
                return;
        }
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
}
