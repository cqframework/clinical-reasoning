package com.alphora.cql.service;

public class Helpers {
    public static boolean isFileUri(String uri) {
        return !uri.startsWith("file") && uri.matches("\\w+?://");
    }
}