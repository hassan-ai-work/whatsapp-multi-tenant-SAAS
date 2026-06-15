@@ -1,6 +1,5 @@                                                                                                                                                                                                     
-@@ -1,6 +1,7 @@
# Manual Ingestion Test Guide

--This guide explains how to manually verify the full document ingestion flow across:                                                                                                                               
-+This guide explains how to manually test the document ingestion flow that was implemented across:                                                                                                                 
+This guide explains how to manually test the document ingestion flow implemented across:

- `api-gateway`
- `whatsapp-brain-service`
- `kafka-service`

--It covers:                                                                                                                                                                                                        
-+It includes:                                                                                                                                                                                                      
+It includes:

+1. what was implemented                                                                                                                                                                                            
+2. prerequisites                                                                                                                                                                                                   
+3. startup order                                                                                                                                                                                                   
+4. how to get an access token                                                                                                                                                                                      
+5. how to call the ingest endpoint                                                                                                                                                                                 
--6. Kafka worker verification                                                                                                                                                                                      
--7. failure scenarios                                                                                                                                                                                              
--8. idempotency/redelivery checks                                                                                                                                                                                  
-+1. what was implemented                                                                                                                                                                                           
-+2. required services                                                                                                                                                                                              
-+3. startup order                                                                                                                                                                                                  
-+4. how to get an access token                                                                                                                                                                                     
-+5. how to call the ingest endpoint                                                                                                                                                                                
-+6. how to verify database rows                                                                                                                                                                                    
-+7. how to verify MinIO upload                                                                                                                                                                                     
-+8. how to verify Kafka processing                                                                                                                                                                                 
-+9. how to test failure scenarios                                                                                                                                                                                  
-+10. how to test idempotent redelivery
-
----                                                                                                                                                                                                                
+1. what was implemented                                                                                                                                                                                            
+2. prerequisites                                                                                                                                                                                                   
+3. startup order                                                                                                                                                                                                   
+4. how to get an access token                                                                                                                                                                                      
+5. how to call the ingest endpoint                                                                                                                                                                                 
+6. how to verify database rows                                                                                                                                                                                     
+7. how to verify MinIO upload                                                                                                                                                                                      
+8. how to verify Kafka processing                                                                                                                                                                                  
+9. how to test failure scenarios                                                                                                                                                                                   
+10. how to test idempotent redelivery

--## 1. What the flow should do                                                                                                                                                                                     
-+## 1. What was implemented                                                                                                                                                                                        
+---

--Expected architecture behavior:                                                                                                                                                                                   
-+### API Gateway                                                                                                                                                                                                   
-+The gateway now:                                                                                                                                                                                                  
+## 1. What was implemented

-+- validates bearer JWT with Keycloak                                                                                                                                                                              
-+- reads `preferred_username` from the authenticated JWT                                                                                                                                                           
-+- injects `X-Authenticated-User`                                                                                                                                                                                  
--   - accepts multipart upload                                                                                                                                                                                     
--   - resolves tenant                                                                                                                                                                                              
--   - inserts a `documents` row with `PENDING`                                                                                                                                                                     
--   - uploads the file to MinIO                                                                                                                                                                                    
--   - publishes `DocumentIngestionEvent` to Kafka                                                                                                                                                                  
--   - returns `202 Accepted`
-      -4. Kafka worker:                                                                                                                                                                                            
--   - consumes the event                                                                                                                                                                                           
--   - downloads the file from MinIO                                                                                                                                                                                
--   - extracts text with Apache Tika                                                                                                                                                                               
--   - chunks text using `500` chars with `50` overlap                                                                                                                                                              
--   - generates embeddings                                                                                                                                                                                         
--   - stores rows in `public.document_chunks`                                                                                                                                                                      
--   - sets document status to `PROCESSED`
-      -5. On failure after document creation:                                                                                                                                                                      
--   - document status becomes `FAILED`
-      -6. On Kafka redelivery:                                                                                                                                                                                     
--   - duplicate logical chunks are not inserted                                                                                                                                                                    
--   - document remains `PROCESSED`
-      +- validates bearer JWT with Keycloak                                                                                                                                                                        
-      +- reads `preferred_username` from the authenticated JWT                                                                                                                                                     
-      +- injects `X-Authenticated-User`                                                                                                                                                                            
-      +- protects `/v1/ingest/docs`                                                                                                                                                                                
-      +- forwards `/v1/ingest/docs` to `http://localhost:8080`                                                                                                                                                     
-      +- applies:                                                                                                                                                                                                  
-+  - `TokenRelay`                                                                                                                                                                                                  
-+  - `InjectUsername`                                                                                                                                                                                              
+### API Gateway
                                                                                                                                                                                                                    
-----                                                                                                                                                                                                               
-+### Brain Service                                                                                                                                                                                                 
-+The brain service now:                                                                                                                                                                                            
+The gateway now:

+- validates bearer JWT with Keycloak                                                                                                                                                                               
+- reads `preferred_username` from the authenticated JWT                                                                                                                                                            
+- injects `X-Authenticated-User`                                                                                                                                                                                   
-+  - `title` optional                                                                                                                                                                                              
-+  - `source` optional                                                                                                                                                                                             
-+  - `metadata` optional JSON string
-     +- reads `X-Authenticated-User`                                                                                                                                                                               
-     +- reads `X-Authenticated-User`                                                                                                                                                                               
-     +- resolves tenant using current placeholder strategy:                                                                                                                                                        
-+  - `tenantRepository.findByUsername(authenticatedUsername.toLowerCase())`
-     +- inserts row into `public.documents` with `PENDING`                                                                                                                                                         
-     +- uploads file to MinIO                                                                                                                                                                                      
-     +- builds object key as:                                                                                                                                                                                      
+- validates bearer JWT with Keycloak                                                                                                                                                                               
+- reads `preferred_username` from the authenticated JWT                                                                                                                                                            
+- injects `X-Authenticated-User`                                                                                                                                                                                   
+- protects `/v1/ingest/docs`                                                                                                                                                                                       
+- forwards `/v1/ingest/docs` to `http://localhost:8080`                                                                                                                                                            
+- applies:
+  - `TokenRelay`
+  - `InjectUsername`

