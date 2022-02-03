package org.opencds.cqf.cql.evaluator.builder.util;

public class UriUtil {
    private UriUtil() {}
    
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