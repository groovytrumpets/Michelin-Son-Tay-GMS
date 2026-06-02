# Michelin Son Tay Garage Management System (MST-GMS)

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-Clean_Architecture-brightgreen.svg)
![React](https://img.shields.io/badge/React.js-Frontend-blue.svg)
![MySQL](https://img.shields.io/badge/MySQL-70%2B_Tables-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)

## 📌 Introduction

The purpose of this system is to provide a comprehensive and integrated software platform for an authorized Michelin dealer in Son Tay. It supports the operational management of the garage in an efficient, secure, and transparent manner, handling end-to-end workflows including booking, service ticket processing, and warehouse management.

**Live System:** [sontaygarage.vn](https://sontaygarage.vn/)  | **Frontend Repo:** [Click Here](https://github.com/HDTVux/Michelin-Son-Tay-GMS-FrontEnd.git)

---

## 📑 Table of Contents
- [Technologies](#️-technologies)
- [Video Demo](#-video-demo)
- [Actors & Roles](#-actors--roles)
- [Documentation](#-documentation)
- [Getting Started](#getting-started)
- [Deployment](#-deployment)
- [Development Team](#development-team)

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

## 🎥 Video Demo

Watch the full demonstration of the MST-GMS platform in action, covering both customer booking workflows and internal garage management:

[![MST-GMS Video Demo](https://img.youtube.com/vi/PLhQPTBqhWuI3h1bP_WRcivySEzC3-EOyI/maxresdefault.jpg)](https://www.youtube.com/playlist?list=PLhQPTBqhWuI3h1bP_WRcivySEzC3-EOyI "Click to Watch Video Demo")

---

## 👥 Actors & Roles

- **Customer**: Use services: book appointments, track progress, and give feedback.
- **Receptionist**: Manage bookings, check-in, service tickets, and handle customer care.
- **Technician**: Perform assigned tasks and update service progress.
- **Advisor**: Review requests, create estimates, assign technicians, and ensure service quality.
- **Accountant**: Manage billing, payments.
- **Warehouse Keeper**: Manage warehouse operations, inventory, import, export items.
- **Manager**: Oversee operations, staff, pricing, and reports.
- **Admin**: Manage users, roles, permissions, and system security.

---

## 📚 Documentation

The following resources and links are available:
- 🌐 **Live Website**: [https://sontaygarage.vn/](https://sontaygarage.vn/)
- 📁 **Documentation Folder**: Check the `/docs` folder or [Google Drive](https://drive.google.com/drive/folders/13LBJXMy4XoLQMktEPq-fzLSuzgLIIruJ) for:
  - Database Schema (SQL)
  - Architectural sequence diagrams (numbered sequentially with whole integers).
    
---
<a id="development-team"></a>
## 👨‍💻 Development Team

### Phase 2: Active Maintainers & Core Team
Following the successful graduation phase, the system is actively maintained, refactored, and scaled by the core engineering team:
- **Nguyễn Nam Khánh** (Leader / System Architect)
- **Lê Trọng Tấn** (Backend Engineer)
- **Đỗ Đăng Hiệp** (Business Analyst)

### Phase 1: Graduation Project Phase (Original Team)
This project was initially established and successfully defended as a university graduation project from January to May 2026 by **Group 42 (Software Engineering)** from **FPT University**, under the valuable supervision and guidance of **Ms. Tran Thu Thuy**. 

*We acknowledge and appreciate the foundational contributions of the original 5-member team:*
| Avatar | Member | Role | Core Responsibilities | GitHub |
| :---: | :--- | :--- | :--- | :---: |
| <img src="https://avatars.githubusercontent.com/u/166611562?v=4" width="45" height="45" style="border-radius:50%;"/> | **Nguyễn Nam Khánh** | **Team Leader**<br>Backend Engineer | Overall project management, Backend architecture, core services design, and system deployment. | [![GitHub](https://img.shields.io/badge/-Profile-181717?style=flat&logo=github)](https://github.com/groovytrumpets) |
| <img src="https://github.com/github.png" width="45" height="45" style="border-radius:50%;"/> | **Đỗ Đăng Hiệp** | QA/Tester/BA | Test case creation, bug tracking, ensuring software quality, and clarifying business requirements. | [![GitHub](https://img.shields.io/badge/-Profile-181717?style=flat&logo=github)](https://github.com/YOUR_GITHUB_USERNAME) |
| <img src="https://github.com/github.png" width="45" height="45" style="border-radius:50%;"/> | **Hồ Dương Tuấn Vũ** | Frontend Engineer | Leading frontend development, main operational service flows, and state management in React. | [![GitHub](https://img.shields.io/badge/-Profile-181717?style=flat&logo=github)](https://github.com/HDTVux) |
| <img src="https://github.com/github.png" width="45" height="45" style="border-radius:50%;"/> | **Vũ Công Đạt** | Frontend Engineer | UI/UX design implementation, customer dashboard development, real-time notification views via WebSocket. | [![GitHub](https://img.shields.io/badge/-Profile-181717?style=flat&logo=github)](https://github.com/CongDat2003) |
| <img src="https://avatars.githubusercontent.com/u/211706548?v=4" width="45" height="45" style="border-radius:50%;"/> | **Lê Trọng Tấn** | Backend Engineer | RESTful API development, database integration, and feature implementations, Hikvision camera hardware integration, system security. | [![GitHub](https://img.shields.io/badge/-Profile-181717?style=flat&logo=github)](https://github.com/tanle04) |

---
<a id="getting-started"></a>
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
