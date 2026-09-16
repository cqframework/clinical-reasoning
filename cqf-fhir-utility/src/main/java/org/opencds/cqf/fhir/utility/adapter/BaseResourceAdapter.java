package org.opencds.cqf.fhir.utility.adapter;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.Sets;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.Constants;

public abstract class BaseResourceAdapter extends BaseAdapter implements IResourceAdapter {
    protected final BaseRuntimeElementDefinition<?> elementDefinition;
    protected final IBaseResource resource;

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
        elementDefinition = fhirContext().getElementDefinition(this.resource.getClass());
    }

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
}