--Make sure these are available and running:
--                                                                                                                                                                                                                  
--- PostgreSQL                                                                                                                                                                                                      
--- Kafka                                                                                                                                                                                                           
--- MinIO                                                                                                                                                                                                           
--- Keycloak                                                                                                                                                                                                        
--- `whatsapp-brain-service`                                                                                                                                                                                        
--- `kafka-service`                                                                                                                                                                                                 
--- `api-gateway`                                                                                                                                                                                                   
--                                                                                                                                                                                                                  
--Also make sure:                                                                                                                                                                                                   
--                                                                                                                                                                                                                  
--- the database schema has already been created via `V1__init.sql`                                                                                                                                                 
--- the MinIO bucket exists:                                                                                                                                                                                        
--  - `document-ingestion`
-     -- Keycloak is issuing JWTs for your configured realm                                                                                                                                                         
-     -- your JWT includes:                                                                                                                                                                                         
--  - `preferred_username`
--                                                                                                                                                                                                                  
-----
--                                                                                                                                                                                                                  
--## 3. Startup order
--                                                                                                                                                                                                                  
--Recommended order:
--                                                                                                                                                                                                                  
--1. PostgreSQL                                                                                                                                                                                                     
--2. Kafka                                                                                                                                                                                                          
--3. MinIO                                                                                                                                                                                                          
--4. Keycloak                                                                                                                                                                                                       
--5. `whatsapp-brain-service`                                                                                                                                                                                       
--6. `kafka-service`                                                                                                                                                                                                
--7. `api-gateway`                                                                                                                                                                                                  
--
-----
--                                                                                                                                                                                                                  
--## 4. Verify tenant exists
--                                                                                                                                                                                                                  
--Current tenant resolution in `whatsapp-brain-service` uses:
--                                                                                                                                                                                                                  
--- `tenantRepository.findByUsername(authenticatedUsername.toLowerCase())`
--                                                                                                                                                                                                                  
--So the JWT claim `preferred_username` must match a row in `public.tenants.username`.
--                                                                                                                                                                                                                  
--Use SQL:
--                                                                                                                                                                                                                  
+### Brain Service

+The brain service now:

+- exposes `POST /v1/ingest/docs`                                                                                                                                                                                   
+- accepts multipart request:
+  - `file` required
+  - `title` optional
+  - `source` optional
+  - `metadata` optional JSON string                                                                                                                                                                                
     +- reads `X-Authenticated-User`                                                                                                                                                                                     
     +- reads `X-Authenticated-User`                                                                                                                                                                                     
     +- resolves tenant using the current placeholder strategy:
+  - `tenantRepository.findByUsername(authenticatedUsername.toLowerCase())`                                                                                                                                         
     +- inserts a row into `public.documents` with status `PENDING`                                                                                                                                                      
     +- uploads the file to MinIO                                                                                                                                                                                        
     +- builds object key as:

