package hms.software;

public class SessionManager {

    private static SessionManager instance;
    private int logged_in_user_id;
    private String logged_in_username;
    private String logged_in_role;
    private boolean is_logged_in;

    private SessionManager() {
        is_logged_in = false;
    }

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int user_id, String username, String role) {
        this.logged_in_user_id  = user_id;
        this.logged_in_username = username;
        this.logged_in_role     = role;
        this.is_logged_in       = true;
    }

    public void logout() {
        logged_in_user_id  = 0;
        logged_in_username = null;
        logged_in_role     = null;
        is_logged_in       = false;
    }

    // setUser calls login — fixes the empty method bug
    public void setUser(int userId, String username, String role) {
        login(userId, username, role);
    }

    // clear calls logout
    public void clear() {
        logout();
    }

    public int getUserId()      { return logged_in_user_id; }
    public String getUsername() { return logged_in_username; }
    public String getRole()     { return logged_in_role; }
    public boolean isLoggedIn() { return is_logged_in; }

    public boolean hasRole(String role) {
        return logged_in_role != null && logged_in_role.equalsIgnoreCase(role);
    }
}