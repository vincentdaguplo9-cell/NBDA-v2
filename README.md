# NBDA - Naval Blood Donation Archive

A DOH-compliant JavaFX desktop application for managing blood donor registration, donation screening, blood bag inventory, and issuance for the Naval Blood Donation Center in Naval, Biliran, Philippines.

## Features

### Core Workflow
- **Donor Registration** - Register new donors with personal information, contact details, and blood type
- **Donation Screening** - Pre-donation health screening with vital signs validation
- **Blood Bag Management** - Track bags from collection through quarantine, TTI screening, release, and expiry
- **Issuance** - Process blood release requests with patient and crossmatch details
- **System Logs** - Audit trail for all inventory actions

### Donor Eligibility Checks
- Age verification (17-65 years)
- Guardian consent for 16-17 year olds
- Weight minimum (50 kg)
- 3-month donation interval
- Blood pressure, pulse, temperature, and hemoglobin thresholds
- Checklist: sleep, meal, alcohol, illness, tattoos, recent operations

### Inventory Tracking
- Whole blood only
- 35-day expiry window
- QUARANTINE status after collection
- TTI screening required for release
- Issued or expired status tracking

## Technology Stack

- **JavaFX 17** - UI framework
- **MySQL** - Database
- **Java 25** - Runtime

## Project Structure

```
Naval-Blood-Donation-Archive/
├── src/
│   ├── Main.java                    # Application entry point
│   ├── database/
│   │   ├── DBConnection.java      # MySQL connection manager
│   │   └── SchemaInitializer.java # Auto schema setup
│   ├── model/
│   │   ├── BloodBagRecord.java   # Blood bag data model
│   │   ├── DohDonor.java        # Donor data model
│   │   ├── UserAccount.java      # User account model
│   │   ├── ScreeningInput.java   # Pre-donation screening input
│   │   ├── TtiScreeningInput.java # TTI screening input
│   │   └── ...                 # Other result models
│   ├── dao/
│   │   ├── AuthDAO.java         # Authentication
│   │   ├── DohDonorDAO.java    # Donor operations
│   │   ├── DohInventoryDAO.java # Inventory operations
│   │   ├── RecordsDAO.java     # Issuance records
│   │   └── AuditDAO.java       # Audit log
│   └── ui/
│       ├── LoginController.java       # Login screen
│       ├── AppShellController.java   # Main layout with sidebar
│       ├── DashboardController.java   # Dashboard with KPIs and charts
│       ├── DonorRegistrationController.java # New donor registration
│       ├── RecordDonationController.java # Record return donation
│       ├── InventoryViewController.java   # Blood bag inventory
│       ├── RecordsViewController.java      # System logs
│       ├── DonorHistoryViewController.java # Donor history
│       └── FxmlView.java           # FXML resource helper
├── assets/
│   ├── css/
│   │   └── nbda-modern.css      # Application styles
│   ├── fxml/
│   │   ├── Login.fxml            # Login screen
│   │   ├── AppShell.fxml         # Main layout
│   │   ├── Dashboard.fxml        # Dashboard
│   │   ├── DonorRegistration.fxml # New donor registration
│   │   ├── RecordDonation.fxml   # Record return donation
│   │   ├── InventoryView.fxml    # Blood bag inventory
│   │   ├── RecordsView.fxml      # System logs
│   │   └── DonorHistoryView.fxml  # Donor history
│   └── img/
│       └── bg.jpg              # Login background image
├── config/
│   └── db.properties          # Database configuration
├── blood_archive.sql          # Database schema
├── schema.sql               # Alternative schema reference
├── compile.ps1             # Compile script
├── run.ps1                 # Run script
└── build-run.bat            # Build and run batch
```

## Screens

### Login
Username and password authentication. Default accounts:
- `admin` / `admin`
- `staff` / `staff`

### Dashboard
Overview with:
- Total Bags KPI
- Available Bags KPI
- Pending TTI Screening KPI
- Total Donors KPI
- Blood Stock by Type (Bar Chart)
- Inventory Distribution (Pie Chart)
- Recent System Activity

### New Donor Registration
- Donor profile (name, DOB, sex, barangay, blood type, contact)
- Medical screening (weight, BP, hemoglobin, pulse, temperature)
- Readiness checklist
- Validation and registration

### Record Return Donation
- Search returning donor
- Donor info display (read-only)
- Medical screening entry
- Wait time calculation

### Blood Archive (Inventory)
- All bags view with status filters
- QUARANTINE bags for TTI screening
- Available bags for release
- Released and issued bags tracking

### System Logs
- Audit trail of all inventory actions
- Date range filtering
- Action type filtering

## Database Schema

Tables:
- `users` - User accounts for authentication
- `donors` - Donor information
- `donor_screening` - Screening history
- `blood_inventory` - Blood bag records
- `audit_log` - System audit trail

## Setup

### Prerequisites
1. Java 17 or higher
2. MySQL 5.7 or higher
3. JavaFX SDK

### Configuration
Edit `config/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/blood_archive?useSSL=false&serverTimezone=UTC
db.user=root
db.pass=YOUR_PASSWORD
```

### Running the App

**Using PowerShell:**
```powershell
.\run.ps1
```

**Using Batch:**
```batch
build-run.bat
```

**Manual Compile and Run:**
```powershell
.\compile.ps1
java --module-path "C:\Program Files\Java\javafx-sdk-25.0.2\lib" --add-modules javafx.controls,javafx.graphics,javafx.fxml -cp build\classes Main
```

## Usage Workflow

### Register New Donor
1. Login with admin or staff account
2. Click "New Registration" in sidebar
3. Fill donor profile information
4. Enter medical screening values
5. Complete readiness checklist
6. Click "Register & Verify"

### Record Return Donation
1. Click "Record Return" in sidebar
2. Search for donor by name or contact
3. Verify eligibility status
4. Enter collection date and volume
5. Enter vital signs
6. Click "Record Donation"

### Process Blood Bag
1. Click "Blood Archive" in sidebar
2. View bags by status (QUARANTINE, AVAILABLE, RELEASED, ISSUED, EXPIRED)
3. For QUARANTINE bags: Run TTI screening
4. Release or discard as needed

### View System Logs
1. Click "System Logs" in sidebar
2. Filter by date range or action type
3. Review audit trail

## Build Notes

- The compile step may show a `java.nio.file.AccessDeniedException` involving the MySQL connector JAR on some Windows setups - this does not affect the compiled output.
- Auto schema initialization runs on startup.
- The app opens in fullscreen mode by default.

## License

This application is developed for the Naval Blood Donation Center, Naval, Biliran, Philippines.