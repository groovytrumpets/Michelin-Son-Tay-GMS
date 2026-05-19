Michelin Son Tay Garage Management System (MST-GMS)
📌 Introduction
The purpose of this system is to provide a comprehensive and integrated software platform for an authorized Michelin dealer in Son Tay. It supports the operational management of the garage in an efficient, secure, and transparent manner, handling end-to-end workflows including booking, service ticket processing, and warehouse management.

🛠️ Technologies
Backend: Java 17, Spring Boot (Clean Architecture, Domain-Driven Design)

Frontend: React.js

Database: MySQL (Scalable schema with 70+ tables)

Deployment: Docker, Linux Server

Storage: Cloudinary

Real-time & Notifications: WebSocket/STOMP, Zalo OA API

Authentication: Google OAuth2

Hardware Integration: Hikvision ISAPI

Live System: sontaygarage.vn

🚀 Installation & Run
1. Requirements
JDK: Java JDK 17

Environment: Node.js (for React frontend)

Database: MySQL Server

DevOps: Docker & Docker Compose

IDE: IntelliJ IDEA / Eclipse (Backend) & Visual Studio Code (Frontend)

👥 Actors & Roles
Admin/Manager: Manage employee accounts, oversee warehouse inventory, configure promotions and tax logic, view reports, and manage garage operations.

Service Advisor/Staff: Create and manage service tickets, assign tasks to mechanics, check stock availability, and process payments.

Mechanic: View assigned service tickets, update task statuses, and request parts from the warehouse.

Customer: Book service appointments online, authenticate via Google OAuth2, and receive real-time updates and notifications.

System: Calculate dynamic pricing and taxes (e.g., re-applying tax upon promotion removal), trigger Zalo OA notifications via WebSocket, manage media uploads to Cloudinary, and interface with Hikvision cameras.

📑 Documentation
The following resources and links are available:

Live Website: https://sontaygarage.vn/

GitHub Repository: MST-GMS Repository

Documentation Folder: Check the /docs folder for Database Schema (SQL) and architectural sequence diagrams (numbered sequentially with whole integers).

🛠️ Setup Guide
(A) Install Prerequisites
Download and install Java JDK 17. Set the JAVA_HOME environment variable.

Download and install Node.js for frontend package management.

Download and install MySQL Server and a client like MySQL Workbench.

(B) Database Setup
Open MySQL Workbench or your preferred database tool.

Execute the provided initialization script in docs/schema.sql to create the 70+ tables.

Verify the database has been created and populated with initial setup data.

(C) Backend Setup (Spring Boot)
Open the backend project folder in IntelliJ IDEA.

Navigate to src/main/resources/application.properties (or .yml) and configure the environment variables:

MySQL Database credentials.

Cloudinary API keys.

Google OAuth2 Client ID and Secret.

Zalo OA API credentials.

Run the application using Maven or your IDE's run configuration. The server will typically start on port 8080.

(D) Frontend Setup (React.js)
Open the frontend folder in Visual Studio Code.

Open the terminal and run npm install to fetch all dependencies.

Configure your .env file with the correct backend API endpoint URL.

Run npm start to launch the development server.

(E) Docker Deployment (Production)
Ensure Docker and Docker Compose are installed on your Linux machine.

Navigate to the root directory containing the docker-compose.yml file.

Run docker-compose up -d --build to build the images and start the containers in the background.