-
-@@ -1 +1,5 @@
-                                                                                                                                                                                                                   
-+- builds metadata with:                                                                                                                                                                                           
-+  - `uploader`                                                                                                                                                                                                    
-+  - `originalFilename`                                                                                                                                                                                            
-+  - `contentType`                                                                                                                                                                                                 
-+  - `sizeBytes`                                                                                                                                                                                                   
-+  - `ingestRequestId`                                                                                                                                                                                             
-+  - `tags`                                                                                                                                                                                                        
-+  - `client`                                                                                                                                                                                                      
-+  - `client`                                                                                                                                                                                                      
-+  - `custom`
-     +- publishes `DocumentIngestionEvent` to Kafka topic:                                                                                                                                                         
-+  - `document-ingestion-topic`
-     +- returns `202 Accepted`                                                                                                                                                                                     
-     +- sets `documents.status = FAILED` if MinIO or Kafka fails after row insert                                                                                                                                  
-
-+### Kafka Service                                                                                                                                                                                                 
-+The Kafka worker now:                                                                                                                                                                                             
-+                                                                                                                                                                                                                  
-+- consumes `document-ingestion-topic`                                                                                                                                                                             
-+- downloads file from MinIO                                                                                                                                                                                       
-+- extracts text using Apache Tika                                                                                                                                                                                 
-+- chunks text using:                                                                                                                                                                                              
-+  - size `500`                                                                                                                                                                                                    
-+  - overlap `50`
-     +- builds chunk metadata:                                                                                                                                                                                     
-+  - `chunkIndex`                                                                                                                                                                                                  
-+  - `chunkStart`                                                                                                                                                                                                  
-+  - `chunkEnd`                                                                                                                                                                                                    
-+  - `chunkHash`                                                                                                                                                                                                   
-+  - `s3Key`                                                                                                                                                                                                       
-+  - `title`                                                                                                                                                                                                       
-+  - `source`                                                                                                                                                                                                      
-+  - `ingestRequestId`
-     +- generates embeddings                                                                                                                                                                                       
-     +- inserts rows into `public.document_chunks`                                                                                                                                                                 
-     +- sets `documents.status = PROCESSED` on success                                                                                                                                                             
-     +- sets `documents.status = FAILED` on failure                                                                                                                                                                
-     +- handles duplicate Kafka redelivery idempotently using the existing DB unique constraint                                                                                                                    
-+                                                                                                                                                                                                                  
-+---                                                                                                                                                                                                               
-+                                                                                                                                                                                                                  
-+## 2. Required services                                                                                                                                                                                           
-+                                                                                                                                                                                                                  
-+Before testing, make sure these are running:                                                                                                                                                                      
-+                                                                                                                                                                                                                  
-+- PostgreSQL                                                                                                                                                                                                      
-+- Kafka                                                                                                                                                                                                           
-+- MinIO                                                                                                                                                                                                           
-+- Keycloak                                                                                                                                                                                                        
-+- `whatsapp-brain-service`                                                                                                                                                                                        
-+- `kafka-service`                                                                                                                                                                                                 
-+- `api-gateway`                                                                                                                                                                                                   
-+                                                                                                                                                                                                                  
-+Also make sure:                                                                                                                                                                                                   
-+                                                                                                                                                                                                                  
-+- DB schema already exists from `V1__init.sql`                                                                                                                                                                    
-+- MinIO bucket exists:                                                                                                                                                                                            
-+  - `document-ingestion`
-     +- JWT contains:                                                                                                                                                                                              
-+  - `preferred_username`
-     +- a tenant exists in DB with username matching `preferred_username`                                                                                                                                          
-+                                                                                                                                                                                                                  
-+---                                                                                                                                                                                                               
-+                                                                                                                                                                                                                  
-+## 3. Startup order                                                                                                                                                                                               
-+                                                                                                                                                                                                                  
-+Recommended order:                                                                                                                                                                                                
-+                                                                                                                                                                                                                  
-+1. PostgreSQL                                                                                                                                                                                                     
-+2. Kafka                                                                                                                                                                                                          
-+3. MinIO                                                                                                                                                                                                          
-+4. Keycloak                                                                                                                                                                                                       
-+5. `whatsapp-brain-service`                                                                                                                                                                                       
-+6. `kafka-service`                                                                                                                                                                                                
-+7. `api-gateway`                                                                                                                                                                                                  
-+                                                                                                                                                                                                                  
-+---                                                                                                                                                                                                               
-+                                                                                                                                                                                                                  
-+## 4. Verify tenant exists                                                                                                                                                                                        
-+                                                                                                                                                                                                                  
-+Current tenant resolution depends on username.                                                                                                                                                                    
-+                                                                                                                                                                                                                  
-+Run this SQL:                                                                                                                                                                                                     
-+
-
-
-select id, username, status
-                                                                                                                                                                                                                   
-from public.tenants
-                                                                                                                                                                                                                   
-order by id;
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- a tenant row exists
-                                                                                                                                                                                                                   
-- one tenant username matches the JWT `preferred_username`
-                                                                                                                                                                                                                   
-
-
-Example:
-                                                                                                                                                                                                                   
-- if token has `preferred_username = hassan`
-                                                                                                                                                                                                                   
-- DB must contain tenant with `username = hassan`
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 5. Get a Keycloak access token
-                                                                                                                                                                                                                   
-
-
-Example request:
-                                                                                                                                                                                                                   
-
-curl -X POST "http://localhost:8180/realms/replato-gateway/protocol/openid-connect/token" \
-                                                                                                                                                                                                                   
--H "Content-Type: application/x-www-form-urlencoded" \
-                                                                                                                                                                                                                   
--d "client_id=replato-gateway" \
-                                                                                                                                                                                                                   
--d "client_secret=Fdu40gsWOf0HiSqSftcL5KkmIsVwdDW7" \
-                                                                                                                                                                                                                   
--d "username=YOUR_USERNAME" \
-                                                                                                                                                                                                                   
--d "password=YOUR_PASSWORD" \
-                                                                                                                                                                                                                   
--d "grant_type=password"
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- JSON response with `access_token`
-                                                                                                                                                                                                                   
-
-
-Export it for reuse:
-                                                                                                                                                                                                                   
-
-export ACCESS_TOKEN="PASTE_ACCESS_TOKEN_HERE"
-                                                                                                                                                                                                                   
-
-
-
-Optional: inspect token claims and confirm `preferred_username` exists:
-                                                                                                                                                                                                                   
-
-python - <<'PY'
-                                                                                                                                                                                                                   
-import os, json, base64
-                                                                                                                                                                                                                   
-token = os.environ["ACCESS_TOKEN"].split(".")[1]
-                                                                                                                                                                                                                   
-token += "=" * (-len(token) % 4)
-                                                                                                                                                                                                                   
-print(json.dumps(json.loads(base64.urlsafe_b64decode(token)), indent=2))
-                                                                                                                                                                                                                   
-PY
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 6. Send a successful ingest request
-                                                                                                                                                                                                                   
-
-
-Use this curl command:
-                                                                                                                                                                                                                   
-
-curl -X POST "http://localhost:9000/v1/ingest/docs" \
-                                                                                                                                                                                                                   
--H "Authorization: Bearer $ACCESS_TOKEN" \
-                                                                                                                                                                                                                   
--F "file=@/path/to/sample.txt" \
-                                                                                                                                                                                                                   
--F "title=Sample Doc" \
-                                                                                                                                                                                                                   
--F "source=manual-upload" \
-                                                                                                                                                                                                                   
--F 'metadata={"tags":["invoice","q1"],"client":{"id":"c-123","name":"ACME"},"department":"finance"}'
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- HTTP `202 Accepted`
-                                                                                                                                                                                                                   
-
-
-Expected response example:
-                                                                                                                                                                                                                   
-
-{
-                                                                                                                                                                                                                   
-"documentId": 1,
-                                                                                                                                                                                                                   
-"tenantId": 1,
-                                                                                                                                                                                                                   
-"status": "PENDING",
-                                                                                                                                                                                                                   
-"s3Key": "document-ingestion/1/1/sample.txt",
-                                                                                                                                                                                                                   
-"ingestRequestId": "11111111-1111-1111-1111-111111111111"
-                                                                                                                                                                                                                   
-}
-                                                                                                                                                                                                                   
-
-
-
-Save:
-                                                                                                                                                                                                                   
-- `documentId`
-                                                                                                                                                                                                                   
-- `tenantId`
-                                                                                                                                                                                                                   
-- `s3Key`
-                                                                                                                                                                                                                   
-- `ingestRequestId`
-                                                                                                                                                                                                                   
-
-
-You will use them below.
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 7. Verify document row inserted
-                                                                                                                                                                                                                   
-
-
-Run:
-                                                                                                                                                                                                                   
-
-select id, tenant_id, title, source, status, metadata, created_at, updated_at
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc;
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- newest row exists
-                                                                                                                                                                                                                   
-- `tenant_id` matches expected tenant
-                                                                                                                                                                                                                   
-- `title` is request title or filename fallback
-                                                                                                                                                                                                                   
-- `source` is request source or `UPLOAD`
-                                                                                                                                                                                                                   
-- initial status is `PENDING`
-                                                                                                                                                                                                                   
-
-
-To inspect only latest row:
-                                                                                                                                                                                                                   
-
-select id, tenant_id, title, source, status
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc
-                                                                                                                                                                                                                   
-limit 1;
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 8. Verify stored metadata
-                                                                                                                                                                                                                   
-
-
-Run:
-                                                                                                                                                                                                                   
-
-select
-                                                                                                                                                                                                                   
-id,
-                                                                                                                                                                                                                   
-metadata->>'uploader' as uploader,
-                                                                                                                                                                                                                   
-metadata->>'originalFilename' as original_filename,
-                                                                                                                                                                                                                   
-metadata->>'contentType' as content_type,
-                                                                                                                                                                                                                   
-metadata->>'sizeBytes' as size_bytes,
-                                                                                                                                                                                                                   
-metadata->>'ingestRequestId' as ingest_request_id,
-                                                                                                                                                                                                                   
-metadata->'tags' as tags,
-                                                                                                                                                                                                                   
-metadata->'client' as client,
-                                                                                                                                                                                                                   
-metadata->'custom' as custom
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc
-                                                                                                                                                                                                                   
-limit 1;
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- `uploader` = JWT `preferred_username`
-                                                                                                                                                                                                                   
-- `originalFilename` = uploaded filename
-                                                                                                                                                                                                                   
-- `contentType` = MIME type or `application/octet-stream`
-                                                                                                                                                                                                                   
-- `sizeBytes` = file size
-                                                                                                                                                                                                                   
-- `ingestRequestId` exists
-                                                                                                                                                                                                                   
-- `tags` matches metadata tags or `[]`
-                                                                                                                                                                                                                   
-- `client` matches metadata client or `{}`
-                                                                                                                                                                                                                   
-- `custom` contains the original metadata JSON object when valid JSON object was supplied
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 9. Verify file uploaded to MinIO
-                                                                                                                                                                                                                   
-
-
-Expected object key format:
-                                                                                                                                                                                                                   
-
-document-ingestion/{tenantId}/{documentId}/{originalFilename}
-                                                                                                                                                                                                                   
-
-
-
-
-document-ingestion/1/25/sample.txt
-                                                                                                                                                                                                                   
-
-@@ -1,3 +1,4 @@
-                                                                                                                                                                                                                   
--This key should match the `s3Key` returned by the API.                                                                                                                                                            
-+You can verify using MinIO UI or `mc`.
-
-----                                                                                                                                                                                                               
-+Example `mc` commands:
-                                                                                                                                                                                                                   
--## 11. Verify Kafka worker processed the event
--                                                                                                                                                                                                                  
--Check logs from `kafka-service`.
--                                                                                                                                                                                                                  
--Expected sequence:
--                                                                                                                                                                                                                  
--1. event consumed from topic `document-ingestion-topic`                                                                                                                                                           
--2. file downloaded from MinIO                                                                                                                                                                                     
--3. text extracted by Tika                                                                                                                                                                                         
--4. chunks generated                                                                                                                                                                                               
--5. embeddings generated                                                                                                                                                                                           
--6. `document_chunks` inserted                                                                                                                                                                                     
--7. document status updated to `PROCESSED`                                                                                                                                                                         
--
-----
--                                                                                                                                                                                                                  
--## 12. Verify chunk rows were created
--                                                                                                                                                                                                                  
--Run:
--                                                                                                                                                                                                                  
-
-
-mc alias set local http://localhost:9001 minioadmin minioadmin
-                                                                                                                                                                                                                   
-mc ls local/document-ingestion
-                                                                                                                                                                                                                   
-mc find local/document-ingestion --name "sample.txt"
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- uploaded object exists
-                                                                                                                                                                                                                   
-- key matches returned `s3Key`
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 10. Verify Kafka worker processed the event
-                                                                                                                                                                                                                   
-
-
-Check `kafka-service` logs.
-                                                                                                                                                                                                                   
-
-
-Expected sequence:
-                                                                                                                                                                                                                   
-1. event consumed from `document-ingestion-topic`
-                                                                                                                                                                                                                   
-2. file downloaded from MinIO
-                                                                                                                                                                                                                   
-3. text extracted with Tika
-                                                                                                                                                                                                                   
-4. chunks generated
-                                                                                                                                                                                                                   
-5. embeddings generated
-                                                                                                                                                                                                                   
-6. chunk rows inserted
-                                                                                                                                                                                                                   
-7. document status updated to `PROCESSED`
-                                                                                                                                                                                                                   
-
-
-If you want to inspect topic messages manually:
-                                                                                                                                                                                                                   
-
-kafka-console-consumer.sh \
-                                                                                                                                                                                                                   
---bootstrap-server localhost:9092 \
-                                                                                                                                                                                                                   
---topic document-ingestion-topic \
-                                                                                                                                                                                                                   
---from-beginning
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 11. Verify chunk rows inserted
-                                                                                                                                                                                                                   
-
-
-Use your `documentId`.
-                                                                                                                                                                                                                   
-
-
-Run:
-                                                                                                                                                                                                                   
-
-select id, tenant_id, document_id, content, metadata, created_at
-                                                                                                                                                                                                                   
-from public.document_chunks
-                                                                                                                                                                                                                   
-where document_id = YOUR_DOCUMENT_ID
-                                                                                                                                                                                                                   
-order by id;
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- one or more chunk rows exist
-                                                                                                                                                                                                                   
-- `tenant_id` and `document_id` are correct
-                                                                                                                                                                                                                   
-
-
-Focused chunk metadata query:
-                                                                                                                                                                                                                   
-
-select
-                                                                                                                                                                                                                   
-id,
-                                                                                                                                                                                                                   
-metadata->>'chunkIndex' as chunk_index,
-                                                                                                                                                                                                                   
-metadata->>'chunkStart' as chunk_start,
-                                                                                                                                                                                                                   
-metadata->>'chunkEnd' as chunk_end,
-                                                                                                                                                                                                                   
-metadata->>'chunkHash' as chunk_hash,
-                                                                                                                                                                                                                   
-metadata->>'s3Key' as s3_key,
-                                                                                                                                                                                                                   
-metadata->>'title' as title,
-                                                                                                                                                                                                                   
-metadata->>'source' as source,
-                                                                                                                                                                                                                   
-metadata->>'ingestRequestId' as ingest_request_id
-                                                                                                                                                                                                                   
-from public.document_chunks
-                                                                                                                                                                                                                   
-where document_id = YOUR_DOCUMENT_ID
-                                                                                                                                                                                                                   
-order by (metadata->>'chunkIndex')::int;
-                                                                                                                                                                                                                   
-
-
-
-Expected metadata per chunk:
-                                                                                                                                                                                                                   
-- `chunkIndex`
-                                                                                                                                                                                                                   
-- `chunkStart`
-                                                                                                                                                                                                                   
-- `chunkEnd`
-                                                                                                                                                                                                                   
-- `chunkHash`
-                                                                                                                                                                                                                   
-- `s3Key`
-                                                                                                                                                                                                                   
-- `title`
-                                                                                                                                                                                                                   
-- `source`
-                                                                                                                                                                                                                   
-- `ingestRequestId`
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 12. Verify final document status
-                                                                                                                                                                                                                   
-
-
-Run:
-                                                                                                                                                                                                                   
-
-select id, status, updated_at
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-where id = YOUR_DOCUMENT_ID;
-                                                                                                                                                                                                                   
-
-
-
-Expected on success:
-                                                                                                                                                                                                                   
-- `PROCESSED`
-                                                                                                                                                                                                                   
-
-
-Expected on failure:
-                                                                                                                                                                                                                   
-- `FAILED`
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 13. Manual failure tests
-                                                                                                                                                                                                                   
-
-
-### 13.1 Call gateway without token
-                                                                                                                                                                                                                   
-
-curl -X POST "http://localhost:9000/v1/ingest/docs" \
-                                                                                                                                                                                                                   
--F "file=@/path/to/sample.txt"
-                                                                                                                                                                                                                   
-
-@@ -1,8 +1,8 @@
-                                                                                                                                                                                                                   
-Expected:
--                                                                                                                                                                                                                  
-- request rejected                                                                                                                                                                                                 
-- unauthorized response
-
- ---
-                                                                                                                                                                                                                   
--### 14.2 Token missing `preferred_username`                                                                                                                                                                       
-+### 13.2 Use token without `preferred_username`
-
--Use a token that does not include `preferred_username`.                                                                                                                                                           
-+Use a JWT missing `preferred_username`.
-
-Expected:
--                                                                                                                                                                                                                  
-- gateway rejects request
-  -- status `401 Unauthorized`
-  +- `401 Unauthorized`
-
- ---
-                                                                                                                                                                                                                   
--### 14.3 Unknown tenant                                                                                                                                                                                           
-+### 13.3 Unknown tenant
-
--Use a token whose `preferred_username` is not present in `public.tenants.username`.                                                                                                                               
-+Use a token whose `preferred_username` does not exist in DB.
-
--Expected:                                                                                                                                                                                                         
-+Check current tenants:
-
--- brain service fails tenant resolution                                                                                                                                                                           
--- request returns `404 Not Found`                                                                                                                                                                                 
--
-----
--                                                                                                                                                                                                                  
--### 14.4 Invalid metadata JSON
--                                                                                                                                                                                                                  
--Send invalid JSON:
--                                                                                                                                                                                                                  
-
-
-select id, username
-                                                                                                                                                                                                                   
-from public.tenants
-                                                                                                                                                                                                                   
-order by id;
-                                                                                                                                                                                                                   
-
-
-
-Then call ingest with that token.
-                                                                                                                                                                                                                   
-
-
-Expected:
-                                                                                                                                                                                                                   
-- brain service fails tenant lookup
-                                                                                                                                                                                                                   
-- request returns `404 Not Found`
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-### 13.4 Invalid metadata JSON
-                                                                                                                                                                                                                   
-
-
-Send invalid metadata:
-                                                                                                                                                                                                                   
-
-curl -X POST "http://localhost:9000/v1/ingest/docs" \
-                                                                                                                                                                                                                   
--H "Authorization: Bearer $ACCESS_TOKEN" \
-                                                                                                                                                                                                                   
--F "file=@/path/to/sample.txt" \
-                                                                                                                                                                                                                   
--F 'metadata=not-json'
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- request still succeeds if file and tenant are valid
-                                                                                                                                                                                                                   
-- metadata falls back to empty object
-                                                                                                                                                                                                                   
-- stored metadata has:
-                                                                                                                                                                                                                   
-   - `tags = []`
-
-   - `client = {}`
-
-- `custom` is absent
-                                                                                                                                                                                                                   
-
-
-Verify:
-                                                                                                                                                                                                                   
-
-select
-                                                                                                                                                                                                                   
-metadata->'tags' as tags,
-                                                                                                                                                                                                                   
-metadata->'client' as client,
-                                                                                                                                                                                                                   
-metadata->'custom' as custom
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc
-                                                                                                                                                                                                                   
-limit 1;
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-### 13.5 MinIO failure after row insert
-                                                                                                                                                                                                                   
-
-
-Stop MinIO, then send a valid request:
-                                                                                                                                                                                                                   
-
-curl -X POST "http://localhost:9000/v1/ingest/docs" \
-                                                                                                                                                                                                                   
--H "Authorization: Bearer $ACCESS_TOKEN" \
-                                                                                                                                                                                                                   
--F "file=@/path/to/sample.txt"
-                                                                                                                                                                                                                   
-
-@@ -1,34 +1,8 @@
-                                                                                                                                                                                                                   
-Expected:                                                                                                                                                                                                          
-+- row may already be inserted                                                                                                                                                                                     
-+- upload fails                                                                                                                                                                                                    
-+- document status becomes `FAILED`
-
--- request rejected                                                                                                                                                                                                
--- unauthorized response                                                                                                                                                                                           
-+Verify:
-
-----
--                                                                                                                                                                                                                  
--### 14.2 Token missing `preferred_username`
--                                                                                                                                                                                                                  
--Use a token that does not include `preferred_username`.
--                                                                                                                                                                                                                  
--Expected:
--                                                                                                                                                                                                                  
--- gateway rejects request                                                                                                                                                                                         
--- status `401 Unauthorized`                                                                                                                                                                                       
--
-----
--                                                                                                                                                                                                                  
--### 14.3 Unknown tenant
--                                                                                                                                                                                                                  
--Use a token whose `preferred_username` is not present in `public.tenants.username`.
--                                                                                                                                                                                                                  
--Expected:
--                                                                                                                                                                                                                  
--- brain service fails tenant resolution                                                                                                                                                                           
--- request returns `404 Not Found`                                                                                                                                                                                 
--
-----
--                                                                                                                                                                                                                  
--### 14.4 Invalid metadata JSON
--                                                                                                                                                                                                                  
--Send invalid JSON:
--                                                                                                                                                                                                                  
-
-
-select id, status
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc
-                                                                                                                                                                                                                   
-limit 1;
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-### 13.6 Kafka publish failure after row insert
-                                                                                                                                                                                                                   
-
-
-Stop Kafka, then send a valid request:
-                                                                                                                                                                                                                   
-
-curl -X POST "http://localhost:9000/v1/ingest/docs" \
-                                                                                                                                                                                                                   
--H "Authorization: Bearer $ACCESS_TOKEN" \
-                                                                                                                                                                                                                   
--F "file=@/path/to/sample.txt"
-                                                                                                                                                                                                                   
-
-@@ -1,34 +1,8 @@
-                                                                                                                                                                                                                   
-Expected:                                                                                                                                                                                                          
-+- row inserted                                                                                                                                                                                                    
-+- Kafka publish fails                                                                                                                                                                                             
-+- document status becomes `FAILED`
-
--- request rejected                                                                                                                                                                                                
--- unauthorized response                                                                                                                                                                                           
-+Verify:
-
-----
--                                                                                                                                                                                                                  
--### 14.2 Token missing `preferred_username`
--                                                                                                                                                                                                                  
--Use a token that does not include `preferred_username`.
--                                                                                                                                                                                                                  
--Expected:
--                                                                                                                                                                                                                  
--- gateway rejects request                                                                                                                                                                                         
--- status `401 Unauthorized`                                                                                                                                                                                       
--
-----
--                                                                                                                                                                                                                  
--### 14.3 Unknown tenant
--                                                                                                                                                                                                                  
--Use a token whose `preferred_username` is not present in `public.tenants.username`.
--                                                                                                                                                                                                                  
--Expected:
--                                                                                                                                                                                                                  
--- brain service fails tenant resolution                                                                                                                                                                           
--- request returns `404 Not Found`                                                                                                                                                                                 
--
-----
--                                                                                                                                                                                                                  
--### 14.4 Invalid metadata JSON
--                                                                                                                                                                                                                  
--Send invalid JSON:
--                                                                                                                                                                                                                  
-
-
-select id, status
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc
-                                                                                                                                                                                                                   
-limit 1;
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-### 13.7 Worker-side failure
-                                                                                                                                                                                                                   
-
-
-Cause Kafka worker processing failure, for example:
-                                                                                                                                                                                                                   
-- stop MinIO after event publish
-                                                                                                                                                                                                                   
-- break embedding configuration
-                                                                                                                                                                                                                   
-- provide a file that causes extraction or processing failure
-                                                                                                                                                                                                                   
-
-
-Expected:
-                                                                                                                                                                                                                   
-- worker catches exception
-                                                                                                                                                                                                                   
-- document status becomes `FAILED`
-                                                                                                                                                                                                                   
-
-
-Verify:
-                                                                                                                                                                                                                   
-
-select id, status
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-where id = YOUR_DOCUMENT_ID;
-                                                                                                                                                                                                                   
-
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 14. Manual idempotent redelivery test
-                                                                                                                                                                                                                   
-
-
-Your DB already has the unique constraint:
-                                                                                                                                                                                                                   
-
-(tenant_id, document_id, chunk_index_int, chunk_hash_text)
-                                                                                                                                                                                                                   
-
-
-
-This is used to prevent duplicate logical chunks.
-                                                                                                                                                                                                                   
-
-
-### Step 1: Check current chunk count
-                                                                                                                                                                                                                   
-
-select count(*)
-                                                                                                                                                                                                                   
-from public.document_chunks
-                                                                                                                                                                                                                   
-where document_id = YOUR_DOCUMENT_ID;
-                                                                                                                                                                                                                   
-
-
-
-Save the count.
-                                                                                                                                                                                                                   
-
-
-### Step 2: Re-send the same Kafka event
-                                                                                                                                                                                                                   
-
-
-Use the same event values from the original request.
-                                                                                                                                                                                                                   
-
-
-Example event payload:
-                                                                                                                                                                                                                   
-
-{
-                                                                                                                                                                                                                   
-"documentId": 25,
-                                                                                                                                                                                                                   
-"tenantId": 1,
-                                                                                                                                                                                                                   
-"title": "Sample Doc",
-                                                                                                                                                                                                                   
-"source": "manual-upload",
-                                                                                                                                                                                                                   
-"s3Key": "document-ingestion/1/25/sample.txt",
-                                                                                                                                                                                                                   
-"bucket": "document-ingestion",
-                                                                                                                                                                                                                   
-"uploader": "hassan",
-                                                                                                                                                                                                                   
-"originalFilename": "sample.txt",
-                                                                                                                                                                                                                   
-"contentType": "text/plain",
-                                                                                                                                                                                                                   
-"sizeBytes": 123,
-                                                                                                                                                                                                                   
-"ingestRequestId": "11111111-1111-1111-1111-111111111111",
-                                                                                                                                                                                                                   
-"status": "PENDING",
-                                                                                                                                                                                                                   
-"metadata": {
-                                                                                                                                                                                                                   
-
-"uploader": "hassan",
-                                                                                                                                                                                                                   
-"originalFilename": "sample.txt",
-                                                                                                                                                                                                                   
-"contentType": "text/plain",
-                                                                                                                                                                                                                   
-"sizeBytes": 123,
-                                                                                                                                                                                                                   
-"ingestRequestId": "11111111-1111-1111-1111-111111111111",
-                                                                                                                                                                                                                   
-"tags": [],
-                                                                                                                                                                                                                   
-"client": {}
-                                                                                                                                                                                                                   
-
-}
-                                                                                                                                                                                                                   
-}
-                                                                                                                                                                                                                   
-
-
-
-Example Kafka producer command:
-                                                                                                                                                                                                                   
-
-kafka-console-producer.sh \
-                                                                                                                                                                                                                   
---bootstrap-server localhost:9092 \
-                                                                                                                                                                                                                   
---topic document-ingestion-topic
-                                                                                                                                                                                                                   
-
-
-
-Paste the JSON event and send it.
-                                                                                                                                                                                                                   
-
-
-### Step 3: Check chunk count again
-                                                                                                                                                                                                                   
-
-select count(*)
-                                                                                                                                                                                                                   
-from public.document_chunks
-                                                                                                                                                                                                                   
-where document_id = YOUR_DOCUMENT_ID;
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- count does not increase for duplicate logical chunks
-                                                                                                                                                                                                                   
-
-
-### Step 4: Verify document still processed
-                                                                                                                                                                                                                   
-
-select id, status
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-where id = YOUR_DOCUMENT_ID;
-                                                                                                                                                                                                                   
-
-
-
-Expected:
-                                                                                                                                                                                                                   
-- `PROCESSED`
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 15. Quick end-to-end checklist
-                                                                                                                                                                                                                   
-
-
-Use this checklist:
-                                                                                                                                                                                                                   
-
-
-- token contains `preferred_username`
-                                                                                                                                                                                                                   
-- matching tenant exists in DB
-                                                                                                                                                                                                                   
-- gateway accepts bearer token
-                                                                                                                                                                                                                   
-- gateway injects `X-Authenticated-User`
-                                                                                                                                                                                                                   
-- `POST /v1/ingest/docs` returns `202`
-                                                                                                                                                                                                                   
-- document row inserted with `PENDING`
-                                                                                                                                                                                                                   
-- file uploaded to MinIO
-                                                                                                                                                                                                                   
-- Kafka event published
-                                                                                                                                                                                                                   
-- worker consumes event
-                                                                                                                                                                                                                   
-- Tika extracts text
-                                                                                                                                                                                                                   
-- chunks inserted
-                                                                                                                                                                                                                   
-- embeddings stored
-                                                                                                                                                                                                                   
-- document becomes `PROCESSED`
-                                                                                                                                                                                                                   
-- failures become `FAILED`
-                                                                                                                                                                                                                   
-- redelivery does not duplicate logical chunks
-                                                                                                                                                                                                                   
-
-
----
-                                                                                                                                                                                                                   
-
-
-## 16. Useful SQL reference
-                                                                                                                                                                                                                   
-
-
-List tenants:
-                                                                                                                                                                                                                   
-
-select id, username, status
-                                                                                                                                                                                                                   
-from public.tenants
-                                                                                                                                                                                                                   
-order by id;
-                                                                                                                                                                                                                   
-
-
-
-List recent documents:
-                                                                                                                                                                                                                   
-
-select id, tenant_id, title, source, status, metadata
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc;
-                                                                                                                                                                                                                   
-
-
-
-Inspect latest document metadata:
-                                                                                                                                                                                                                   
-
-select
-                                                                                                                                                                                                                   
-id,
-                                                                                                                                                                                                                   
-metadata->>'uploader' as uploader,
-                                                                                                                                                                                                                   
-metadata->>'originalFilename' as original_filename,
-                                                                                                                                                                                                                   
-metadata->>'contentType' as content_type,
-                                                                                                                                                                                                                   
-metadata->>'sizeBytes' as size_bytes,
-                                                                                                                                                                                                                   
-metadata->>'ingestRequestId' as ingest_request_id,
-                                                                                                                                                                                                                   
-metadata->'tags' as tags,
-                                                                                                                                                                                                                   
-metadata->'client' as client,
-                                                                                                                                                                                                                   
-metadata->'custom' as custom
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-order by id desc
-                                                                                                                                                                                                                   
-limit 1;
-                                                                                                                                                                                                                   
-
-
-
-List chunks for one document:
-                                                                                                                                                                                                                   
-
-select id, tenant_id, document_id, content, metadata
-                                                                                                                                                                                                                   
-from public.document_chunks
-                                                                                                                                                                                                                   
-where document_id = YOUR_DOCUMENT_ID
-                                                                                                                                                                                                                   
-order by id;
-                                                                                                                                                                                                                   
-
-
-
-Inspect chunk metadata:
-                                                                                                                                                                                                                   
-
-select
-                                                                                                                                                                                                                   
-id,
-                                                                                                                                                                                                                   
-metadata->>'chunkIndex' as chunk_index,
-                                                                                                                                                                                                                   
-metadata->>'chunkStart' as chunk_start,
-                                                                                                                                                                                                                   
-metadata->>'chunkEnd' as chunk_end,
-                                                                                                                                                                                                                   
-metadata->>'chunkHash' as chunk_hash,
-                                                                                                                                                                                                                   
-metadata->>'s3Key' as s3_key,
-                                                                                                                                                                                                                   
-metadata->>'title' as title,
-                                                                                                                                                                                                                   
-metadata->>'source' as source,
-                                                                                                                                                                                                                   
-metadata->>'ingestRequestId' as ingest_request_id
-                                                                                                                                                                                                                   
-from public.document_chunks
-                                                                                                                                                                                                                   
-where document_id = YOUR_DOCUMENT_ID
-                                                                                                                                                                                                                   
-order by (metadata->>'chunkIndex')::int;
-                                                                                                                                                                                                                   
-
-
-
-Check final status:
-                                                                                                                                                                                                                   
-
-select id, status, updated_at
-                                                                                                                                                                                                                   
-from public.documents
-                                                                                                                                                                                                                   
-where id = YOUR_DOCUMENT_ID;
-                                                                                                                                                                                                                   
-


