package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementCodeFilterAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class DataRequirementAdapter implements IDataRequirementAdapter {

    private final DataRequirement dataRequirement;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public DataRequirementAdapter(ICompositeType dataRequirement) {
        if (!(dataRequirement instanceof DataRequirement)) {
            throw new IllegalArgumentException(
                    "object passed as dataRequirement argument is not a DataRequirement data type");
        }
        this.dataRequirement = (DataRequirement) dataRequirement;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
    }

    @Override
    public DataRequirement get() {
        return dataRequirement;
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
    public boolean hasType() {
        return get().hasType();
    }

    @Override
    public String getType() {
        return get().getType();
    }

    @Override
    public boolean hasCodeFilter() {
        return get().hasCodeFilter();
    }

    @Override
    public List<IDataRequirementCodeFilterAdapter> getCodeFilter() {
        return get().getCodeFilter().stream()
                .map(DataRequirementCodeFilterAdapter::new)
                .collect(Collectors.toList());
    }
}
