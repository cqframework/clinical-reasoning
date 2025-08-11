package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.List;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cql.LibraryEngine;

/**
 * Convenience class to hold a library engine and a mapping of library IDs to measure IDs for
 * library and measure evaluation.
 */
public class MultiLibraryIdMeasureEngineDetails {
    private final LibraryEngine libraryEngine;
    private final ListMultimap<VersionedIdentifier, IIdType> libraryIdToMeasureIds;

    private MultiLibraryIdMeasureEngineDetails(Builder builder) {
        this.libraryEngine = builder.libraryEngine;
        this.libraryIdToMeasureIds = builder.libraryIdToMeasureIdsBuilder.build();
    }

    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    public List<VersionedIdentifier> getLibraryIdentifiers() {
        // Assuming we want the first library identifier
        return List.copyOf(libraryIdToMeasureIds.keySet());
    }

    public List<IIdType> getMeasureIdsForLibrary(VersionedIdentifier libraryId) {
        return libraryIdToMeasureIds.get(libraryId);
    }

    public static Builder builder(LibraryEngine engine) {
        return new Builder(engine);
    }

    public List<IIdType> getAllMeasureIds() {
        return List.copyOf(libraryIdToMeasureIds.values());
    }

    public static class Builder {
        private final LibraryEngine libraryEngine;
        private final ImmutableListMultimap.Builder<VersionedIdentifier, IIdType> libraryIdToMeasureIdsBuilder =
                ImmutableListMultimap.builder();

        public Builder(LibraryEngine libraryEngine) {
            this.libraryEngine = libraryEngine;
        }

        public Builder addLibraryIdToMeasureId(VersionedIdentifier libraryId, IIdType measureId) {
            libraryIdToMeasureIdsBuilder.put(libraryId, measureId);
            return this;
        }

        public MultiLibraryIdMeasureEngineDetails build() {
            return new MultiLibraryIdMeasureEngineDetails(this);
        }
    }
}
