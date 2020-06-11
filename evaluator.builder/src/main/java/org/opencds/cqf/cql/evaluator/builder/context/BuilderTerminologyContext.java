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
 *    1. A pre-constructed TerminologyProvider
 *    2. String representations of Terminology Resources
 *    3. A remote terminology repository
 *    4. A filesystem with terminology content
 *    5. A Bundle containing FHIR Terminology
 */
public class BuilderTerminologyContext extends BuilderContext implements TerminologyContext {

    /**
     * set TerminologyProvider with Preconfigured TerminologyProvider used to execute
     * 
     * @param terminologyProvider preconfigured TerminologyProvider
     * @return BuilderDataContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderDataContext withTerminologyProvider(TerminologyProvider terminologyProvider) {
        this.terminologyProvider = terminologyProvider;
        return asDataContext(this);
    }

    /**
     * set TerminologyProvider using a list of String representations of any Terminology required for execution
     * 
     * @param terminologyBundles String representations of Terminology
     * @return BuilderDataContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderDataContext withStringTerminologyProvider(List<String> terminologyBundles) {
        StringTerminologyProviderBuilder stringTerminologyProviderBuilder = new StringTerminologyProviderBuilder();
        this.terminologyProvider = stringTerminologyProviderBuilder.build(this.models, terminologyBundles);
        return asDataContext(this);
    }

    /**
     * set TerminologyProvider using a FHIR Bundle containing any Terminology required for execution
     * 
     * @param bundle Bundle containing Terminology
     * @return BuilderDataContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderDataContext withBundleTerminologyProvider(IBaseBundle bundle) {
        if (models.get("http://hl7.org/fhir") != null) {
            BundleTerminologyProviderBuilder bundleTerminologyProviderBuilder = new BundleTerminologyProviderBuilder();
            this.terminologyProvider = bundleTerminologyProviderBuilder.build(models, bundle);
            return asDataContext(this);
        } else
            throw new IllegalArgumentException(
                    "In order to use terminology Library must use the FHIR model, Cannot find Model Info for http://hl7.org/fhir");
    }

    /**
     * set TerminologyProvider using a URI for any Terminology required for execution
     * 
     * @param terminologyUri containing Terminology
     * @return BuilderDataContext a new instance with the appropriate context filled out.
     */
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

    /**
     * set TerminologyProvider using a URL pointing to Remote repository containing Terminology needed for execution
     * If now ClientFactory is provided a DefaultClientFactory will be used.
     * Must be a URL of a HAPI FHIR Client as of now.
     * 
     * @param terminologyUrl needed for execution
     * @return BuilderTerminologyContext a new instance with the appropriate context filled out.
     */
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