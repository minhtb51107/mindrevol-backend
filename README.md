# MindRevol - Journey Sharing Social Platform 🚀

MindRevol is a goal-centric social networking platform designed to help Gen Z users track their personal development journeys, share moments, and connect through empathy.

> **Status:** MVP (Minimum Viable Product) - Personal Learning Project.

## 🌟 Key Features

* **🔐 Advanced Authentication:**
    * OAuth2 integration (Google, Facebook, TikTok - Sandbox).
    * Secure Session Management using **Redis**.
    * JWT Access/Refresh Token mechanism.
* **🛣️ Journey & Feed:**
    * Create and manage time-bound journeys (e.g., "30 Days of Coding").
    * Post updates (Check-ins) with images and mood tracking.
    * Real-time Newsfeed updates.
* **💬 Real-time Communication:**
    * Instant messaging (Chat) using **WebSocket**.
    * Real-time Notifications.
* **📊 Analytics & Monitoring:**
    * Integrated **PostHog** for product analytics (User retention, feature usage).
    * **LGTM Stack** (Loki, Grafana, Prometheus) for centralized logging and server monitoring.
    * **UptimeRobot** for health checks.
* **⚙️ DevOps & CI/CD:**
    * Automated deployment pipeline using **GitHub Actions**.
    * Deployed on **Render** (Dockerized).

## 🛠️ Tech Stack

**Backend:**
* **Language:** Java 21
* **Framework:** Spring Boot 3 (Modular Monolith Architecture)
* **Database:** PostgreSQL
* **Caching & Session:** Redis
* **Security:** Spring Security, OAuth2
* **Real-time:** WebSocket (STOMP)

**Frontend:**
* React (Vite) + TypeScript
* Tailwind CSS
* *Frontend Repo:* [Link to your Frontend Repo here]

**Infrastructure:**
* Docker & Docker Compose
* GitHub Actions (CI/CD)
* ImageKit (Media Storage)

## 📐 Architecture

The project follows a **Modular Monolith** architecture to ensure code maintainability while keeping deployment simple.

* `modules/auth`: Authentication & Authorization logic.
* `modules/journey`: Journey management and participation logic.
* `modules/feed`: Newsfeed generation and interactions.
* `modules/chat`: Real-time messaging logic.

## 🚀 Getting Started

### Prerequisites
* Java 21 SDK
* Docker & Docker Compose
* Maven/Gradle

### Installation

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/minhtb51107/mindrevol-backend.git](https://github.com/minhtb51107/mindrevol-backend.git)
    cd mindrevol-backend
    ```

2.  **Configure Environment**
    Create a `.env` file or update `application.properties` with your credentials:
    ```properties
    DB_URL=jdbc:postgresql://localhost:5432/mindrevol
    REDIS_HOST=localhost
    GOOGLE_CLIENT_ID=your_id
    # ... other keys
    ```

3.  **Run with Docker (Recommended)**
    ```bash
    docker-compose up -d
    ```

4.  **Run with Gradle**
    ```bash
    ./gradlew bootRun
    ```

## 📸 Screenshots

| Login Screen | Journey Feed | Analytics Dashboard |
|:---:|:---:|:---:|
| ![Login](link_to_image_1.png) | ![Feed](link_to_image_2.png) | ![PostHog](link_to_image_3.png) |

## 👤 Author

**Minh (Fullstack Developer)**
* Github: [@minhtb51107](https://github.com/minhtb51107)
* Email: [Email của bạn]

---
*This project was developed as a Capstone Project to practice building comprehensive web systems.*
"# clonemindrevol" 
