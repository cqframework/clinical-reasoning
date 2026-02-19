package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.repository.IRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.BundleHelper;

/**
 * This interface exposes common functionality across Operations that use CQL evaluation
 */
@SuppressWarnings("UnstableApiUsage")
public interface ICqlOperationRequest extends IOperationRequest {

    /**
     * Returns the object to be used as the %context variable for FHIRPath evaluation.  Is expected to be null when not supporting FHIRPath.
     * @return IBase
     */
    IBase getContextVariable();

    /**
     * Returns the object to be used as the %resource variable for FHIRPath evaluation.  Is expected to be null when not supporting FHIRPath.
     * @return IBase
     */
    default IBase getResourceVariable() {
        return null;
    }

    IIdType getSubjectId();

    IBaseParameters getParameters();

    default Map<String, Object> getRawParameters() {
        return new HashMap<>();
    }

    IBaseBundle getData();

    LibraryEngine getLibraryEngine();

    @Override
    default IRepository getRepository() {
        return getLibraryEngine().getRepository();
    }

    default void resolvePrefetchData(IBaseBundle data, List<? extends IBaseBackboneElement> prefetchData) {
        var factory = getAdapterFactory();
        prefetchData.stream()
                .map(factory::createParametersParameter)
                .flatMap(p -> p.getPartValues("data").stream())
                .filter(IBaseBundle.class::isInstance)
                .flatMap(b -> BundleHelper.getEntryResources((IBaseBundle) b).stream())
                .forEach(r -> BundleHelper.addEntry(data, BundleHelper.newEntryWithResource(r)));
    }
}
