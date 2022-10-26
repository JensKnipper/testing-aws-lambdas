package de.jensknipper.lambdatesting.misc;

import java.net.URI;
import java.net.URISyntaxException;

public final class EnvironmentHelper {
  private EnvironmentHelper() {}

  public static URI getEnvOrDefault(final String name, final URI defaultValue) {
    final String value = System.getenv(name);
    if (value == null || value.equals("")) {
      return defaultValue;
    }
    try {
      return new URI(value);
    } catch (final URISyntaxException e) {
      return defaultValue;
    }
  }
}
