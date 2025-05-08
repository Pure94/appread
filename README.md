# AppRead

AppRead is an application that allows users to clone and analyze Git repositories. It consists of a Spring Boot backend and an Electron-based Vue.js frontend.

## Project Overview

AppRead provides the following features:
- Clone Git repositories (with optional token for private repos)
- Browse repository file structure
- Process and analyze repository content
- Vector-based document search capabilities

## Prerequisites

### Backend
- Java 21 or higher
- Maven
- PostgreSQL with PgVector extension
- OpenAI API key for embeddings

### Frontend
- Node.js 14 or higher
- npm or yarn

## Installation

### Backend

1. Clone this repository:
```bash
git clone https://github.com/yourusername/appread.git
cd appread
```

2. Build the backend:
```bash
./mvnw clean package
```

### Frontend

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

## Running the Application

### Option 1: Using Docker Compose (Recommended)

The easiest way to run the complete application is using Docker Compose:

1. Make sure you have Docker and Docker Compose installed
2. Set your OpenAI API key as an environment variable:
```bash
export OPENAI_API_KEY=your_openai_api_key
```
3. Start the application:
```bash
docker-compose up
```

This will start:
- PostgreSQL database with PgVector extension
- Spring Boot backend
- (Note: You'll still need to run the Electron frontend separately)

### Option 2: Running Components Separately

#### Backend

1. Make sure PostgreSQL with PgVector extension is running
2. Set your OpenAI API key:
```bash
export OPENAI_API_KEY=your_openai_api_key
```
3. Run the Spring Boot application:
```bash
./mvnw spring-boot:run
```

#### Frontend Electron App

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. For development mode (with hot-reload):
```bash
npm run electron:serve
```

3. For production build:
```bash
npm run electron:build
```

4. The built application will be available in the `dist_electron` directory

## Development Notes

- In development mode, the Electron app expects the backend to be running separately on port 8080
- In production mode, the Electron app will start the backend JAR automatically

## Architecture

- **Backend**: Spring Boot application with PostgreSQL/PgVector for vector storage
- **Frontend**: Vue.js 3 with Electron for desktop application capabilities
- **Communication**: REST API between frontend and backend

## Environment Variables

- `OPENAI_API_KEY`: Required for embedding generation
- `SPRING_DATASOURCE_URL`: Database connection URL (default: jdbc:postgresql://localhost:5432/appread)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: postgres)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: postgres)