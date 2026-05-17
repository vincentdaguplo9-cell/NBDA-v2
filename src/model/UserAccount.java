package model;

import java.util.Date;

// Authenticated user context for login and audit attribution.
public class UserAccount {
    private final int userId;
    private final String staffId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final boolean active;
    private final Date createdAt;

    public UserAccount(int userId, String username, String firstName, String lastName, String role) {
        this(userId, null, username, firstName, lastName, role, true, null);
    }

    public UserAccount(int userId, String staffId, String username, String firstName, String lastName, String role, boolean active, Date createdAt) {
        this.userId = userId;
        this.staffId = staffId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getUserId() {
        return userId;
    }

    public String getStaffId() {
        return staffId;
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

    public boolean isActive() {
        return active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getDisplayName() {
        return (firstName + " " + lastName).trim();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
