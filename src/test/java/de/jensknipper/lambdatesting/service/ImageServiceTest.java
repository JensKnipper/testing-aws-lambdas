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
    final File testImage = new File("src/test/resources/image.png");
    final BufferedImage image = ImageIO.read(testImage);

    // when
    final BufferedImage resizedImage = imageService.resize(image);

    // then
    assertThat(resizedImage.getHeight()).isLessThanOrEqualTo(maxDimension);
    assertThat(resizedImage.getWidth()).isLessThanOrEqualTo(maxDimension);

    // manual testing only
    final File outFile = new File("target/image.png");
    ImageIO.write(resizedImage, "jpg", outFile);
  }
}
