package org.opencds.cqf.cql.evaluator;

import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.evaluator.factory.ClientFactory;

public class BuilderParameters {
    public List<ModelInfo> models;
    public String terminologyUrl;
    public String libraryPath;
    public ClientFactory clientFactory;
}