package de.jensknipper.lambdatesting;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensknipper.lambdatesting.io.Output;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.Network.NetworkImpl;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
// TODO use IT to execute in maven verify cycle

@Testcontainers
public class ComponentTestIT {
  private static final Logger LOG = LoggerFactory.getLogger(ComponentTestIT.class);
  private static final String localstackNetworkAlias = "localstack";
  public static final int MAX_DIMENSION = 300;

  public final ObjectMapper objectMapper = new ObjectMapper();

  @Container
  private static final LocalStackContainer localStack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack"))
          .withServices(Service.LAMBDA, Service.S3)
          .withEnv("DEFAULT_REGION", Region.EU_CENTRAL_1.toString())
          .withNetwork(Network.SHARED)
          .withNetworkAliases(localstackNetworkAlias)
          .withEnv("LAMBDA_DOCKER_NETWORK", ((NetworkImpl) Network.SHARED).getName())
          .withFileSystemBind(
              new File("target/").getPath(), "/opt/code/localstack/target/", BindMode.READ_ONLY)
          .withLogConsumer(new Slf4jLogConsumer(LOG));

  private static final AwsCredentialsProvider credentialsProvider =
      StaticCredentialsProvider.create(
          AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()));

  @BeforeAll
  public static void beforeAll() throws IOException {
    createLambdaFunction();
  }

  @Test
  void testHappyPath() throws IOException {
    // given
    createBucket("bucket");
    uploadFile("bucket", "image.jpg", "src/test/resources/image.jpg");

    // when
    InvokeResponse response = invokeLambda("src/test/resources/componenttest.json");

    // then
    assertThat(response.statusCode()).isEqualTo(200);

    var resultAsJson = new String(response.payload().asByteArray());
    BufferedImage resizedImage = getImage(resultAsJson);
    assertThat(resizedImage.getHeight()).isLessThanOrEqualTo(MAX_DIMENSION);
    assertThat(resizedImage.getWidth()).isLessThanOrEqualTo(MAX_DIMENSION);
  }

  private static void createLambdaFunction() throws FileNotFoundException {
    final Map<String, String> variables =
        Map.of(
            "AWS_ACCESS_KEY_ID",
            localStack.getAccessKey(),
            "AWS_SECRET_ACCESS_KEY",
            localStack.getSecretKey(),
            "S3_ENDPOINT_OVERRIDE",
            "http://" + localstackNetworkAlias + ":" + 4566,
            "MAX_DIMENSION",
            "" + MAX_DIMENSION);

    final CreateFunctionRequest request =
        CreateFunctionRequest.builder()
            .functionName("thumbnail-generator-lambda")
            .runtime(Runtime.JAVA11)
            .handler("de.jensknipper.lambdatesting.EventHandler")
            .role("arn:aws:iam::123456:role/irrelevant")
            .packageType(PackageType.ZIP)
            .code(
                FunctionCode.builder()
                    .zipFile(
                        SdkBytes.fromInputStream(
                            new FileInputStream("target/testing-aws-lambdas-1.0.jar")))
                    .build())
            .environment(Environment.builder().variables(variables).build())
            .build();

    final CreateFunctionResponse response = getLambdaClient().createFunction(request);
    LOG.info("Created lambda response: {}", response.toString());
  }

  private InvokeResponse invokeLambda(final String filePath) throws FileNotFoundException {
    final InvokeRequest request =
        InvokeRequest.builder()
            .functionName("thumbnail-generator-lambda")
            .payload(SdkBytes.fromInputStream(new FileInputStream(filePath)))
            .invocationType(InvocationType.REQUEST_RESPONSE)
            .build();

    final InvokeResponse response = getLambdaClient().invoke(request);
    LOG.info("invoked lambda with file: {}, response: {}", filePath, response.toString());
    return response;
  }

  private static LambdaClient getLambdaClient() {
    return LambdaClient.builder()
        .region(Region.EU_CENTRAL_1)
        .credentialsProvider(credentialsProvider)
        .endpointOverride(localStack.getEndpointOverride(Service.LAMBDA))
        .build();
  }

  private void createBucket(final String bucketName) {
    final S3Client s3Client = getS3Client();
    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
  }

  private void uploadFile(final String bucketName, String key, String linkToFile) {
    final S3Client s3Client = getS3Client();
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(key).build(),
        RequestBody.fromFile(new File(linkToFile)));
  }

  private S3Client getS3Client() {
    final S3Client s3Client =
        S3Client.builder()
            .httpClientBuilder(ApacheHttpClient.builder())
            .region(Region.EU_CENTRAL_1)
            .endpointOverride(localStack.getEndpointOverride(Service.S3))
            .credentialsProvider(credentialsProvider)
            .build();
    return s3Client;
  }

  private BufferedImage getImage(String resultAsJson) throws IOException {
    Output output = objectMapper.readValue(resultAsJson, Output.class);
    URL localImageUrl =
        new URL(
            output
                .getFileLink()
                .replace("localstack", "localhost")
                .replace("4566", localStack.getMappedPort(4566).toString()));
    BufferedImage resizedImage = ImageIO.read(localImageUrl);
    return resizedImage;
  }
}
