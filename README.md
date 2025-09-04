
# 📞 Telecom Billing System (CLI-Based)

A **Command-Line Interface (CLI)** application that simulates a **telecom billing system**, built using **Core Java** and **Oracle SQL**.  
This project demonstrates **OOP principles**, **JDBC connectivity**, and **menu-driven operations** for **customer management, call simulation, billing, and reporting**.

---

## ✅ Features

### **For Customers**
- **Login & Profile Management**
- **Make Calls, Send SMS, Use Data**
- **View Bills & Balance**
- **Pay Bills (Postpaid) / Recharge (Prepaid)**

### **For Admin**
- **Manage Customers** (Add, Update, Delete)
- **Manage Plans** (Prepaid & Postpaid)
- **Generate Reports**
    - Top 5 Customers by Usage
    - Monthly Revenue
    - Most Popular Plan
    - Defaulters (Pending Payments)

### **Other Features**
- Role-based access (**Admin & Customer**)
- **GST calculation & discounts** in billing
- **Logging** of transactions (calls, payments, bills)
- **Password hashing** for secure login
- **Transaction management** for reliable billing

---

## 🛠 Tech Stack
- **Language:** Core Java (JDK 8+)
- **Database:** Oracle SQL
- **JDBC:** For database connectivity
- **CLI:** Menu-driven interface
- **Logging:** Java Logging API / File-based logs

---

## 🏗 Project Structure
```
TelecomBillingSystem/
├── src/
│   ├── model/         # Entity classes (Customer, Plan, CallRecord, etc.)
│   ├── dao/           # Database access classes (JDBC)
│   ├── service/       # Business logic (Billing, Payment, Reports)
│   ├── cli/           # CLI menu and user interaction
│   └── Main.java      # Entry point
├── resources/
│   └── db.properties  # Database configuration
└── README.md
```

---

## 🗄 Database Schema

### **Tables**
- `customers (customer_id, name, phone, plan_id, type, balance, password)`
- `plans (plan_id, name, call_rate, sms_rate, data_rate, validity)`
- `call_records (id, customer_id, duration, type, timestamp, cost)`
- `sms_records (id, customer_id, timestamp, cost)`
- `data_usage (id, customer_id, data_used_mb, timestamp, cost)`
- `bills (bill_id, customer_id, amount, due_date, status)`
- `payments (id, bill_id, amount, date, method)`

---

## 🔑 Core Functional Flow

### **Main Menu**
```
1. Admin Login
2. Customer Login
3. Exit
```

### **Admin Menu**
```
1. Manage Customers
2. Manage Plans
3. Generate Reports
4. Logout
```

### **Customer Menu**
```
1. View Profile
2. Make a Call
3. Send SMS
4. Use Data
5. View Bill / Balance
6. Pay Bill / Recharge
7. Logout
```

---

## ✅ How to Run
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/telecom-billing-system.git
   cd telecom-billing-system
   ```
2. Configure **Oracle Database**:
   - Import schema and tables from `resources/db-schema.sql`.
   - Update `resources/db.properties` with your DB credentials.
3. Compile and run:
   ```bash
   javac -d bin src/**/*.java
   java -cp bin Main
   ```

---

## 👥 Team Members
- **Member 1:** Database Design & JDBC Utility
- **Member 2:** Customer Management
- **Member 3:** Plan Management & Reports
- **Member 4:** Usage Simulation (Calls, SMS, Data)
- **Member 5:** Billing, Payments & Integration

---

## ✅ Future Enhancements
- Export reports as PDF/Excel.
- Implement OTP-based customer login.
- Add multi-threading for concurrent call simulation.
- Add graphical UI (JavaFX / Web).

---

## 📄 License
This project is developed for **educational purposes** as part of **Amdocs Training**.
