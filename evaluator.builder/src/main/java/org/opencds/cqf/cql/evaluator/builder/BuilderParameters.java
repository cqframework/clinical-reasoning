package org.opencds.cqf.cql.evaluator.builder;

import java.util.List;
import java.util.Map;

import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;

/**
 * Parameters for default Building
 * ip.libraries = libraries;
        ip.libraryPath = libraryPath;
        ip.libraryName = libraryName;
        ip.libraryVersion = libraryVersion;
        //ip.expressions = toListOfExpressions(expressions);
        ip.terminologyUrl = terminologyUri;
        ip.models = toModelInfoList(models);
        //ip.parameters = toParameterInfoList(parameters);
        ip.contextParameters = toMap("Context Parameters", contextParameters);
        ip.verbose = verbose;
 */
public class BuilderParameters {
    public List<String> libraries;
    public String library;
    public String libraryName;
    public String libraryVersion;
    //public List<ExpressionInfo> expressions;
    public String terminology;
    public String data;
    public List<ModelInfo> models;
    //public List<ParameterInfo> parameters;
    public Map<String, String> contextParameters;
    public boolean verbose;
    public ClientFactory clientFactory;
}