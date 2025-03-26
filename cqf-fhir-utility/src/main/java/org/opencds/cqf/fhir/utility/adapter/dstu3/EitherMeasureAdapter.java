package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.model.primitive.UriDt;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.IdType;
import org.opencds.cqf.fhir.utility.adapter.IEitherMeasureAdapter;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class EitherMeasureAdapter implements IEitherMeasureAdapter {

    private final Either3<IdType, String, Reference> either;

    public EitherMeasureAdapter(Either3<IdType, String, Reference> either) {
        this.either = either;
    }

    @Override
    public Either3<? extends IIdType, String, ? extends IPrimitiveType<String>> getEither() {
        return null;
    }
}
