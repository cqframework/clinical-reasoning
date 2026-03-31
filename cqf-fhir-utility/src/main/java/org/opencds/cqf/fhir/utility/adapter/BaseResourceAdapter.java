package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.FhirTerser;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Constants;

import static java.util.Objects.requireNonNull;

public abstract class BaseResourceAdapter extends AdapterBase implements IResourceAdapter {
//    protected final FhirContext fhirContext;
//    protected final FhirTerser fhirTerser;
    protected final BaseRuntimeElementDefinition<?> elementDefinition;
    protected final IBaseResource resource;
//    protected final IAdapterFactory adapterFactory;

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
        super(FhirContext.forCached(requireNonNull(resource.getStructureFhirVersionEnum())));
        this.resource = resource;
//        fhirContext = FhirContext.forCached(resource.getStructureFhirVersionEnum());
//        fhirTerser = new FhirTerser(fhirContext);
//        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        elementDefinition = fhirContext().getElementDefinition(this.resource.getClass());
    }

//    public FhirContext fhirContext() {
//        return fhirContext;
//    }

    public IBaseResource get() {
        return resource;
    }

    @Override
    public void setId(IIdType id) {
        setValue(get(), "id", id);
    }

    @Override
    public void setValue(String path, Object value) {
        setValue(get(), path, value);
    }

    //    public IAdapterFactory getAdapterFactory() {
//        return adapterFactory;
//    }

//    @SuppressWarnings("unchecked")
//    @Override
//    public <E extends IBaseExtension<?, ?>> E addExtension() {
//        if (get() instanceof IBaseHasExtensions baseHasExtensions) {
//            return (E) baseHasExtensions.addExtension();
//        }
//        return null;
//    }
}
