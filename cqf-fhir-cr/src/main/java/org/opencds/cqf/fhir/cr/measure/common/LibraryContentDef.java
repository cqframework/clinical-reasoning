package org.opencds.cqf.fhir.cr.measure.common;

import java.io.InputStream;
import java.nio.file.Path;
import org.opencds.cqf.fhir.utility.monad.Either;

// LUKETODO:  how should this work, exactly?
// 1. an either with the path or the binary
// 2. just the binary
// 3. just the Base64 String?
// 4. just the entire CQL String?
public class LibraryContentDef implements IDef {

    private final Either<Path, byte[]> contentEither;

    public LibraryContentDef(Either<Path, byte[]> contentEither) {
        this.contentEither = contentEither;
    }

    // LUKETODO:  implement
    public InputStream getContentAsStream() {
        return null;
    }
}
