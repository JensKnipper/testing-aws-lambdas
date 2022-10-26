package de.jensknipper.lambdatesting;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import de.jensknipper.lambdatesting.io.Input;
import de.jensknipper.lambdatesting.io.Output;
import de.jensknipper.lambdatesting.misc.EnvironmentHelper;
import de.jensknipper.lambdatesting.service.EventHandlingService;
import de.jensknipper.lambdatesting.service.ImageService;
import de.jensknipper.lambdatesting.service.S3Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.net.URI;

public class EventHandler implements RequestHandler<Input, Output> {
  private final EventHandlingService eventHandlingService;

  public EventHandler() {
    final URI s3EndpointOverride = EnvironmentHelper.getEnvOrDefault("S3_ENDPOINT_OVERRIDE", null);

    final String maxDimensionProperty = System.getenv("MAX_DIMENSION");
    final var maxDimension = Integer.parseInt(maxDimensionProperty);

    final AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

    S3Service s3Service = new S3Service(s3EndpointOverride, credentialsProvider);
    ImageService imageService = new ImageService(maxDimension);

    this.eventHandlingService = new EventHandlingService(s3Service, imageService);
  }

  @Override
  public Output handleRequest(final Input input, final Context context) {
    return eventHandlingService.handleEvent(input.getBucket(), input.getKey());
  }
}
