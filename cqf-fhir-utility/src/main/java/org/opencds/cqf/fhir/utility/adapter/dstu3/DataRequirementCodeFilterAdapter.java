package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementCodeFilterAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class DataRequirementCodeFilterAdapter implements IDataRequirementCodeFilterAdapter {

    private final DataRequirementCodeFilterComponent codeFilter;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public DataRequirementCodeFilterAdapter(IBaseDatatypeElement codeFilter) {
        if (!(codeFilter instanceof DataRequirementCodeFilterComponent)) {
            throw new IllegalArgumentException(
                    "object passed as codeFilter argument is not a DataRequirementCodeFilterComponent data type");
        }
        this.codeFilter = (DataRequirementCodeFilterComponent) codeFilter;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
    }

    @Override
    public DataRequirementCodeFilterComponent get() {
        return codeFilter;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public boolean hasCode() {
        return get().hasValueCoding();
    }

    @Override
    public List<ICodingAdapter> getCode() {
        return get().getValueCoding().stream().map(CodingAdapter::new).collect(Collectors.toList());
    }

    @Override
    public boolean hasPath() {
        return get().hasPath();
    }

    @Override
    public String getPath() {
        return get().getPath();
    }

    @Override
    public boolean hasValueSet() {
        return false;
    }

    @Override
    public IPrimitiveType<String> getValueSet() {
        return null;
    }
}
