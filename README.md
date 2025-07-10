:

📧 Email Sending Service
A lightweight service to send emails and track their status.
Built with Spring Boot (Java) for the backend and React.js for the frontend.

🧩 Features
Send emails via REST API

Track delivery status by requestId

Retry logic, fallback provider (mock setup)

Simple React UI for quick testing

🏗 Project Structure
css
Copy
Edit
email-service/
├── backend/ (Spring Boot)
│   └── src/main/java/com/emailservice/...
├── frontend/ (React App)
│   └── src/components/EmailForm.jsx
🚀 Backend Setup (Spring Boot)
1. Clone and Navigate
   bash
   Copy
   Edit
   git clone https://github.com/your-repo/email-service.git
   cd email-service/backend
2. Configure Environment
   Create an .env or use application.properties:

properties
Copy
Edit
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your@email.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
Also enable CORS (already done in WebConfig.java):

java
Copy
Edit
registry.addMapping("/api/**").allowedOrigins("http://localhost:3000");
3. Run the Backend
   bash
   Copy
   Edit
   ./mvnw spring-boot:run
# or run via IntelliJ
API will be available at:
📍 http://localhost:8080/api/email/send
📍 http://localhost:8080/api/email/status/{requestId}

💻 Frontend Setup (React)
1. Navigate to Frontend
   bash
   Copy
   Edit
   cd ../frontend
2. Install Dependencies
   bash
   Copy
   Edit
   npm install
3. Start the App
   bash
   Copy
   Edit
   npm start
   App runs at:
   🌐 http://localhost:3000

📮 API Usage
POST /api/email/send
Send an email.

json
Copy
Edit
{
"requestId": "req-101",
"to": "recipient@example.com",
"subject": "Hello",
"body": "This is a test email"
}
🟢 Response: Plain text like
✅ Email sent successfully via MockProvider1

GET /api/email/status/{requestId}
Check delivery status.

🟢 Response:

json
Copy
Edit
{
"requestId": "req-101",
"status": "SENT",
"providerUsed": "MockProvider1",
"attempts": 1,
"message": "Email sent successfully",
"timestamp": "2025-07-10T21:40:07.2498111"
}
