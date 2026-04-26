package model;

import java.util.List;

// Small DTO for dashboard headline metrics.
public class DashboardSummary {
    private final int totalBags;
    private final int expiryWarnings;
    private final List<String> criticalStockLines;

    public DashboardSummary(int totalBags, int expiryWarnings, List<String> criticalStockLines) {
        this.totalBags = totalBags;
        this.expiryWarnings = expiryWarnings;
        this.criticalStockLines = criticalStockLines;
    }

    public int getTotalBags() {
        return totalBags;
    }

    public int getExpiryWarnings() {
        return expiryWarnings;
    }

    public List<String> getCriticalStockLines() {
        return criticalStockLines;
    }
}