document-ingestion/{tenantId}/{documentId}/{originalFilename}


@@ -1 +1,6 @@

+- builds metadata with:
+  - `uploader`
+  - `originalFilename`
+  - `contentType`
+  - `sizeBytes`
+  - `ingestRequestId`
+  - `tags`
+  - `tags`
+  - `client`
+  - `custom`                                                                                                                                                                                                       
     +- publishes `DocumentIngestionEvent` to Kafka topic:
+  - `document-ingestion-topic`                                                                                                                                                                                     
     +- returns `202 Accepted`                                                                                                                                                                                           
     +- sets `documents.status = FAILED` if MinIO or Kafka fails after row insert

+### Kafka Service
+
+The Kafka worker now:
+
+- consumes `document-ingestion-topic`                                                                                                                                                                              
+- downloads file from MinIO                                                                                                                                                                                        
+- extracts text using Apache Tika                                                                                                                                                                                  
+- chunks text using:
+  - size `500`
+  - overlap `50`                                                                                                                                                                                                   
     +- builds chunk metadata:
+  - `chunkIndex`
+  - `chunkStart`
+  - `chunkEnd`
+  - `chunkHash`
+  - `s3Key`
+  - `title`
+  - `source`
+  - `ingestRequestId`                                                                                                                                                                                              
     +- generates embeddings                                                                                                                                                                                             
     +- inserts rows into `public.document_chunks`                                                                                                                                                                       
     +- sets `documents.status = PROCESSED` on success                                                                                                                                                                   
     +- sets `documents.status = FAILED` on failure                                                                                                                                                                      
     +- handles duplicate Kafka redelivery idempotently using the existing DB unique constraint
