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

  public EventHandlingService(S3Service s3Service, ImageService imageService) {
    this.s3Service = s3Service;
    this.imageService = imageService;
  }

  public Output handleEvent(String bucket, String fileKey) {
    try {
      InputStream s3Object = s3Service.getObject(bucket, fileKey);

      BufferedImage image = ImageIO.read(s3Object);
      BufferedImage newImage = imageService.resize(image);
      var outputStream = new ByteArrayOutputStream();
      String fileEnding = imageService.getImageExtension(fileKey).orElseThrow();
      ImageIO.write(newImage, fileEnding, outputStream);

      String newFileName = "thumbnail-" + fileKey;
      String link = s3Service.uploadFile(bucket, newFileName, outputStream);

      return new Output(bucket, newFileName, link);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
