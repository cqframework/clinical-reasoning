package org.opencds.cqf.cql.evaluator.cql2elm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.evaluator.fhir.api.LibraryAdapter;

import static org.opencds.cqf.cql.evaluator.fhir.common.AdapterFactory.libraryAdapterFor;

public abstract class VersionComparingLibrarySourceProvider
        implements LibrarySourceProvider {

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        Objects.requireNonNull(versionedIdentifier, "versionedIdentifier can not be null.");

        IBaseResource library = this.getLibrary(versionedIdentifier);
        if (library == null) {
            return null;
        }

        return this.getCqlStream(library);
    }

    private InputStream getCqlStream(IBaseResource library) {

        LibraryAdapter libraryAdapter = libraryAdapterFor(library);
        
        if (libraryAdapter.hasContent(library)) {
            for (ICompositeType attachment : libraryAdapter.getContent(library)) {
                if (libraryAdapter.getContentType(attachment).equals("text/cql")) {
                    return new ByteArrayInputStream(libraryAdapter.getData(attachment));
                }
            }
        }

        return null;
    }

    protected abstract IBaseResource getLibrary(VersionedIdentifier libraryIdentifier);

    protected IBaseResource select(Collection<IBaseResource> libraries, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(libraries, "libraries can not be null");
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");

        String targetVersion = libraryIdentifier.getVersion();

        IBaseResource library = null;
        IBaseResource maxLibrary = null;

        for (IBaseResource l : libraries) {

            LibraryAdapter libraryAdapter = libraryAdapterFor(library);

            String currentVersion = libraryAdapter.getVersion(l);
            if ((targetVersion != null && currentVersion != null && currentVersion.equals(targetVersion)) || 
                (targetVersion == null && currentVersion == null)) {
                library = l;
            }

            if (maxLibrary == null || compareVersions(libraryAdapter.getVersion(maxLibrary), currentVersion) < 0) {
                maxLibrary = l;
            }
        }

        if (targetVersion == null && maxLibrary != null) {
            library = maxLibrary;
        }

        return library;
    }

    public static int compareVersions(String version1, String version2) {
        // Treat null as MAX VERSION
        if (version1 == null && version2 == null) {
            return 0;
        }

        if (version1 != null && version2 == null) {
            return -1;
        }

        if (version1 == null && version2 != null) {
            return 1;
        }

        String[] string1Vals = version1.split("\\.");
        String[] string2Vals = version2.split("\\.");

        int length = Math.max(string1Vals.length, string2Vals.length);

        for (int i = 0; i < length; i++) {
            Integer v1 = (i < string1Vals.length) ? Integer.parseInt(string1Vals[i]) : 0;
            Integer v2 = (i < string2Vals.length) ? Integer.parseInt(string2Vals[i]) : 0;

            // Making sure Version1 bigger than version2
            if (v1 > v2) {
                return 1;
            }
            // Making sure Version1 smaller than version2
            else if (v1 < v2) {
                return -1;
            }
        }

        // Both are equal
        return 0;
    }
}