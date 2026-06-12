package hms.ui;

import hms.software.DatabaseConnection;
import hms.software.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class LoginForm extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;
    private JButton btnLogin;
    private JLabel lblStatus;

    public LoginForm() {
        setTitle("HMS - Login");
        setSize(420, 340);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel header = new JLabel("HMS Login", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        main.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 12));

        form.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        form.add(txtUsername);

        form.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        form.add(txtPassword);

        form.add(new JLabel("Role:"));
        cmbRole = new JComboBox<>(new String[]{"admin", "doctor", "staff", "receptionist"});
        form.add(cmbRole);

        form.add(new JLabel());
        btnLogin = new JButton("Login");
        btnLogin.setBackground(new Color(0, 120, 215));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        form.add(btnLogin);

        main.add(form, BorderLayout.CENTER);

        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setForeground(Color.RED);
        lblStatus.setBorder(new EmptyBorder(10, 0, 0, 0));
        main.add(lblStatus, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        add(main);

        btnLogin.addActionListener(e -> handleLogin());
        txtPassword.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String role = (String) cmbRole.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Username and password are required.");
            return;
        }

        try {
            Connection con = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT * FROM user_account WHERE username = ? AND password_hash = ? AND role = ? AND is_active = 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                SessionManager.getInstance().setUser(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("role")
                );
                dispose();
                new Dashboard().setVisible(true);
            } else {
                lblStatus.setText("Invalid credentials or role mismatch.");
            }
        } catch (SQLException ex) {
            lblStatus.setText("DB error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
