package model;

// Captures TTI results before a quarantined unit is released or discarded.
public class TtiScreeningInput {
    private final String hiv;
    private final String hbv;
    private final String hcv;
    private final String syphilis;
    private final String malaria;
    private final String testKit;
    private final String remarks;

    public TtiScreeningInput(String hiv, String hbv, String hcv, String syphilis, String malaria, String testKit, String remarks) {
        this.hiv = hiv;
        this.hbv = hbv;
        this.hcv = hcv;
        this.syphilis = syphilis;
        this.malaria = malaria;
        this.testKit = testKit;
        this.remarks = remarks;
    }

    public String getHiv() {
        return hiv;
    }

    public String getHbv() {
        return hbv;
    }

    public String getHcv() {
        return hcv;
    }

    public String getSyphilis() {
        return syphilis;
    }

    public String getMalaria() {
        return malaria;
    }

    public String getTestKit() {
        return testKit;
    }

    public String getRemarks() {
        return remarks;
    }

    public boolean allNonReactive() {
        return "NON_REACTIVE".equalsIgnoreCase(hiv)
                && "NON_REACTIVE".equalsIgnoreCase(hbv)
                && "NON_REACTIVE".equalsIgnoreCase(hcv)
                && "NON_REACTIVE".equalsIgnoreCase(syphilis)
                && "NON_REACTIVE".equalsIgnoreCase(malaria);
    }
}
