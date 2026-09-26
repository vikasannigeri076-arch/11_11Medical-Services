# 🏥 11_11 Medical Services

> **A smart pharmacy marketplace connecting customers with nearby pharmacies for medicine availability, prescription-based orders, pricing, payment, and delivery.**

---

## 📌 Overview

**11_11 Medical Services** is a smart medicine and pharmacy platform designed to make the process of finding and ordering medicines easier for customers.

Instead of visiting multiple pharmacies to check whether a medicine is available, a customer can **upload a prescription and send a medicine request to nearby pharmacies**.

Pharmacies can review the prescription, check medicine availability, provide the total price and preparation/delivery information, and respond to the customer.

The customer can then **compare pharmacy responses and select the preferred pharmacy** to place the order.

---

## 🎯 Problem Statement

Finding medicines can be difficult when:

* A medicine is unavailable at a nearby pharmacy.
* Customers have to contact multiple pharmacies.
* Prescription-based medicine requests require additional verification.
* Customers cannot easily compare prices between pharmacies.
* There is no simple way to coordinate the pharmacy, customer, and delivery process.

### 💡 Proposed Solution

11_11 Medical Services provides a single platform where:

**Customer → Prescription → Nearby Pharmacies → Offers → Selection → Payment → Delivery**

This creates a more organized medicine-ordering workflow.

---

## ✨ Key Features

### 👤 Customer

* 📱 Phone-based authentication
* 🌐 Language selection
* 📍 Location/address support
* 📄 Prescription upload
* 💊 Medicine request
* 🏪 Receive responses from nearby pharmacies
* 💰 Compare pharmacy prices
* ⏱️ Compare preparation/delivery information
* 🛒 Select a preferred pharmacy
* 💳 UPI payment
* 💵 Cash on Delivery
* 📦 Order tracking
* 🧾 View bill details
* 📜 Order history
* ⚙️ Customer settings

### 🏪 Pharmacy / Shop Owner

* 🔐 Shop owner authentication
* 📝 Shop registration
* 📋 Receive customer medicine requests
* 📄 View customer prescriptions
* 💊 Check medicine availability
* 💰 Enter medicine subtotal
* 🧾 Provide bill information
* 📸 Upload bill
* ⏱️ Provide preparation/delivery time
* ✅ Approve orders
* ❌ Cancel orders
* 🚚 Update order status
* 📜 View order history

### 💳 Payment

The platform supports:

* **UPI Payment**
* **Cash on Delivery**

For UPI orders, the customer completes payment before the order proceeds through the approval and tracking workflow.

---

## 🔄 Order Workflow

```text
Customer
   │
   │ Upload Prescription
   ▼
Medicine Request
   │
   ▼
Nearby Pharmacies
   │
   ├───────────────┐
   │               │
   ▼               ▼
Pharmacy A      Pharmacy B
   │               │
   │ Price         │ Price
   │ Availability  │ Availability
   │ Time          │ Time
   └───────┬───────┘
           │
           ▼
    Customer Compares
           │
           ▼
   Select Preferred Pharmacy
           │
           ▼
      Payment / COD
           │
           ▼
   Pharmacy Confirmation
           │
           ▼
      Order Tracking
           │
           ▼
        Delivery
```

---

## 👥 User Roles

| Role                | Responsibility                                                          |
| ------------------- | ----------------------------------------------------------------------- |
| 👤 Customer         | Upload prescription, compare pharmacy responses, place and track orders |
| 🏪 Pharmacy         | Verify prescription, provide availability and pricing, process orders   |
| 🚚 Delivery Partner | Handle medicine delivery and order movement                             |

---

## 🧠 How It Works

### 1. Customer Request

The customer uploads a prescription and submits a medicine request.

### 2. Pharmacy Response

Nearby pharmacies receive the request and review the prescription.

The pharmacy can provide:

* Medicine availability
* Medicine subtotal
* Bill details
* Preparation/delivery time

### 3. Customer Comparison

