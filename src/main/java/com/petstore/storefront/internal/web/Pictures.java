package com.petstore.storefront.internal.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Resolves a picture file name to something that can be served.
 *
 * <p>The catalog data refers to pictures by the file names of the legacy application ({@code
 * fish3.gif}, {@code dog1.gif}, ...). Those image files may not be redistributed, so this
 * repository does not contain them. A picture is taken from the local folder if a file of that name
 * exists there; otherwise a placeholder graphic that matches the kind of picture is used.
 */
@Component
class Pictures {

  /** A plain file name: no path separators, no leading dot, nothing but safe characters. */
  private static final Pattern SAFE_NAME =
      Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*(\\.[A-Za-z0-9]+)?");

  private final Path localDirectory;

  Pictures(ImageProperties properties) {
    this.localDirectory = properties.localDirectory().toAbsolutePath().normalize();
  }

  static boolean isSafeName(String name) {
    return name != null && SAFE_NAME.matcher(name).matches();
  }

  /** The file of that name in the local folder, if the name is safe and the file exists. */
  Optional<Path> local(String name) {
    if (!isSafeName(name)) {
      return Optional.empty();
    }
    Path file = localDirectory.resolve(name).normalize();
    if (!file.startsWith(localDirectory) || !Files.isRegularFile(file)) {
      return Optional.empty();
    }
    return Optional.of(file);
  }

  boolean isLocal(String name) {
    return local(name).isPresent();
  }

  /** Name of the placeholder graphic for a picture file name. */
  static String placeholder(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.contains("logo")) {
      return "logo";
    }
    if (lower.endsWith("_flag.gif")) {
      return switch (lower.substring(0, 2)) {
        case "ja" -> "language-ja";
        case "zh" -> "language-zh";
        default -> "language-en";
      };
    }
    if (lower.contains("fish")) {
      return "fish";
    }
    if (lower.contains("bird")) {
      return "bird";
    }
    if (lower.contains("cat")) {
      return "cat";
    }
    if (lower.contains("dog")) {
      return "dog";
    }
    if (lower.contains("lizard") || lower.contains("frog") || lower.contains("reptile")) {
      return "reptile";
    }
    return "pet";
  }
}
