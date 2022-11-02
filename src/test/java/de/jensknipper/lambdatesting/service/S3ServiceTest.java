package de.jensknipper.lambdatesting.service;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class S3ServiceTest {
  @Container
  private static final LocalStackContainer localStack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack"))
          .withServices(LocalStackContainer.Service.S3)
          .withEnv("DEFAULT_REGION", Region.EU_CENTRAL_1.toString());

  private static final AwsCredentialsProvider credentialsProvider =
      StaticCredentialsProvider.create(
          AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()));
  private final S3Client s3Client =
      S3Client.builder()
          .region(Region.EU_CENTRAL_1)
          .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
          .credentialsProvider(credentialsProvider)
          .build();
  private final S3Service s3Service =
      new S3Service(
          localStack.getEndpointOverride(LocalStackContainer.Service.S3), credentialsProvider);

  @Test
  void getObjectWithNonExistentBucketShouldReturnEmpty() {
    // given
    // when + then
    assertThrows(
        NoSuchBucketException.class, () -> s3Service.getObject("non-existent-bucket", "file"));
  }

  @Test
  void getObjectWithNonExistentFileShouldReturnEmpty() {
    // given
    createBucketAndFile("first-bucket", "some-random-file-name", "content");

    // when + then
    assertThrows(
        NoSuchKeyException.class, () -> s3Service.getObject("first-bucket", "non-existent-file"));
  }

  @Test
  void getObjectShouldReturnInputStream() throws IOException {
    // given
    createBucketAndFile("second-bucket", "file", "content");

    // when
    final InputStream result = s3Service.getObject("second-bucket", "file");

    // then
    final String resultAsString = new String(result.readAllBytes());
    assertThat(resultAsString).isEqualTo("content");
  }

  @Test
  void uploadFileWithNonExistentBucketShouldReturnEmpty() {
    // given
    // when + then
    assertThrows(
        NoSuchBucketException.class,
        () -> s3Service.uploadFile("non-existent-bucket", "file", new ByteArrayOutputStream()));
  }

  @Test
  void uploadFileShouldCreateFileAndReturnUrl() throws IOException {
    // given
    createBucket("third-bucket");
    final ByteArrayOutputStream fileStream = new ByteArrayOutputStream();
    final byte[] fileContent = "content".getBytes();
    fileStream.write(fileContent);

    // when
    final String result = s3Service.uploadFile("third-bucket", "file.txt", fileStream);

    // then
    // TODO use result
    final byte[] uploadedFileBytes = getFileAsBytes("third-bucket", "file.txt");
    assertThat(uploadedFileBytes).isEqualTo(fileContent);
  }

  private void createBucketAndFile(
      final String bucketName, final String fileName, final String content) {
    createBucket(bucketName);
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(fileName).build(),
        RequestBody.fromString(content));
  }

  private void createBucket(final String bucketName) {
    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
  }

  private byte[] getFileAsBytes(final String bucketName, final String fileName) {
    return s3Client
        .getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(fileName).build())
        .asByteArray();
  }
}
