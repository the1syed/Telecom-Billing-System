# Telecom Billing System

A **Telecom Billing System** developed in **Java** with **Oracle/MySQL** database integration.  
The system manages **customers, plans, and billing**, providing functionalities for both **admin** and **customers**.

---

## Overview
This system simulates the core operations of a telecom service provider.  
It supports operations such as:
- Customer registration and management
- Plan creation and management
- Billing calculations based on usage
- Viewing detailed customer and plan information

---

## Features

### Admin
- Add / Remove / Update **Customer**
- Add / Remove / Update **Plan**
- View **All Customers** and **All Plans**
- Generate **Customer Bills**

### Customer
- View **My Details**
- View **My Plan**
- Check **Billing History**
- Update **Personal Details**

---

## Technology Stack
- **Programming Language:** Java
- **Database:** Oracle / MySQL
- **Build Tool:** Maven / Gradle (optional)
- **IDE:** Eclipse / IntelliJ IDEA
- **Version Control:** Git

---
## 🏗️ Architecture  

>admin

    >AdminBillingService.java

    
    >AdminCommon.java
    
    
    >AdminCustomerService.java
    
    
    >AdminMenu.java
    
    
    >AdminPlanService.java
    
    
    >AdminService.java
    
    
    >AdminSupportService.java
    
    
    >AdminUsageService.java

    
>customer

    >AccountService.java
    
    
    >AuthService.java
    
    
    >BillingService.java
    
    
    >CustomerMenu.java
    
    
    >CustomerService.java
    
    
    >FraudService.java
    
    
    >PlanService.java
    
    
    >RechargeService.java
    
    
    >RegistrationService.java
    
    
    >SupportService.java

    
    >UsageService.java


>utils


    >DataViewFormatter.java
    
    
    >DBConnection.java
   
    >InputUtil.java

    
>readme.md


>SchemaInitializer.java



>TelecomBillingSystem.java

## Setup

1. **Clone the repository**
```bash
git clone https://github.com/the1syed/Telecom-Billing-System.git
cd Telecom-Billing-System

2. **Database Setup
- Create a database in Oracle/MySQL.
- Run the SQL script to create tables and insert sample data.
- Configure DB Connection
- Update the database connection details (URL, username, password) in the DB connection class.
- Build and Run
- Compile the project:

```bash
javac -d bin src/**/*.java

- Run the main class:
```bash
java -cp bin Main


## Usage

- Admin Login: Use credentials defined in the DB or add your own.
- Customer Login: Use customer ID or credentials defined in the DB.
- Navigate using the console menu to perform operations.
---

## 💡 Concepts Used  

- **Core Java**: Loops, Conditionals, Exception Handling  
- **OOP**: Classes, Objects, Inheritance, Abstraction, Encapsulation  
- **JDBC**: Database connectivity (Statement & PreparedStatement)  
- **Exception Handling**: Try-catch for user input (`NumberFormatException`, `SQLException`)  
- **SQL**: CRUD operations, Joins, Group By, Having  


