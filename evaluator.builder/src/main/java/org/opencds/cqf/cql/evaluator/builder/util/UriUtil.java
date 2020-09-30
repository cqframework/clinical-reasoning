package org.opencds.cqf.cql.evaluator.builder.util;

public class UriUtil {
    public static boolean isFileUri(String uri) {
        if (uri == null) {
            return false;
        }

        return uri.startsWith("file") || !uri.matches("\\w+?://.*");
    }
}