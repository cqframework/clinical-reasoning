package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.model.api.IQueryParameterType;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import org.hl7.elm.r1.VersionedIdentifier;

public interface IDefRepo {
    <T extends IDef> Optional<T> loadDefByUrl(Class<T> defClass, String canonicalUrl);

    Optional<LibraryDef> loadLibraryDef(VersionedIdentifier libraryId);

    Optional<MeasureDef> loadMeasureDefByUrl(String canonicalUrl);
    Optional<MeasureDef> loadMeasureDefById(String measureId);

    <T extends IDef> List<T> searchDefs(Class<T> resourceType, Multimap<String, List<IQueryParameterType>> searchParameters);
}
