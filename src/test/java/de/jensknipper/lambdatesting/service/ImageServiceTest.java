package de.jensknipper.lambdatesting.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ImageServiceTest {

  private final int maxDimension = 300;
  private final ImageService imageService = new ImageService(maxDimension);

  @Test
  void shouldResizeImage() throws IOException {
    // given
    File testImage = new File("src/test/resources/image.jpg");
    BufferedImage image = ImageIO.read(testImage);

    // when
    final BufferedImage resizedImage = imageService.resize(image);

    // then
    assertThat(resizedImage.getHeight()).isLessThanOrEqualTo(maxDimension);
    assertThat(resizedImage.getWidth()).isLessThanOrEqualTo(maxDimension);

    // manual testing only
    File outFile = new File("target/image.jpg");
    ImageIO.write(resizedImage, "jpg", outFile);
  }
}
