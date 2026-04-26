package model;

import java.time.LocalDate;

// Encapsulates the active donor-screening form for one intended collection.
public class ScreeningInput {
    private final LocalDate collectionDate;
    private final int collectionVolumeMl;
    private final double weightKg;
    private final int systolicBp;
    private final int diastolicBp;
    private final int pulseBpm;
    private final double temperatureC;
    private final double hemoglobinGdl;
    private final double sleptHours;
    private final boolean guardianConsentProvided;
    private final boolean hadMeal;
    private final boolean alcoholInLast24h;
    private final boolean hasFeverCoughColds;
    private final boolean hadTattooOrPiercingLast12Months;
    private final boolean hadRecentOperation;
    private final boolean currentlyPregnant;

    public ScreeningInput(LocalDate collectionDate,
                          int collectionVolumeMl,
                          double weightKg,
                          int systolicBp,
                          int diastolicBp,
                          int pulseBpm,
                          double temperatureC,
                          double hemoglobinGdl,
                          double sleptHours,
                          boolean guardianConsentProvided,
                          boolean hadMeal,
                          boolean alcoholInLast24h,
                          boolean hasFeverCoughColds,
                          boolean hadTattooOrPiercingLast12Months,
                          boolean hadRecentOperation,
                          boolean currentlyPregnant) {
        this.collectionDate = collectionDate;
        this.collectionVolumeMl = collectionVolumeMl;
        this.weightKg = weightKg;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.pulseBpm = pulseBpm;
        this.temperatureC = temperatureC;
        this.hemoglobinGdl = hemoglobinGdl;
        this.sleptHours = sleptHours;
        this.guardianConsentProvided = guardianConsentProvided;
        this.hadMeal = hadMeal;
        this.alcoholInLast24h = alcoholInLast24h;
        this.hasFeverCoughColds = hasFeverCoughColds;
        this.hadTattooOrPiercingLast12Months = hadTattooOrPiercingLast12Months;
        this.hadRecentOperation = hadRecentOperation;
        this.currentlyPregnant = currentlyPregnant;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public int getCollectionVolumeMl() {
        return collectionVolumeMl;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public int getSystolicBp() {
        return systolicBp;
    }

    public int getDiastolicBp() {
        return diastolicBp;
    }

    public int getPulseBpm() {
        return pulseBpm;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    public double getHemoglobinGdl() {
        return hemoglobinGdl;
    }

    public double getSleptHours() {
        return sleptHours;
    }

    public boolean isGuardianConsentProvided() {
        return guardianConsentProvided;
    }

    public boolean isHadMeal() {
        return hadMeal;
    }

    public boolean isAlcoholInLast24h() {
        return alcoholInLast24h;
    }

    public boolean isHasFeverCoughColds() {
        return hasFeverCoughColds;
    }

    public boolean isHadTattooOrPiercingLast12Months() {
        return hadTattooOrPiercingLast12Months;
    }

    public boolean isHadRecentOperation() {
        return hadRecentOperation;
    }

    public boolean isCurrentlyPregnant() {
        return currentlyPregnant;
    }

    public String getBloodPressureDisplay() {
        return systolicBp + "/" + diastolicBp;
    }
}
