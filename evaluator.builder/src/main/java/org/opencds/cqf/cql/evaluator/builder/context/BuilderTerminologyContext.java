package org.opencds.cqf.cql.evaluator.builder.context;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.context.api.TerminologyContext;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringTerminologyProviderBuilder;

/**
 * Provides TerminologyContext needed for CQL Evaluation
 */
public class BuilderTerminologyContext extends BuilderContext implements TerminologyContext {

    @Override
    public BuilderDataContext withTerminologyProvider(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
        return asDataContext(this);
    }

    @Override
    public BuilderDataContext withTerminologyProvider(List<String> terminologyBundles) {
        StringTerminologyProviderBuilder stringTerminologyProviderBuilder = new StringTerminologyProviderBuilder();
        this.terminologyProvider = stringTerminologyProviderBuilder.build(this.models, terminologyBundles);
        return asDataContext(this);
    }

    @Override
    // public BuilderDataContext withBundleTerminologyProvider(ModelInfo modelInfo,
    // IBaseBundle bundle) {
    public BuilderDataContext withBundleTerminologyProvider(IBaseBundle bundle) {
        if (models.get("http://hl7.org/fhir") != null) {
            BundleTerminologyProviderBuilder bundleTerminologyProviderBuilder = new BundleTerminologyProviderBuilder();
            this.terminologyProvider = bundleTerminologyProviderBuilder.build(models, bundle);
            return asDataContext(this);
        } else
            throw new IllegalArgumentException(
                    "In order to use terminology Library must use the FHIR model, Cannot find Model Info for http://hl7.org/fhir");
    }

    @Override
    public BuilderDataContext withFileTerminologyProvider(String terminologyUri) {
        if (models.get("http://hl7.org/fhir") != null) {
            FileTerminologyProviderBuilder fileTerminologyProviderBuilder = new FileTerminologyProviderBuilder();
            this.terminologyProvider = fileTerminologyProviderBuilder.build(models, terminologyUri);
            return asDataContext(this);
        } else
            throw new IllegalArgumentException(
                    "In order to use terminology Library must use the FHIR model, Cannot find Model Info for http://hl7.org/fhir");
    }

    @Override
    public BuilderDataContext withRemoteTerminologyProvider(URL terminologyUrl)
            throws IOException, InterruptedException, URISyntaxException {
        if (models.get("http://hl7.org/fhir") != null) {
            RemoteTerminologyProviderBuilder remoteTerminologyProviderBuilder= new RemoteTerminologyProviderBuilder();
            this.terminologyProvider = remoteTerminologyProviderBuilder.build(terminologyUrl, models, this.clientFactory);
            return asDataContext(this);
        } else throw new IllegalArgumentException("In order to use terminology Library must use the FHIR model, Cannot find Model Info for http://hl7.org/fhir");
    }
    
    private BuilderDataContext asDataContext(BuilderContext thisBuilderContext) {
        BuilderDataContext dataContext = new BuilderDataContext();
        // This is a hack for now (figure out casting)
        dataContext.libraryLoader = thisBuilderContext.libraryLoader;
        dataContext.dataProviderMap = thisBuilderContext.dataProviderMap;
        dataContext.terminologyProvider = thisBuilderContext.terminologyProvider;
        dataContext.models = thisBuilderContext.models;
        dataContext.clientFactory = thisBuilderContext.clientFactory;
        dataContext.engineOptions = thisBuilderContext.engineOptions;
        dataContext.parameterDeserializer = thisBuilderContext.parameterDeserializer;
        dataContext.setTranslatorOptions(thisBuilderContext.getTranslatorOptions());
        return dataContext;
    }
}