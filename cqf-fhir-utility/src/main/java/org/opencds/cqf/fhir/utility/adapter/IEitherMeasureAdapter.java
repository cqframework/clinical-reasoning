package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.monad.Either3;

public interface IEitherMeasureAdapter {
    Either3<? extends IIdType, String, ? extends IPrimitiveType<String>> getEither();
}
