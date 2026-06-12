import hms.software.DatabaseConnection;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Connection con = DatabaseConnection.getInstance().getConnection();
        if (con != null) {
            System.out.println("Connected to HMS database successfully!");
        } else {
            System.out.println("Connection failed. Check password and MySQL server.");
        }
    }
}