package org.opencds.cqf.fhir.cr.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.BundleHelper;

/**
 * This interface exposes common functionality across Operations that use CQL evaluation
 */
public interface ICqlOperationRequest extends IOperationRequest {
    IBase getContextVariable();

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
    default Repository getRepository() {
        return getLibraryEngine().getRepository();
    }

    default void resolvePrefetchData(IBaseBundle data, List<? extends IBaseBackboneElement> prefetchData) {
        var factory = getAdapterFactory();
        prefetchData.stream()
                .map(factory::createParametersParameter)
                .map(p -> p.getPartValues("data"))
                .filter(IBaseBundle.class::isInstance)
                .flatMap(b -> BundleHelper.getEntryResources((IBaseBundle) b).stream())
                .forEach(r -> BundleHelper.addEntry(data, BundleHelper.newEntryWithResource(r)));
    }
}
