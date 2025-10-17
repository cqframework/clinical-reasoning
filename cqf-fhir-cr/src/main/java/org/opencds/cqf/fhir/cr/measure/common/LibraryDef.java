package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.elm.r1.VersionedIdentifier;

public class LibraryDef {
    private final VersionedIdentifier libraryId;

    public LibraryDef(VersionedIdentifier libraryId) {
        this.libraryId = libraryId;
    }

    public VersionedIdentifier getLibraryId() {
        return libraryId;
    }
}
