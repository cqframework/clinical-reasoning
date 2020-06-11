package org.opencds.cqf.cql.evaluator.builder.context.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderDataContext;

public interface TerminologyContext {
    public BuilderDataContext withTerminologyProvider(TerminologyProvider terminologyProvider);
    public BuilderDataContext withStringTerminologyProvider(List<String> terminologyBundles);
    public BuilderDataContext withBundleTerminologyProvider(IBaseBundle bundles);
    public BuilderDataContext withFileTerminologyProvider(String fileUri);
    public BuilderDataContext withRemoteTerminologyProvider(URL terminologyUrl) throws IOException, InterruptedException, URISyntaxException;
}
