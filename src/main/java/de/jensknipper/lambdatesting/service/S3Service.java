package de.jensknipper.lambdatesting.service;

import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

public class S3Service {

  private final S3Client s3Client;

  public S3Service(
      @Nullable final URI endpointOverride, final AwsCredentialsProvider credentialsProvider) {
    s3Client =
        S3Client.builder()
            .region(Region.EU_CENTRAL_1)
            .credentialsProvider(credentialsProvider)
            .endpointOverride(endpointOverride)
            .build();
  }

  public InputStream getObject(final String bucket, final String key) {
    final GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucket).key(key).build();
    return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
  }

  public String uploadFile(
      final String bucketName, final String fileKey, final ByteArrayOutputStream fileStream) {
    final PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(fileKey).build();
    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileStream.toByteArray()));
    return s3Client
        .utilities()
        .getUrl(builder -> builder.bucket(bucketName).key(fileKey))
        .toExternalForm();
  }
}
