package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.elm.r1.VersionedIdentifier;

public class LibraryDef implements IDef {
    private final VersionedIdentifier libraryId;
    private final LibraryContentDef content;

    public LibraryDef(VersionedIdentifier libraryId, LibraryContentDef content) {
        this.libraryId = libraryId;
        this.content = content;
    }

    public VersionedIdentifier getLibraryId() {
        return libraryId;
    }

    public LibraryContentDef getContent() {
        return content;
    }
}
