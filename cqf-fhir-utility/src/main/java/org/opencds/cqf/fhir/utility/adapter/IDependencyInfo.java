package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseExtension;

/**
 * Represents a dependency on another FHIR artifact.
 * <p>
 * The {@code reference} property is intended to be a FHIR canonical reference (i.e., {@code url} or {@code url|version})
 * as defined by the FHIR specification. Implementations are responsible for resolving any non-canonical references
 * to canonical form before returning them via this interface.
 */
public interface IDependencyInfo {
    String getReferenceSource();

    void setReferenceSource(String referenceSource);

    /**
     * Returns the canonical reference string (url or url|version) for the dependency.
     * This must not be a relative reference or a plain id.
     *
     * @return the canonical reference string for the dependency
     */
    String getReference();

    /**
     * Sets the canonical reference string (url or url|version) for the dependency.
     * Callers must supply a canonical reference string; implementations may normalize or validate the reference.
     *
     * @param reference the canonical reference string for the dependency
     */
    void setReference(String reference);

    String getReferencePackageId();

    void setReferencePackageId(String referencePackageId);

    <E extends IBaseExtension<?, ?>> List<E> getExtension();
}
