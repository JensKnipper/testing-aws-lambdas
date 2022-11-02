package de.jensknipper.lambdatesting.service;

import de.jensknipper.lambdatesting.io.Output;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EventHandlingService {
  private final S3Service s3Service;
  private final ImageService imageService;

  public EventHandlingService(final S3Service s3Service, final ImageService imageService) {
    this.s3Service = s3Service;
    this.imageService = imageService;
  }

  public Output handleEvent(final String bucket, final String fileKey) {
    try {
      final InputStream inputStream = s3Service.getObject(bucket, fileKey);

      final BufferedImage image = ImageIO.read(inputStream);
      final BufferedImage newImage = imageService.resize(image);
      final ByteArrayOutputStream outputStream = toByteStream(newImage, fileKey);

      final String newFileName = "thumbnail-" + fileKey;
      final String link = s3Service.uploadFile(bucket, newFileName, outputStream);

      return new Output(bucket, newFileName, link);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ByteArrayOutputStream toByteStream(BufferedImage newImage, String fileKey)
      throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final String fileEnding = imageService.getImageExtension(fileKey).orElseThrow();
    ImageIO.write(newImage, fileEnding, outputStream);
    return outputStream;
  }
}
