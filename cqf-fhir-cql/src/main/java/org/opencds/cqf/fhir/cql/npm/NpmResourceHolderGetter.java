package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CanonicalType;

// LUKETODO:  javadoc
// LUKETODO:  better name?
public interface NpmResourceHolderGetter {

    NpmResourceHolderGetter DEFAULT = new NpmResourceHolderGetter() {};

    // LUKETODO:  unit test this:
    default NpmResourceHolder loadNpmResources(CanonicalType measureUrl) {
        return NpmResourceHolder.EMPTY;
    }

    /**
     * Hackish:  Either the downstream app injected this or we default to a NO-OP implementation.
     *
     * @param npmResourceHolderGetter The NpmResourceHolderGetter, if injected by the downstream app, otherwise null.
     * @return Either the downstream app's NpmResourceHolderGetter or a no-op implementation.
     */
    static NpmResourceHolderGetter getDefaultIfEmpty(@Nullable NpmResourceHolderGetter npmResourceHolderGetter) {
        return Optional.ofNullable(npmResourceHolderGetter).orElse(NpmResourceHolderGetter.DEFAULT);
    }
}
