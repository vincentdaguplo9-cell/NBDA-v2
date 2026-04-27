# NBDA v2.1 Changes Log

## Overview

This document details all changes made for NBDA v2.1 compared to v2.0. The main goal of v2.1 was to add DOH-compliant external card ID mapping (PRC/DOH IDs) and screening status tracking.

---

## v2.1 Summary

| Aspect | v2.0 | v2.1 |
|--------|------|------|
| External ID | None | PRC/DOH ID mapping |
| Donor Source | Not tracked | External source tracking |
| Middle Name | Not tracked | Added |
| Screening Status | Not tracked | ELIGIBLE/TEMPORARILY_DEFERRED |
| NO ID Protocol | Not enforced | Auto-defer if no valid ID |

---

## Database Schema Changes

### Donors Table

```sql
-- v2.0 columns (existing)
donor_id INT PRIMARY KEY AUTO_INCREMENT
first_name VARCHAR(50) NOT NULL
last_name VARCHAR(50) NOT NULL
sex VARCHAR(10)
birth_date DATE
blood_type ENUM(...)
barangay VARCHAR(50)
contact_no VARCHAR(20)
last_successful_donation DATE

-- NEW v2.1 columns ADDED
external_card_id VARCHAR(50)      -- PRC/DOH ID (e.g., RC-12345, DOH-67890)
external_source VARCHAR(20)       -- Source: PRC, DOH, LGU, NONE
middle_name VARCHAR(50)            -- Middle name/lInitial
screening_status VARCHAR(30)       -- ELIGIBLE, TEMPORARILY_DEFERRED
decision_reason TEXT               -- Reason for deferral
next_eligible_date DATE            -- When donor can return
```

### blood_inventory Table

No schema changes - already had TTI fields:
- tti_test_method, tti_hiv, tti_hbv, tti_hcv, tti_syphilis, tti_malaria, tti_remarks

---

## Model Changes

### DohDonor.java

```java
// NEW fields added
private String externalCardId;
private String externalSource;
private String middleName;

// NEW constructor parameter order
DohDonor(Integer id, String externalCardId, String externalSource, 
         String firstName, String middleName, String lastName, ...)
```

### DonorRecord.java

```java
// NEW fields added
private final String externalCardId;
private final String externalSource;
private final String middleName;
```

---

## UI Changes

### DonorRegistration.fxml

**NEW fields added:**
- External Card ID (PRC/DOH) - TextField for input
- ID Source - ComboBox (PRC, DOH, LGU, NONE)

**GridPane layout fixed:**
- Row 0: External Card ID, ID Source
- Row 1: First Name, Last Name
- Row 2: DOB, Sex
- Row 3: Barangay, Blood Type
- Row 4: Contact (spans 2 columns)
- Row 5: Last Donation, Collection Date

### DonorRegistrationController.java

**NEW @FXML fields:**
- @FXML TextField externalCardIdField
- @FXML ComboBox<String> externalSourceCombo

**NEW initialization:**
- externalSourceCombo.items = ["PRC", "DOH", "LGU", "NONE"]

---

### RecordDonation.fxml

**NEW display-only fields added:**
- External Card ID field (shows donor's PRC/DOH ID)
- ID Source field (shows source)

### RecordDonationController.java

**NEW fields:**
- @FXML TextField externalCardIdField
- @FXML TextField externalSourceField
- private DohDonor foundDonor (stores found donor for donation)

**Behavior change:**
- Previously: Created new DohDonor with null fields
- Now: Uses foundDonor from database (preserves externalCardId)

---

## DAO Changes

### DohDonorDAO.java

#### insertDonor()
```sql
-- Updated to include new columns
INSERT INTO donors (external_card_id, external_source, first_name, middle_name, last_name, ...)
VALUES (?, ?, ?, ?, ?, ...)
```

#### evaluateEligibility()
**NEW - NO ID Protocol:**

```java
String externalId = donor.getExternalCardId();
String externalSource = donor.getExternalSource();
if (externalId == null || externalId.isBlank() || "NONE".equals(externalSource)) {
    reasons.add("Valid PRC/DOH ID required - please register with external card ID.");
}
```

This auto-defers donors who don't have a valid external card ID.

#### findDonorBySearch()
```sql
-- Updated to fetch new columns
SELECT donor_id, external_card_id, external_source, first_name, middle_name, ...
FROM donors WHERE ...
```

---

### RecordsDAO.java

#### parseDonor() / searchDonors()
- Updated constructors to accept new parameters (externalCardId, externalSource, middleName)

---

## Workflow Changes

### NO ID Protocol

1. **New Registration:** Must enter external card ID (PRC/DOH) and source
2. **Return Donation:** 
   - Search for donor → donor's externalCardId loaded from DB
   - If NO valid externalCardId → deferred with message "Valid PRC/DOH ID required"
   - Only donors with external card ID can donate

### Screening Status

- Stored in donor_screening.screening_status
- Values: ELIGIBLE, TEMPORARILY_DEFERRED
- decision_reason and next_eligible_date track deferrals

---

## Files Modified

| File | Change Type |
|------|-------------|
| schema-v2.1.sql | Created (new database schema) |
| src/model/DohDonor.java | Modified (new fields) |
| src/model/DonorRecord.java | Modified (new fields) |
| src/dao/DohDonorDAO.java | Modified (insertDonor, evaluateEligibility, findDonorBySearch) |
| src/dao/RecordsDAO.java | Modified (parseDonor, searchDonors) |
| assets/fxml/DonorRegistration.fxml | Modified (new fields, layout fix) |
| src/ui/DonorRegistrationController.java | Modified (@FXML, initialize) |
| assets/fxml/RecordDonation.fxml | Modified (new display fields) |
| src/ui/RecordDonationController.java | Modified (foundDonor tracking) |

---

## Deployment Notes

### Fresh Install (v2.1)
```sql
-- Run schema-v2.1.sql
SOURCE schema-v2.1.sql;
```

### Migration from v2.0 to v2.1
```sql
-- Add new columns to existing database
ALTER TABLE donors 
ADD COLUMN external_card_id VARCHAR(50) DEFAULT NULL,
ADD COLUMN external_source VARCHAR(20) DEFAULT 'NONE',
ADD COLUMN middle_name VARCHAR(50) DEFAULT '',
ADD COLUMN screening_status VARCHAR(30) DEFAULT 'ELIGIBLE',
ADD COLUMN decision_reason TEXT DEFAULT NULL,
ADD COLUMN next_eligible_date DATE DEFAULT NULL;
```

---

## v2.1 Complete Features

1. ✅ External Card ID mapping (PRC/DOH)
2. ✅ External Source tracking (PRC, DOH, LGU, NONE)
3. ✅ Middle name tracking
4. ✅ Screening status (ELIGIBLE/TEMPORARILY_DEFERRED)
5. ✅ NO ID Protocol - auto-defer donors without valid ID
6. ✅ Record Donation shows external ID
7. ✅ GridPane layout fixed in Donor Registration

---

## Notes

- This is a breaking change for database - requires v2.1 schema
- Donors without external_card_id will be deferred at donation time
- For legacy donors, manually add external_card_id = 'LEGACY' and external_source = 'NONE' to allow donations