# EcoTrip Java

EcoTrip is a Java 17 desktop application for managing an eco-tourism platform. It provides front-office and back-office workflows for users, accommodations, activities, transport, reservations, products, carts, payments, and account management through a JavaFX interface.

The application is intended for travel and hospitality teams that need a rich desktop client backed by a MySQL database. It follows a layered Java architecture with JavaFX controllers, domain models, repositories, services, utility classes, and reusable FXML/CSS resources.

## Topics

- Java desktop application
- JavaFX and FXML
- Maven project
- Object-oriented programming
- MVC-style architecture
- MySQL persistence
- Repository and service layers
- Authentication and authorization
- Eco-tourism, travel, booking, accommodation, activities, transport
- Payment integration
- Image upload and media management
- AI-assisted recommendations and content generation
- Email, SMS, maps, QR codes, PDF generation

## Keywords

`java`, `javafx`, `fxml`, `maven`, `mysql`, `jdbc`, `oop`, `mvc`, `desktop-app`, `tourism`, `eco-tourism`, `booking`, `reservation`, `accommodation`, `transport`, `ecommerce`, `payments`, `mollie`, `cloudinary`, `gemini-api`, `huggingface`, `twilio`, `google-oauth`, `openstreetmap`, `leaflet`, `pdf`, `qr-code`

## Features

- User registration, login, password reset, profile management, and role-based navigation.
- Google OAuth login support.
- Front-office screens for activities, accommodations, transport, products, cart, favorites, and reservations.
- Back-office dashboards for managing users, activities, guides, schedules, accommodations, rooms, equipment, products, orders, payments, reservations, transport categories, transport, and drivers.
- MySQL-backed persistence using JDBC repositories.
- Activity maps with Leaflet/OpenStreetMap and current weather lookup.
- AI-generated descriptions and transport recommendations using Gemini.
- Review moderation and face-recognition support using Hugging Face integrations.
- Cloudinary image upload for accommodation and review media.
- Email password-reset workflow and Twilio SMS notifications.
- Mollie payment checkout support.
- PDF and QR code generation for reservation-related workflows.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| UI | JavaFX 17, FXML, CSS |
| Build | Maven |
| Database | MySQL, JDBC |
| Security | jBCrypt |
| JSON / HTTP | Jackson, org.json, OkHttp, Java HTTP Client |
| Media / Documents | Cloudinary, OpenPDF, iText, ZXing |
| Maps | Leaflet, OpenStreetMap |
| AI / APIs | Gemini, Hugging Face, Open-Meteo, ExchangeRate API |
| Notifications | JavaMail, Twilio |
| Payments | Mollie |
| Testing | JUnit 5 |

## Project Structure

```text
src/
  main/
    java/tn/esprit/
      config/        Integration configuration classes
      controller/    JavaFX controllers for auth, front office, back office, and layouts
      database/      MySQL connection singleton
      models/        Domain models
      navigation/    Route and scene management
      repository/    JDBC data access layer
      services/      Business logic and external API integrations
      session/       Current user/session state
      utils/         Shared helpers and integration utilities
      Main.java      JavaFX entry point
    resources/
      styles/        Application CSS
      views/         FXML screens and reusable components
      leaflet/       Local Leaflet assets
      images/        Static images
      config.properties
  test/
    java/            JUnit tests
```

## Prerequisites

- JDK 17 or newer.
- Maven 3.8+.
- MySQL Server 8.x or compatible.
- A database named `ecotrip`.
- Internet access for external integrations such as Gemini, Hugging Face, Cloudinary, Twilio, Mollie, Open-Meteo, exchange rates, and map tiles.
- Optional: IntelliJ IDEA or another Java IDE with JavaFX support.

Check your local versions:

```bash
java -version
mvn -version
mysql --version
```

## Database Setup

The current database connection is defined in:

```text
src/main/java/tn/esprit/database/Base.java
```

Default connection values:

```text
URL:  jdbc:mysql://localhost:3306/ecotrip?useSSL=false&allowPublicKeyRetrieval=true
User: root
Pass: empty password
```

Create the database before running the app:

```sql
CREATE DATABASE ecotrip CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

If your MySQL username, password, host, port, or database name is different, update `Base.java` accordingly before running the project.

> Note: This repository does not include a visible SQL migration or schema dump. Import your project database schema/data before launching the application.

## Configuration

Create or update:

```text
src/main/resources/config.properties
```

Example configuration:

```properties
# Cloudinary
cloudinary.cloud.name=your_cloud_name
cloudinary.api.key=your_cloudinary_api_key
cloudinary.api.secret=your_cloudinary_api_secret

# Gemini
gemini.api.key=your_gemini_api_key
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent

# Hugging Face moderation, optional
moderation.api.url=https://api-inference.huggingface.co/models/facebook/bart-large-mnli
moderation.api.key=your_huggingface_token
moderation.api.timeout=10

# Twilio SMS
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_FROM=+10000000000
TWILIO_TO=+10000000000
```

Additional configuration is currently stored in Java configuration classes:

- `src/main/java/tn/esprit/config/EmailConfig.java`
- `src/main/java/tn/esprit/config/GoogleAuthConfig.java`
- `src/main/java/tn/esprit/config/HuggingFaceConfig.java`
- `src/main/java/tn/esprit/utils/MollieConfig.java`

Before publishing or deploying the project, replace local credentials with your own values and avoid committing real secrets. For a production-ready setup, move secrets to environment variables or an ignored local properties file.

Optional environment variables:

```bash
# Used by Mollie amount conversion when store currency is TND and checkout currency is EUR
MOLLIE_TND_TO_EUR_RATE=0.2930

# Used to store uploaded files in a Symfony public directory instead of local-public/
ECOTRIP_SYMFONY_PUBLIC_DIR=/absolute/path/to/symfony/public

# Alternative Gemini key used by some activity AI helpers
GEMINI_API_KEY=your_gemini_api_key
```

You can also pass the upload directory and Gemini key as JVM system properties:

```bash
mvn javafx:run -Decotrip.symfony.public_dir=/absolute/path/to/public -Dgemini.api.key=your_gemini_api_key
```

## Installation

Clone the repository:

```bash
git clone <repository-url>
cd projet-PIDEV-3A37-2526-Ecotrip
```

Install Maven dependencies:

```bash
mvn clean install
```

If you only want to compile the application:

```bash
mvn clean compile
```

## Build

Build the Maven project:

```bash
mvn clean package
```

Run tests:

```bash
mvn test
```

This project is configured with Maven through `pom.xml`. A Gradle build file is not currently present. If Gradle support is added later, use the generated Gradle wrapper commands such as:

```bash
./gradlew build
./gradlew test
```

On Windows PowerShell, the equivalent wrapper commands would be:

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

## Run

Run the JavaFX application with Maven:

```bash
mvn javafx:run
```

The configured JavaFX entry point is:

```text
tn.esprit.Main
```

The application starts on the login screen and opens a desktop window titled `EcoTrip`.

## Running From an IDE

1. Open the project as a Maven project.
2. Use JDK 17 as the project SDK.
3. Reload Maven dependencies.
4. Ensure MySQL is running and the `ecotrip` database exists.
5. Fill `src/main/resources/config.properties` and any required Java config classes.
6. Run `tn.esprit.Main`.

## External Services

Some features require third-party accounts or network access:

| Service | Purpose |
| --- | --- |
| MySQL | Main application data |
| Cloudinary | Image uploads |
| Gemini API | AI descriptions and transport recommendations |
| Hugging Face | Face recognition and review moderation |
| Google OAuth | Social login |
| Gmail SMTP / JavaMail | Password reset email |
| Twilio | SMS notifications |
| Mollie | Payment checkout |
| Open-Meteo | Activity weather data |
| ExchangeRate API | TND conversion display |
| OpenStreetMap / Leaflet | Maps |

Features that depend on missing credentials may fail, be skipped, or fall back depending on the service implementation.

## Development Notes

- Keep generated files out of source control. The repository already ignores `target/`, `local-public/`, and `src/main/resources/config.properties`.
- Do not commit API keys, OAuth secrets, SMTP passwords, or payment credentials.
- The source tree currently contains some compiled `.class` files under `src/main/java`. These should normally be removed from source control and regenerated by Maven.
- Prefer configuration through environment variables or ignored local properties for sensitive values.

## License

No license file is currently included. Add a `LICENSE` file before distributing or open-sourcing the project.