+
+---
+
+## 2. Prerequisites
+
+Before testing, make sure these are running:
+
+- PostgreSQL                                                                                                                                                                                                       
+- Kafka                                                                                                                                                                                                            
+- MinIO                                                                                                                                                                                                            
+- Keycloak                                                                                                                                                                                                         
+- `whatsapp-brain-service`                                                                                                                                                                                         
+- `kafka-service`                                                                                                                                                                                                  
+- `api-gateway`
+
+Also make sure:
+
+- DB schema already exists from `V1__init.sql`                                                                                                                                                                     
+- MinIO bucket exists:
+  - `document-ingestion`                                                                                                                                                                                           
     +- JWT contains:
+  - `preferred_username`                                                                                                                                                                                           
     +- a tenant exists in DB with username matching `preferred_username`
+
+---
+
+## 3. Startup order
+
+Recommended order:
+
+1. PostgreSQL                                                                                                                                                                                                      
+2. Kafka                                                                                                                                                                                                           
+3. MinIO                                                                                                                                                                                                           
+4. Keycloak                                                                                                                                                                                                        
+5. `whatsapp-brain-service`                                                                                                                                                                                        
+6. `kafka-service`                                                                                                                                                                                                 
+7. `api-gateway`
+
+---
+
+## 4. Verify tenant exists
+
+Current tenant resolution depends on username.
+
+Run this SQL:
+


