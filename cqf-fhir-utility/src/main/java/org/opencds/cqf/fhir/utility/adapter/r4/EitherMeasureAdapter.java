package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.fhir.utility.adapter.IEitherMeasureAdapter;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class EitherMeasureAdapter implements IEitherMeasureAdapter {

    private final Either3<IdType, String, CanonicalType> either;

    public EitherMeasureAdapter(Either3<IdType, String, CanonicalType> either) {
        this.either = either;
    }

    @Override
    public Either3<? extends IIdType, String, ? extends IPrimitiveType<String>> getEither() {
        return either;
    }
}