The customer receives responses from pharmacies and can compare the available options.

### 4. Pharmacy Selection

The customer selects a preferred pharmacy.

Unselected pharmacy requests can be removed from the active order workflow after selection.

### 5. Payment

The customer can choose:

* UPI
* Cash on Delivery

### 6. Order Processing

The selected pharmacy reviews and approves the order.

### 7. Delivery

After approval, the order proceeds through the delivery and tracking workflow.

---

## 🛠️ Technology Stack

| Technology                     | Usage                                   |
| ------------------------------ | --------------------------------------- |
| **Kotlin**                     | Android application development         |
| **Android Studio**             | Development environment                 |
| **XML**                        | Android user interface                  |
| **Firebase Authentication**    | User authentication                     |
| **Firebase Realtime Database** | Application data management             |
| **Firebase Storage**           | Prescription and image storage          |
| **Google Maps**                | Location and map functionality          |
| **Gradle / Kotlin DSL**        | Project build and dependency management |

---

## 🏗️ Application Structure

The application is organized around customer and pharmacy workflows.

```text
11_11MedicalServices
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── res/
│   │   │
│   │   ├── androidTest/
│   │   └── test/
│   │
│   └── build.gradle.kts
│
├── gradle/
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

---

## 🔐 Security & Data

The application uses Firebase services for authentication, database operations, and storage.

Prescription and order-related information should be handled securely and only exposed to the users and pharmacies involved in the relevant order workflow.

> **Note:** Production deployment should include appropriate security rules, access control, validation, and protection of sensitive configuration.

---

## 📱 Application Modules

### Customer Side

```text
Language Selection
       ↓
Customer Login / Signup
       ↓
Customer Home
       ↓
Medicine Request
       ↓
Prescription Upload
       ↓
Available Shops
       ↓
Shop Selection
       ↓
Payment
       ↓
Order Tracking
       ↓
Order History
```

### Pharmacy Side

```text
Shop Login / Signup
       ↓
Shop Dashboard
       ↓
Customer Requests
       ↓
Prescription Review
       ↓
Medicine Availability
       ↓
Bill Details
       ↓
Order Approval
       ↓
Order Processing
```

---

## 📸 Screenshots

Screenshots of the application will be added here to demonstrate the major customer and pharmacy workflows.

### Customer App

| Screen               | Preview     |
| -------------------- | ----------- |
| Customer Login       | Coming soon |
| Customer Home        | Coming soon |
| Prescription Upload  | Coming soon |
| Available Pharmacies | Coming soon |
| Payment              | Coming soon |
| Order Tracking       | Coming soon |

### Pharmacy App

| Screen               | Preview     |
| -------------------- | ----------- |
| Pharmacy Login       | Coming soon |
| Pharmacy Dashboard   | Coming soon |
| Customer Requests    | Coming soon |
| Prescription Details | Coming soon |
| Bill Details         | Coming soon |
| Order Management     | Coming soon |

---

## 🚀 Future Scope

Potential future improvements include:

* 🤖 AI-assisted prescription and medicine information
* 🗺️ Improved real-time delivery tracking
* 🔔 Push notifications
* 📊 Advanced pharmacy analytics
* 🌐 Multi-language expansion
* 📱 iOS and web applications
* 🚚 Dedicated delivery-partner application
* 💊 Additional medicine information and guidance
* 🔒 Enhanced production-level security and access control

---

## 🎓 Project Purpose

11_11 Medical Services is developed as a practical technology project focused on improving the connection between **customers, pharmacies, and medicine delivery services**.

The project explores how mobile applications, cloud services, location technology, and digital payment systems can be combined to create a more convenient pharmacy-ordering workflow.

---

## 👨‍💻 Developer

**Vikas Annigeri**

Android Application Developer & Project Creator

---

## 📄 License

License information will be added based on the project's intended distribution and usage.

---

## ⭐ Support

If you find this project interesting, consider giving the repository a ⭐ on GitHub.
