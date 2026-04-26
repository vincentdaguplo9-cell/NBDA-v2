package model;

// Generic action result for bag release and issuance.
public class InventoryActionResult {
    private final boolean success;
    private final String message;

    private InventoryActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static InventoryActionResult success(String message) {
        return new InventoryActionResult(true, message);
    }

    public static InventoryActionResult failure(String message) {
        return new InventoryActionResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