select id, username, status

from public.tenants

order by id;




Expected:



- a tenant row exists

- one tenant username matches the JWT `preferred_username`



Example:



- if token has `preferred_username = hassan`

- DB must contain tenant with `username = hassan`


                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 5. Get a Keycloak access token



Example request:


curl -X POST "http://localhost:8180/realms/replato-gateway/protocol/openid-connect/token" \

-H "Content-Type: application/x-www-form-urlencoded" \

-d "client_id=replato-gateway" \

-d "client_secret=Fdu40gsWOf0HiSqSftcL5KkmIsVwdDW7" \

-d "username=YOUR_USERNAME" \

-d "password=YOUR_PASSWORD" \

-d "grant_type=password"




Expected:



- JSON response with `access_token`



Export it for reuse:


export ACCESS_TOKEN="PASTE_ACCESS_TOKEN_HERE"




Optional: inspect token claims and confirm `preferred_username` exists:


python - <<'PY'

import os, json, base64

token = os.environ["ACCESS_TOKEN"].split(".")[1]

token += "=" * (-len(token) % 4)

print(json.dumps(json.loads(base64.urlsafe_b64decode(token)), indent=2))

PY



                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 6. Send a successful ingest request



Use this curl command:


curl -X POST "http://localhost:9000/v1/ingest/docs" \

