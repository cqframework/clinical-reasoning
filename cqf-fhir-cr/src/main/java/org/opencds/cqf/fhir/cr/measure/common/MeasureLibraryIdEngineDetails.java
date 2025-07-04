package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cql.LibraryEngine;

public record MeasureLibraryIdEngineDetails(IIdType measureId, VersionedIdentifier libraryId, LibraryEngine engine) {}
