package org.opencds.cqf.cql.evaluator;

import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.evaluator.factory.ClientFactory;

public class Parameters {
    public List<ExpressionInfo> expressions;
    public List<ModelInfo> models;
    public String terminologyUrl;
    public List<ParameterInfo> parameters;
    public Map<String,String> contextParameters;
    public String libraryPath;
    public String libraryName;
    public String libraryVersion;
    public List<String> libraries;

    public ClientFactory clientFactory;
    public Boolean verbose;

}