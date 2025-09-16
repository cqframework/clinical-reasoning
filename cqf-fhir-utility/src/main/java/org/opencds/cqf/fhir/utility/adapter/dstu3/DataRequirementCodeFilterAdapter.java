package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementCodeFilterAdapter;

public class DataRequirementCodeFilterAdapter extends BaseAdapter implements IDataRequirementCodeFilterAdapter {

    private final DataRequirementCodeFilterComponent codeFilter;

    public DataRequirementCodeFilterAdapter(IBase codeFilter) {
        super(FhirVersionEnum.DSTU3, codeFilter);
        if (!(codeFilter instanceof DataRequirementCodeFilterComponent)) {
            throw new IllegalArgumentException(
                    "object passed as codeFilter argument is not a DataRequirementCodeFilterComponent data type");
        }
        this.codeFilter = (DataRequirementCodeFilterComponent) codeFilter;
    }

    @Override
    public DataRequirementCodeFilterComponent get() {
        return codeFilter;
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
        return get().hasValueSet();
    }

    @Override
    public IPrimitiveType<String> getValueSet() {
        return get().getValueSetStringType();
    }
}
