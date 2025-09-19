package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseExtension;

public interface IDependencyInfo {
    String getReferenceSource();

    void setReferenceSource(String referenceSource);

    String getReference();

    void setReference(String reference);

    String getReferencePackageId();

    void setReferencePackageId(String referencePackageId);

    <E extends IBaseExtension<?, ?>> List<E> getExtension();
}
