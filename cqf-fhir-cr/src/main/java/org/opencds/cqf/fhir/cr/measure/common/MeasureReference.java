package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * A typed reference to a Measure resource. Replaces the {@code Either3<IdType, String, CanonicalType>}
 * pattern and the three-separate-lists pattern with a single self-documenting sealed hierarchy.
 *
 * <p>HAPI {@code @OperationParam} delivers measure references as three separate lists (by ID,
 * by identifier, by canonical URL). The transport adapter converts these into a single
 * {@code List<MeasureReference>} at the boundary, and all downstream code works with this type.
 */
public sealed interface MeasureReference {

    /** A measure referenced by its FHIR resource ID (e.g. {@code Measure/example-1}). */
    record ById(IIdType id) implements MeasureReference {
        public ById {
            Objects.requireNonNull(id, "Measure ID must not be null");
        }

        @Override
        public String display() {
            return id.getValue();
        }
    }

    /** A measure referenced by its business identifier (e.g. {@code "CMS125"}). */
    record ByIdentifier(String identifier) implements MeasureReference {
        public ByIdentifier {
            Objects.requireNonNull(identifier, "Measure identifier must not be null");
        }

        @Override
        public String display() {
            return identifier;
        }
    }

    /** A measure referenced by its canonical URL (e.g. {@code "http://example.org/Measure/CMS125"}). */
    record ByCanonicalUrl(String url) implements MeasureReference {
        public ByCanonicalUrl {
            Objects.requireNonNull(url, "Measure canonical URL must not be null");
        }

        @Override
        public String display() {
            return url;
        }
    }

    /** Human-readable display string for error messages and logging. */
    String display();

    /**
     * Converts three HAPI {@code @OperationParam} lists into a single {@code List<MeasureReference>}.
     * This is the boundary conversion — called once in the transport adapter.
     *
     * @param measureIds         measure resource IDs (may be null)
     * @param measureIdentifiers measure business identifiers (may be null)
     * @param measureUrls        measure canonical URLs (may be null)
     * @return combined list preserving input order (IDs first, then identifiers, then URLs)
     */
    static List<MeasureReference> fromOperationParams(
            List<? extends IIdType> measureIds, List<String> measureIdentifiers, List<String> measureUrls) {
        var refs = new ArrayList<MeasureReference>();
        if (measureIds != null) {
            measureIds.stream().filter(Objects::nonNull).forEach(id -> refs.add(new ById(id)));
        }
        if (measureIdentifiers != null) {
            measureIdentifiers.stream().filter(Objects::nonNull).forEach(id -> refs.add(new ByIdentifier(id)));
        }
        if (measureUrls != null) {
            measureUrls.stream().filter(Objects::nonNull).forEach(url -> refs.add(new ByCanonicalUrl(url)));
        }
        return Collections.unmodifiableList(refs);
    }
}
