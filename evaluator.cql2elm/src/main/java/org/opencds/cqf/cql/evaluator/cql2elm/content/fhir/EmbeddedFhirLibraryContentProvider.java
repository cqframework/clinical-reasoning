package org.opencds.cqf.cql.evaluator.cql2elm.content.fhir;

import java.io.InputStream;

import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;

/**
 * This class provides access to the FHIR Libraries that are embedded in the translator jars,
 * specifically FHIRHelpers. Typically you'd include an instance of this library in the list
 * of content providers used for translation if you needed to translate or load CQL content
 * that depended on FHIRHelpers. This would not be needed if you had another source for the
 * FHIRHelpers content configured (e.g. a server with FHIRHelpers loaded, an IG with FHIRHelpers
 * defined, etc)
 */
public class EmbeddedFhirLibraryContentProvider extends FhirLibrarySourceProvider implements LibraryContentProvider {

    @Override
    public InputStream getLibraryContent(VersionedIdentifier libraryIdentifier, LibraryContentType libraryContentType) {
        if (libraryContentType != LibraryContentType.CQL) {
            return null;
        }

        return getLibrarySource(libraryIdentifier);
    }
}
