package model;

import java.time.LocalDate;

// Screening history row for donor review.
public class ScreeningHistoryRecord {
    private final int screeningId;
    private final String donorName;
    private final LocalDate screeningDate;
    private final LocalDate collectionDate;
    private final String status;
    private final String decisionReason;
    private final String screenedBy;

    public ScreeningHistoryRecord(int screeningId, String donorName, LocalDate screeningDate, LocalDate collectionDate,
                                  String status, String decisionReason, String screenedBy) {
        this.screeningId = screeningId;
        this.donorName = donorName;
        this.screeningDate = screeningDate;
        this.collectionDate = collectionDate;
        this.status = status;
        this.decisionReason = decisionReason;
        this.screenedBy = screenedBy;
    }

    public int getScreeningId() {
        return screeningId;
    }

    public String getDonorName() {
        return donorName;
    }

    public LocalDate getScreeningDate() {
        return screeningDate;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public String getStatus() {
        return status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public String getScreenedBy() {
        return screenedBy;
    }
}
