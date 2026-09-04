package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Objects;
import org.opencds.cqf.cql.engine.runtime.Value;

/**
 * The key a value is stored under in {@link HashSetForFhirResourcesAndCqlTypes}, carrying the
 * identity {@link FhirResourceAndCqlTypeUtils} defines for it.
 * <p/>
 * Keying is what lets one relation answer every set operation. Comparing elements pairwise instead
 * - which is what these collections used to do - lets {@code add} and {@code contains} disagree,
 * because they reached for different notions of equality, and costs a linear scan per operation.
 */
sealed interface IdentityKey {

    static IdentityKey of(Object value) {
        var resourceIdentity = FhirResourceAndCqlTypeUtils.resourceIdentity(value);
        if (resourceIdentity != null) {
            return new ResourceKey(resourceIdentity);
        }
        if (value instanceof Value cqlValue) {
            return new CqlValueKey(cqlValue);
        }
        return new PlainKey(value);
    }

    /**
     * A FHIR resource, keyed by resource type and logical id. This is the case that matters: it
     * turns a deep structural walk of a resource graph into a string comparison.
     */
    record ResourceKey(String identity) implements IdentityKey {}

    /**
     * A CQL value that is not a FHIR resource, compared with CQL {@code =}.
     * <p/>
     * CQL {@code =} is not hash-compatible - {@code 1.0 = 1.00} is true where the two have different
     * structural hashes, and DateTime comparison across precisions can be uncertain rather than
     * false - so there is no key to spread these across buckets. They all share one, which makes
     * lookups among them linear, exactly as before. There are few: resources take the keyed path
     * above, and these are the {@code Date}s and {@code Decimal}s a stratifier returns.
     */
    record CqlValueKey(Value value) implements IdentityKey {

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || (other instanceof CqlValueKey otherKey
                            && FhirResourceAndCqlTypeUtils.areEqualCqlTypes(value, otherKey.value));
        }
    }

    /** Anything else - a String, a Map of criteria results - on its own {@code equals}/{@code hashCode}. */
    record PlainKey(Object value) implements IdentityKey {

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other instanceof PlainKey otherKey && Objects.equals(value, otherKey.value));
        }
    }
}
