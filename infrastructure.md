# Local Infrastructure Setup (Kafka & MinIO S3)

This guide provides the instructions to spin up your local infrastructure components using Docker Compose. All services are unified inside a single network to support our multi-tenant RAG pipeline.

### Port Allocation Mapping
To prevent conflicts with our API Gateway (which runs on port `9000`), the infrastructure ports are configured as follows:
* **Kafka Broker Port:** `9092` (Used by local Spring Boot apps)
* **Kafbat Kafka UI Dashboard:** `8989` (Browser console via `/kafka-ui`)
* **MinIO Storage API Port:** `9005` (Used by Spring Boot AWS SDK)
* **MinIO Web Console UI:** `9006` (Browser console to inspect files)

---

## 1. Unified Configuration (`docker-compose.yml`)

Create or update your `docker-compose.yml` file in your infrastructure repository with this configuration:

```yaml
services:
  # --- Apache Kafka Broker (KRaft Mode) ---
  kafka:
    image: apache/kafka:latest
    container_name: kafka-broker
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:29093'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_LISTENERS: 'INTERNAL://0.0.0.0:29092,EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:29093'
      KAFKA_ADVERTISED_LISTENERS: 'INTERNAL://kafka:29092,EXTERNAL://localhost:9092'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'INTERNAL'
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    volumes:
      - ./kafka-data:/var/lib/kafka/data:Z

  # --- Kafbat UI Management Dashboard ---
  kafka-ui:
    image: kafbat/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8989:8080"
    environment:
      - KAFKA_CLUSTERS_0_NAME=local-cluster
      - KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:29092
      - SERVER_SERVLET_CONTEXT_PATH=/kafka-ui
    depends_on:
      - kafka

  # --- MinIO Local S3 Storage Simulator ---
  minio:
    image: minio/minio:latest
    container_name: local-s3-server
    ports:
      - "9005:9000"
      - "9006:9001"
    environment:
      MINIO_ROOT_USER: local-access-key
      MINIO_ROOT_PASSWORD: local-secret-key
    volumes:
      - ./minio-data:/data:Z
    command: server /data --console-address ":9001"
```

---

## 2. Infrastructure Lifecycle Management

### Start All Services
Run this command from the directory containing your `docker-compose.yml` file to download and launch Kafka, Kafka UI, and MinIO in the background:
```bash
docker compose up -d
```

### Stop and Clean Up Services
To gracefully stop all running services and clean up their container runtime tracking environments, run:
```bash
docker compose down
```
*(Note: Because of persistent volume mappings, your Kafka logs and uploaded MinIO storage documents are saved locally inside your directory and will **not** be lost when downing containers).*

---

## 3. Initial Storage Bucket Configuration

1. Open your web browser and navigate to the MinIO Console: **`http://localhost:9006`**
2. Authenticate using your credentials:
    * **Access Key:** `local-access-key`
    * **Secret Key:** `local-secret-key`
3. Click on **Buckets** in the sidebar menu, then click **Create Bucket**.
4. Name the new bucket exactly: **`document-ingestion`**
5. Click **Save**.

---

## 4. Spring Application Profiles (`application.yml`)

Apply these infrastructure endpoint parameters to connect your local Java microservices:

### Local S3 Properties (Both Services)
```yaml
spring:
  cloud:
    aws:
      credentials:
        access-key: local-access-key
        secret-key: local-secret-key
      region:
        static: us-east-1
      s3:
        endpoint: http://localhost:9005
        path-style-access-enabled: true
```

### Local Kafka Broker Properties (Both Services)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

---

## 5. Multi-Tenant Storage Partition Flow

Documents uploaded through the API gateway map to partitioned directory keys to isolate tenant boundaries:

```text
document-ingestion/ (S3 Bucket Root)
  └── {tenant_id}/
       └── {document_id}/
            └── raw-uploaded-file.pdf
```
