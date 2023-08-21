package org.opencds.cqf.fhir.utility;

public class Uris {
  private Uris() {}

  public static boolean isUri(String uri) {
    if (uri == null) {
      return false;
    }

    return uri.startsWith("file:/") || uri.matches("\\w+?://.*");
  }

  public static boolean isFileUri(String uri) {
    if (uri == null) {
      return false;
    }

    return uri.startsWith("file") || !uri.matches("\\w+?://.*");
  }
}
