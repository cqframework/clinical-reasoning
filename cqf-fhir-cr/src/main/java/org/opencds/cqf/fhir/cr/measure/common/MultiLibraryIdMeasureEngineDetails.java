package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.List;
import java.util.Objects;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.fhir.cql.LibraryEngine;

/**
 * Convenience class to hold a library engine and a mapping of library IDs to measure IDs for
 * library and measure evaluation.
 */
public class MultiLibraryIdMeasureEngineDetails {
    private final LibraryEngine libraryEngine;
    private final ListMultimap<VersionedIdentifier, MeasureDef> libraryIdToMeasureDef;

    private MultiLibraryIdMeasureEngineDetails(Builder builder) {
        this.libraryEngine = builder.libraryEngine;
        this.libraryIdToMeasureDef = builder.libraryIdToMeasureDefBuilder.build();
    }

    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    public List<VersionedIdentifier> getLibraryIdentifiers() {
        // Assuming we want the first library identifier
        return List.copyOf(libraryIdToMeasureDef.keySet());
    }

    public List<? extends IIdType> getMeasureIdsForLibrary(VersionedIdentifier libraryId) {
        return getMeasureDefsForLibrary(libraryId).stream()
                .filter(Objects::nonNull)
                .map(MeasureDef::id)
                .map(IdType::new)
                .toList();
    }

    public List<MeasureDef> getMeasureDefsForLibrary(VersionedIdentifier libraryId) {
        return libraryIdToMeasureDef.get(libraryId);
    }

    public static Builder builder(LibraryEngine engine) {
        return new Builder(engine);
    }

    public List<MeasureDef> getAllMeasureIds() {
        return List.copyOf(libraryIdToMeasureDef.values());
    }

    public static class Builder {
        private final LibraryEngine libraryEngine;
        private final ImmutableListMultimap.Builder<VersionedIdentifier, MeasureDef> libraryIdToMeasureDefBuilder =
                ImmutableListMultimap.builder();

        public Builder(LibraryEngine libraryEngine) {
            this.libraryEngine = libraryEngine;
        }

        public Builder addLibraryIdToMeasureId(VersionedIdentifier libraryId, MeasureDef measureDef) {
            libraryIdToMeasureDefBuilder.put(libraryId, measureDef);
            return this;
        }

        public MultiLibraryIdMeasureEngineDetails build() {
            return new MultiLibraryIdMeasureEngineDetails(this);
        }
    }
}
