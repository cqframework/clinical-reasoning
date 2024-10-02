package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;

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
}
