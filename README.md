# Michelin Son Tay Garage Management System (MST-GMS)

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-Clean_Architecture-brightgreen.svg)
![React](https://img.shields.io/badge/React.js-Frontend-blue.svg)
![MySQL](https://img.shields.io/badge/MySQL-70%2B_Tables-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)

## 📌 Introduction

The purpose of this system is to provide a comprehensive and integrated software platform for an authorized Michelin dealer in Son Tay. It supports the operational management of the garage in an efficient, secure, and transparent manner, handling end-to-end workflows including booking, service ticket processing, and warehouse management.

**Live System:** [sontaygarage.vn](https://sontaygarage.vn/)

---

## 📑 Table of Contents
- [Technologies](#️-technologies)
- [Actors & Roles](#-actors--roles)
- [Documentation](#-documentation)
- [Getting Started](#-getting-started)
  - [Requirements](#1-requirements)
  - [Setup Guide](#2-setup-guide)
- [Deployment](#-deployment)

---

## 🛠️ Technologies

### Backend
- **Core**: Java 17, Spring Boot
- **Architecture**: Clean Architecture, Domain-Driven Design
- **Real-time & Notifications**: WebSocket/STOMP, Zalo OA API
- **Authentication**: Google OAuth2
- **Hardware Integration**: Hikvision ISAPI

### Frontend & Storage
- **Frontend**: React.js
- **Storage**: Cloudinary

### Database & DevOps
- **Database**: MySQL (Scalable schema with 70+ tables)
- **Deployment**: Docker, Linux Server

---

## 👥 Actors & Roles

- **Admin/Manager**: Manage employee accounts, oversee warehouse inventory, configure promotions and tax logic, view reports, and manage garage operations.
- **Service Advisor/Staff**: Create and manage service tickets, assign tasks to mechanics, check stock availability, and process payments.
- **Mechanic**: View assigned service tickets, update task statuses, and request parts from the warehouse.
- **Customer**: Book service appointments online, authenticate via Google OAuth2, and receive real-time updates and notifications.
- **System**: Calculate dynamic pricing and taxes (e.g., re-applying tax upon promotion removal), trigger Zalo OA notifications via WebSocket, manage media uploads to Cloudinary, and interface with Hikvision cameras.

---

## 📚 Documentation

The following resources and links are available:
- 🌐 **Live Website**: [https://sontaygarage.vn/](https://sontaygarage.vn/)
- 📁 **Documentation Folder**: Check the `/docs` folder for:
  - Database Schema (SQL)
  - Architectural sequence diagrams (numbered sequentially with whole integers).

---

## 🚀 Getting Started

### 1. Requirements

Before running the project, ensure you have the following installed:
- **Java**: JDK 17 (`JAVA_HOME` configured)
- **Node.js**: Environment for React frontend
- **Database**: MySQL Server & MySQL Workbench (or similar client)
- **DevOps**: Docker & Docker Compose
- **IDE**: IntelliJ IDEA / Eclipse (Backend) & Visual Studio Code (Frontend)

### 2. Setup Guide

#### (A) Database Setup
1. Open MySQL Workbench.
2. Execute the provided initialization script in `docs/schema.sql` to create the 70+ tables.
3. Verify the database has been created and populated with initial setup data.

#### (B) Backend Setup (Spring Boot)
1. Open the backend project folder in IntelliJ IDEA.
2. Navigate to `src/main/resources/application.properties` (or `.yml`) and configure the environment variables:
```properties
   # Example Configurations
   spring.datasource.url=jdbc:mysql://localhost:3306/mst_gms
   spring.datasource.username=your_db_username
   spring.datasource.password=your_db_password
   cloudinary.api_key=your_api_key
   google.oauth2.client_id=your_client_id
   zalo.oa.api_key=your_zalo_api_key
