package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;

// LUKETODO:  javadoc
// LUKETODO:  better name?
// LUKETODO:  should we try to account for other FHIR versions?  should these be FHIR version specific?
public interface R4NpmPackageLoader {

    R4NpmPackageLoader DEFAULT = new R4NpmPackageLoader() {};

    // LUKETODO:  unit test this:
    default R4NpmResourceInfoForCql loadNpmResources(CanonicalType measureUrl) {
        return R4NpmResourceInfoForCql.EMPTY;
    }

    // LUKETODO:  think about this API:  are we getting a Library or an InputStream?
    default Optional<Library> loadLibrary(String url) {
        return Optional.empty();
    }

    /**
     * Hackish:  Either the downstream app injected this or we default to a NO-OP implementation.
     *
     * @param r4NpmPackageLoader The NpmResourceHolderGetter, if injected by the downstream app, otherwise null.
     * @return Either the downstream app's NpmResourceHolderGetter or a no-op implementation.
     */
    static R4NpmPackageLoader getDefaultIfEmpty(@Nullable R4NpmPackageLoader r4NpmPackageLoader) {
        return Optional.ofNullable(r4NpmPackageLoader).orElse(R4NpmPackageLoader.DEFAULT);
    }
}
