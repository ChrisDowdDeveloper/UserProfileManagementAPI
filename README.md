## Setup Instructions

This section provides instructions to set up and run the User Profile Management application.

### Prerequisites

* **Java JDK**: Version 17 or higher.
* **Maven**: Apache Maven build tool.
* **Google Cloud SDK (gcloud CLI)**: Optional but recommended for managing GCP resources if not using emulators exclusively.
* **GCP Project**: A Google Cloud Platform project is needed if you intend to connect to live GCP services (Datastore, Pub/Sub, KMS).
    * Project ID used in configuration: `userprofilemanagement-460201`
* **Pub/Sub Emulator**: For local development without connecting to live GCP Pub/Sub, an emulator can be used. The application is configured to connect to one at `localhost:8085`.

### Backend Setup (UserProfileManagement API)

1.  **Clone the Repository**:
    ```bash
    git clone <your-repository-url>
    cd UserProfileManagement
    ```

2.  **Configure GCP Credentials**:
    The application expects GCP credentials for Datastore, Pub/Sub, and KMS.
    * Obtain your GCP Service Account key JSON file.
    * Store this file securely, for example, at `~/.gcp/userprofilemanagement-key.json` (or `$HOME/.gcp/userprofilemanagement-key.json`).
    * **Important**: The GCP service account key file is highly sensitive. Ensure it is stored securely, access is restricted, and it is **never committed to version control**.
    * The `application.yml` specifies where the application will look for this file:
        ```yaml
        spring:
          cloud:
            gcp:
              project-id: userprofilemanagement-460201 # Your GCP Project ID
              credentials:
                location: file:${HOME}/.gcp/userprofilemanagement-key.json # Path to your service account key
        ```

3.  **SSL Keystore**:
    The application is configured to run over HTTPS on port `8443` using a PKCS12 keystore.
    * You will need to generate or provide a `keystore.p12` file and place it in the `src/main/resources/` directory.
    * Configure the keystore password in `src/main/resources/application.properties`. Replace `<YOUR_KEYSTORE_PASSWORD>` with your actual password. It is strongly recommended to use environment variables or a secrets manager for this in production.
        ```properties
        server.port=8443
        server.ssl.enabled=true
        server.ssl.key-store-type=PKCS12
        server.ssl.key-store=classpath:keystore.p12
        server.ssl.key-store-password=<YOUR_KEYSTORE_PASSWORD> # Replace with your keystore password
        server.ssl.key-alias=selfsigned_localhost # Or your key alias
        ```
    * To generate a self-signed certificate for local testing (replace `<YOUR_KEYSTORE_PASSWORD>` with your chosen password):
        ```bash
        keytool -genkeypair -alias selfsigned_localhost -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -validity 365 -storepass <YOUR_KEYSTORE_PASSWORD>
        ```
        (Adjust subject details as prompted by keytool).

4.  **Review Application Configuration**:
    Key configuration details are present in `src/main/resources/application.yml` and `src/main/resources/application.properties`.
    * **Application Name**: `UserProfileManagement`
    * **Server Port**: `8443` (HTTPS)
    * **Pub/Sub Topic**: `users-create-topic` (from `application.yml` custom properties)
    * **Pub/Sub Subscription**: `users-create-subscription` (from `application.yml` custom properties)
    * **Pub/Sub Emulator**: Configured to use `localhost:8085` if active.
        ```yaml
        spring:
          cloud:
            gcp:
              pubsub:
                emulator-host: localhost:8085
        ```
    * **Caching (Caffeine)**:
        ```yaml
        spring:
          cache:
            type: caffeine
            caffeine:
              spec: maximumSize=500, expireAfterWrite=10m
        ```
    * **JCE Encryption Key**:
        The application requires a JCE encryption key for data protection. This key should be a strong, randomly generated key.
        In `src/main/resources/application.yml`, the key is expected at:
        ```yaml
        encryption:
          jce:
            key: <YOUR_JCE_ENCRYPTION_KEY> # Replace with your actual key or use environment variable
        ```
        **Important**: Do not hardcode the actual encryption key directly in the `application.yml` file in a production or shared environment.
        * **Recommended Approach**: Supply this key via an environment variable (e.g., `APP_ENCRYPTION_KEY`) and reference it in your `application.yml`:
            ```yaml
            encryption:
              jce:
                key: ${APP_ENCRYPTION_KEY}
            ```
        * Alternatively, consider using Google Cloud KMS for managing this key, especially since `spring-cloud-gcp-starter-kms` is included as a dependency. If using KMS, this property might hold the KMS key resource ID.

5.  **Build the Application**:
    ```bash
    mvn clean install
    ```

6.  **Run the Application**:
    Before running, ensure any sensitive values like the JCE key or keystore password are provided (e.g., via environment variables if you've configured them that way).
    ```bash
    # Example if providing key via environment variable
    # export APP_ENCRYPTION_KEY="your_base64_encoded_jce_key_here"
    # export KEYSTORE_PASSWORD="your_keystore_password_here"
    # (And update application.properties/yml to use these env vars, e.g. server.ssl.key-store-password=${KEYSTORE_PASSWORD})

    mvn spring-boot:run
    ```
    Or run the packaged jar:
    ```bash
    java -jar target/UserProfileManagement-0.0.1-SNAPSHOT.jar
    ```
    The API will be available at `https://localhost:8443`.