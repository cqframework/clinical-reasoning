package org.opencds.cqf.fhir.cr.common;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.BundleHelper;

public interface ICqlOperationRequest extends IOperationRequest {
    IIdType getSubjectId();

    IBaseParameters getParameters();

    boolean getUseServerData();

    IBaseBundle getData();

    LibraryEngine getLibraryEngine();

    @Override
    default Repository getRepository() {
        return getLibraryEngine().getRepository();
    }

    default void resolvePrefetchData(IBaseBundle data, List<? extends IBaseBackboneElement> prefetchData) {
        var factory = getAdapterFactory();
        prefetchData.stream()
                .map(factory::createParametersParameters)
                .map(p -> p.getPartValues("data"))
                .filter(IBaseBundle.class::isInstance)
                .flatMap(b -> BundleHelper.getEntryResources((IBaseBundle) b).stream())
                .forEach(r -> BundleHelper.addEntry(data, BundleHelper.newEntryWithResource(r)));
    }
}
