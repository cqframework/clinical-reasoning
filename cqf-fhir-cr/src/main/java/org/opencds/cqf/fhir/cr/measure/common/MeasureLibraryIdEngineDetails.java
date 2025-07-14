package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cql.LibraryEngine;

// LUKETODO: get rid of this once the new code is firmly in place
public record MeasureLibraryIdEngineDetails(IIdType measureId, VersionedIdentifier libraryId, LibraryEngine engine) {}
