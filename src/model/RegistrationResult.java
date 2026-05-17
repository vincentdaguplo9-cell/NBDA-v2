package model;

// Outcome object for the donor registration + blood bag creation workflow.
public class RegistrationResult {
    public enum Outcome {
        ELIGIBLE,
        DEFERRED,
        ERROR
    }

    private final boolean success;
    private final Outcome outcome;
    private final String message;
    private final String bagId;

    private RegistrationResult(boolean success, Outcome outcome, String message, String bagId) {
        this.success = success;
        this.outcome = outcome;
        this.message = message;
        this.bagId = bagId;
    }

    public static RegistrationResult eligible(String message, String bagId) {
        return new RegistrationResult(true, Outcome.ELIGIBLE, message, bagId);
    }

    public static RegistrationResult deferred(String message) {
        return new RegistrationResult(false, Outcome.DEFERRED, message, null);
    }

    public static RegistrationResult error(String message) {
        return new RegistrationResult(false, Outcome.ERROR, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isDeferred() {
        return outcome == Outcome.DEFERRED;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getMessage() {
        return message;
    }

    public String getBagId() {
        return bagId;
    }
}
