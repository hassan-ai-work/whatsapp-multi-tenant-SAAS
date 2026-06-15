package com.levosoft.microservice.kafka.storage.service;

import com.levosoft.microservice.kafka.storage.config.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final S3Client s3Client;
    private final S3StorageProperties s3StorageProperties;

    public ResponseInputStream<GetObjectResponse> download(String bucket, String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket != null && !bucket.isBlank() ? bucket : s3StorageProperties.bucket())
                .key(s3Key)
                .build();

        return s3Client.getObject(request);
    }
}
