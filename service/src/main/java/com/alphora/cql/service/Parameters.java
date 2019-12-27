package com.alphora.cql.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class Parameters {
    // LibraryName, ExpressionName
    public List<Pair<String, String>> expressions;
    public Map<String,String> modelUris;
    public String terminologyUri;
    // LibraryName, ParameterName, Value
    // LibraryName may be null.
    public Map<Pair<String, String>,String> parameters;
    public Map<String,String> contextParameters;
    public String libraryPath;
    public String libraryName;
    public String libraryVersion;
    public List<String> libraries;

    public Boolean verbose;

}