-H "Authorization: Bearer $ACCESS_TOKEN" \

-F "file=@/path/to/sample.txt" \

-F "title=Sample Doc" \

-F "source=manual-upload" \

-F 'metadata={"tags":["invoice","q1"],"client":{"id":"c-123","name":"ACME"},"department":"finance"}'




Expected:



- HTTP `202 Accepted`



Expected response example:


{

"documentId": 1,

"tenantId": 1,

"status": "PENDING",

"s3Key": "document-ingestion/1/1/sample.txt",

"ingestRequestId": "11111111-1111-1111-1111-111111111111"

}




Save these values from the response:



- `documentId`

- `tenantId`

- `s3Key`

- `ingestRequestId`



You will use them in the checks below.


                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 7. Verify document row inserted



Run:


select id, tenant_id, title, source, status, metadata, created_at, updated_at

from public.documents

order by id desc;




Expected:



- newest row exists

- `tenant_id` matches expected tenant

- `title` is request title or filename fallback

- `source` is request source or `UPLOAD`

- initial status is `PENDING`



To inspect only latest row:


select id, tenant_id, title, source, status

from public.documents

order by id desc

limit 1;



                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 8. Verify stored metadata



Run:


select

id,

metadata->>'uploader' as uploader,

metadata->>'originalFilename' as original_filename,

