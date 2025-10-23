package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementCodeFilterAdapter;

public class DataRequirementAdapter extends BaseAdapter implements IDataRequirementAdapter {

    private final DataRequirement dataRequirement;

    public DataRequirementAdapter(IBase compositeType) {
        super(FhirVersionEnum.DSTU3, compositeType);
        if (!(compositeType instanceof DataRequirement dataRequirementInner)) {
            throw new IllegalArgumentException(
                    "object passed as dataRequirement argument is not a DataRequirement data type");
        }
        this.dataRequirement = dataRequirementInner;
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
    public boolean hasId() {
        return get().hasId();
    }

    @Override
    public String getId() {
        return get().getId();
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
    public boolean hasProfile() {
        return get().hasProfile();
    }

    @Override
    public List<IPrimitiveType<String>> getProfile() {
        return get().getProfile().stream().map(p -> (IPrimitiveType<String>) p).toList();
    }

    @Override
    public boolean hasCodeFilter() {
        return get().hasCodeFilter();
    }

    @Override
    public List<IDataRequirementCodeFilterAdapter> getCodeFilter() {
        return get().getCodeFilter().stream()
                .map(DataRequirementCodeFilterAdapter::new)
                .collect(Collectors.toUnmodifiableList());
    }
}
