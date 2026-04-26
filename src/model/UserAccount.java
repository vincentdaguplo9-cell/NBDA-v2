package model;

// Authenticated user context for login and audit attribution.
public class UserAccount {
    private final int userId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String role;

    public UserAccount(int userId, String username, String firstName, String lastName, String role) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public String getDisplayName() {
        return (firstName + " " + lastName).trim();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