metadata->>'contentType' as content_type,

metadata->>'sizeBytes' as size_bytes,

metadata->>'ingestRequestId' as ingest_request_id,

metadata->'tags' as tags,

metadata->'client' as client,

metadata->'custom' as custom

from public.documents

order by id desc

limit 1;




Expected:



- `uploader` = JWT `preferred_username`

- `originalFilename` = uploaded filename

- `contentType` = MIME type or `application/octet-stream`

- `sizeBytes` = file size

- `ingestRequestId` exists

- `tags` matches metadata tags or `[]`

- `client` matches metadata client or `{}`

- `custom` contains the original metadata JSON object when valid JSON object was supplied


                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 9. Verify file uploaded to MinIO



Expected object key format:


document-ingestion/{tenantId}/{documentId}/{originalFilename}





document-ingestion/1/25/sample.txt


@@ -1,3 +1,4 @@

-This key should match the `s3Key` returned by the API.                                                                                                                                                             
+You can verify using MinIO UI or `mc`.
                                                                                                                                                                                                                    
----                                                                                                                                                                                                                
+Example `mc` commands:

-## 11. Verify Kafka worker processed the event
-                                                                                                                                                                                                                   
-Check logs from `kafka-service`.
-                                                                                                                                                                                                                   
-Expected sequence:
-                                                                                                                                                                                                                   
-1. event consumed from topic `document-ingestion-topic`                                                                                                                                                            
-2. file downloaded from MinIO                                                                                                                                                                                      
-3. text extracted by Tika                                                                                                                                                                                          
-4. chunks generated                                                                                                                                                                                                
-5. embeddings generated                                                                                                                                                                                            
-6. `document_chunks` inserted                                                                                                                                                                                      
-7. document status updated to `PROCESSED`
-
----
-                                                                                                                                                                                                                   
-## 12. Verify chunk rows were created
-                                                                                                                                                                                                                   
-Run:
-                                                                                                                                                                                                                   


mc alias set local http://localhost:9001 minioadmin minioadmin

mc ls local/document-ingestion

mc find local/document-ingestion --name "sample.txt"




Expected:



- uploaded object exists

- key matches returned `s3Key`


                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 10. Verify Kafka worker processed the event



Check `kafka-service` logs.



Expected sequence:



1. event consumed from `document-ingestion-topic`

2. file downloaded from MinIO

3. text extracted with Tika

4. chunks generated

5. embeddings generated

6. chunk rows inserted

7. document status updated to `PROCESSED`



If you want to inspect topic messages manually:


kafka-console-consumer.sh \

--bootstrap-server localhost:9092 \

--topic document-ingestion-topic \

--from-beginning



                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 11. Verify chunk rows inserted



Use your `documentId`.



Run:


select id, tenant_id, document_id, content, metadata, created_at

from public.document_chunks

where document_id = YOUR_DOCUMENT_ID

order by id;




Expected:



- one or more chunk rows exist

- `tenant_id` and `document_id` are correct



Focused chunk metadata query:


select

id,

metadata->>'chunkIndex' as chunk_index,

metadata->>'chunkStart' as chunk_start,

metadata->>'chunkEnd' as chunk_end,

metadata->>'chunkHash' as chunk_hash,

metadata->>'s3Key' as s3_key,

metadata->>'title' as title,

metadata->>'source' as source,

metadata->>'ingestRequestId' as ingest_request_id

from public.document_chunks

where document_id = YOUR_DOCUMENT_ID

order by (metadata->>'chunkIndex')::int;




Expected metadata per chunk:



- `chunkIndex`

- `chunkStart`

- `chunkEnd`

- `chunkHash`

- `s3Key`

- `title`

- `source`

- `ingestRequestId`


                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 12. Verify final document status



Run:


select id, status, updated_at

from public.documents

where id = YOUR_DOCUMENT_ID;




Expected on success:



- `PROCESSED`



Expected on failure:



- `FAILED`


                                                                                                                                                                                                                    
---                                                                                                                                                                                                                 



## 13. Manual failure tests



### 13.1 Call gateway without token


curl -X POST "http://localhost:9000/v1/ingest/docs" \

-F "file=@/path/to/sample.txt"


@@ -2,5 +2,6 @@                                                                                                                                                                                                     
Expected:

- request rejected
- unauthorized response

----                                                                                                                                                                                                                
+### 13.2 Use token without `preferred_username`

-### 14.2 Token missing `preferred_username`
-                                                                                                                                                                                                                   
-Use a token that does not include `preferred_username`.                                                                                                                                                            
+Use a JWT missing `preferred_username`.

Expected:

- gateway rejects request                                                                                                                                                                                          
  -- status `401 Unauthorized`                                                                                                                                                                                        
  +- `401 Unauthorized`

----                                                                                                                                                                                                                
+### 13.3 Unknown tenant

-### 14.3 Unknown tenant                                                                                                                                                                                            
+Use a token whose `preferred_username` does not exist in DB.

-Use a token whose `preferred_username` is not present in `public.tenants.username`.                                                                                                                                
+Check current tenants:

-Expected:
-                                                                                                                                                                                                                   
-- brain service fails tenant resolution                                                                                                                                                                            
-- request returns `404 Not Found`
-
----
-                                                                                                                                                                                                                   
-### 14.4 Invalid metadata JSON
-                                                                                                                                                                                                                   
-Send invalid JSON:
-                                                                                                                                                                                                                   


select id, username

from public.tenants

order by id;




Then call ingest with that token.



Expected:



- brain service fails tenant lookup

- request returns `404 Not Found`



### 13.4 Invalid metadata JSON



Send invalid metadata:


curl -X POST "http://localhost:9000/v1/ingest/docs" \

-H "Authorization: Bearer $ACCESS_TOKEN" \

-F "file=@/path/to/sample.txt" \

-F 'metadata=not-json'




Expected:



- request still succeeds if file and tenant are valid

- metadata falls back to empty object

- stored metadata has:

    - `tags = []`

    - `client = {}`

- `custom` is absent



Verify:


select

metadata->'tags' as tags,

metadata->'client' as client,

metadata->'custom' as custom

from public.documents

order by id desc


limit 1;


limit 1;      