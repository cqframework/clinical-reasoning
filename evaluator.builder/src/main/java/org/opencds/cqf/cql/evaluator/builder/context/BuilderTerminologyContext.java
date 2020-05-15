package org.opencds.cqf.cql.evaluator.builder.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.context.api.TerminologyContext;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileTerminologyProviderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringTerminologyProviderBuilder;

import ca.uhn.fhir.context.FhirContext;

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
        stringTerminologyProviderBuilder.build();
        return asDataContext(this);
    }

    @Override
    // public BuilderDataContext withBundleTerminologyProvider(ModelInfo modelInfo,
    // IBaseBundle bundle) {
    public BuilderDataContext withBundleTerminologyProvider(FhirContext fhirContext, IBaseBundle bundle) {
        BundleTerminologyProviderBuilder bundleTerminologyProviderBuilder = new BundleTerminologyProviderBuilder();
        bundleTerminologyProviderBuilder.build(fhirContext, bundle);
        return asDataContext(this);
    }

    @Override
    public BuilderDataContext withFileTerminologyProvider(String terminologyUrl) {
        Optional<ModelInfo> fhirModelOption = this.models.stream()
                .filter(modelInfo -> modelInfo.getName().equals("http://hl7.org/fhir")).findAny();
        if (fhirModelOption.isPresent()) {
            FileTerminologyProviderBuilder fileTerminologyProviderBuilder = new FileTerminologyProviderBuilder();
            fileTerminologyProviderBuilder.build(fhirModelOption.get(), terminologyUrl);
            return asDataContext(this);
        } else throw new IllegalArgumentException("In order to use terminology Library must use the FHIR model, Cannot find Model Info for http://hl7.org/fhir");
    }

    //Should the remote uri come from a Endpoint just incase there needs to be some sort of Authentication?
    //I understand this is Fhir specific, not sure how we want to do this....
    // public BuilderContext withTerminologyProvider(Endpoint endpoint) {
    //     //someBuilder.build();
        // return asDataContext(this);
    // }
    
    private BuilderDataContext asDataContext(BuilderContext thisBuilderContext) {
        BuilderDataContext dataContext = (BuilderDataContext)thisBuilderContext;
        return dataContext;
    }
}