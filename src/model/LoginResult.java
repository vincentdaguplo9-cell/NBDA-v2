package model;

// Login outcome wrapper for the authentication flow.
public class LoginResult {
    private final boolean success;
    private final String message;
    private final UserAccount user;

    private LoginResult(boolean success, String message, UserAccount user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static LoginResult success(UserAccount user) {
        return new LoginResult(true, "Login successful.", user);
    }

    public static LoginResult failure(String message) {
        return new LoginResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserAccount getUser() {
        return user;
    }
}
