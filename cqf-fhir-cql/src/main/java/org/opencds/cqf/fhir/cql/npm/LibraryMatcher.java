package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;

class LibraryMatcher {

    private static final String TEXT_CQL = "text/cql";

    static boolean doesLibraryMatch(String id, @Nullable Library libraryCandidate) {
        if (libraryCandidate == null) {
            return false;
        }

        if (libraryCandidate.getIdPart().equals(id)) {
            final Optional<Attachment> optCqlData = libraryCandidate.getContent().stream()
                    .filter(content -> TEXT_CQL.equals(content.getContentType()))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }
}
