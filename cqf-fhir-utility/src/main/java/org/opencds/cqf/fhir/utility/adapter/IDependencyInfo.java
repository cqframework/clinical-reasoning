package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseExtension;

public interface IDependencyInfo {
    public String getReferenceSource();

    public void setReferenceSource(String referenceSource);

    public String getReference();

    public void setReference(String reference);

    public String getReferencePackageId();

    public void setReferencePackageId(String referencePackageId);

    public List<? extends IBaseExtension<?, ?>> getExtension();
}
