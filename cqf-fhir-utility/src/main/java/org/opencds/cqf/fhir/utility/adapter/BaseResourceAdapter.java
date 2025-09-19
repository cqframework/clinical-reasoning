package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.Sets;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public abstract class BaseResourceAdapter implements IResourceAdapter {
    protected final FhirContext fhirContext;
    protected final BaseRuntimeElementDefinition<?> elementDefinition;
    protected final IBaseResource resource;
    protected final ModelResolver modelResolver;
    protected final IAdapterFactory adapterFactory;

    protected static final Set<String> LIBRARY_TYPES =
            Sets.newHashSet("logic-library", "model-definition", "asset-collection", "module-definition");

    protected static final Set<String> REFERENCE_EXTENSIONS = Sets.newHashSet(
            Constants.QUESTIONNAIRE_UNIT_VALUE_SET,
            Constants.QUESTIONNAIRE_REFERENCE_PROFILE,
            Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE,
            Constants.SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE,
            Constants.CQFM_INPUT_PARAMETERS,
            Constants.CQF_EXPANSION_PARAMETERS,
            Constants.CQF_CQL_OPTIONS);

    protected static final Set<String> EXPRESSION_EXTENSIONS = Sets.newHashSet(
            Constants.VARIABLE_EXTENSION,
            Constants.SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION,
            Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION,
            Constants.SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION,
            Constants.CQF_EXPRESSION);

    protected static final Set<String> CANONICAL_EXTENSIONS =
            Sets.newHashSet(Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS, Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS);

    protected BaseResourceAdapter(IBaseResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource can not be null");
        }
        this.resource = resource;
        fhirContext = FhirContext.forCached(resource.getStructureFhirVersionEnum());
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        elementDefinition = fhirContext.getElementDefinition(this.resource.getClass());
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    public FhirContext fhirContext() {
        return fhirContext;
    }

    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    public IBaseResource get() {
        return resource;
    }

    public IAdapterFactory getAdapterFactory() {
        return adapterFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends IBaseExtension<?, ?>> E addExtension() {
        if (get() instanceof IBaseHasExtensions baseHasExtensions) {
            return (E) baseHasExtensions.addExtension();
        }
        return null;
    }
}
