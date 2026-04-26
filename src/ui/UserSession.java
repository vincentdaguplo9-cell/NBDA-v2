package ui;

import model.UserAccount;

// In-memory session for the currently authenticated operator.
public final class UserSession {
    private static UserAccount currentUser;

    private UserSession() {
    }

    public static void login(UserAccount user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static UserAccount getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
