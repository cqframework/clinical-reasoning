package com.alphora.cql.cli;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InputParameters {
    public Set<String> expressions;
    public Map<String,String> modelUris;
    public String terminologyUri;
    public Map<String,String> parameters;
    public Map<String,String> contextParameters;
    public String libraryPath;
    public String libraryName;
    public List<String> libraries;
}