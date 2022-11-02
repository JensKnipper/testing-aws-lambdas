package de.jensknipper.lambdatesting.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

public class ImageService {
  private final String JPEG_TYPE = "jpeg";
  private final String JPG_TYPE = "jpg";
  private final String PNG_TYPE = "png";
  private final String GIF_TYPE = "gif";
  private final java.util.List<String> imageTypes =
      List.of(JPEG_TYPE, JPG_TYPE, PNG_TYPE, GIF_TYPE);

  private final int maxDimension;

  public ImageService(final int maxDimension) {
    this.maxDimension = maxDimension;
  }

  public BufferedImage resize(final BufferedImage srcImage) {
    final int srcHeight = srcImage.getHeight();
    final int srcWidth = srcImage.getWidth();

    final float scalingFactor =
        Math.min(maxDimension / (float) srcWidth, maxDimension / (float) srcHeight);
    final int width = (int) (scalingFactor * srcWidth);
    final int height = (int) (scalingFactor * srcHeight);

    final BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    final Graphics2D graphics = resizedImage.createGraphics();

    graphics.setPaint(Color.white);
    graphics.fillRect(0, 0, width, height);

    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.drawImage(srcImage, 0, 0, width, height, null);
    graphics.dispose();
    return resizedImage;
  }

  public Optional<String> getImageExtension(final String filename) {
    return Optional.ofNullable(filename)
        .filter(f -> f.contains("."))
        .map(f -> f.substring(filename.lastIndexOf(".") + 1))
        .filter(imageTypes::contains);
  }
}
