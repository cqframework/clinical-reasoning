package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.List;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

/**
 * Convenience class to hold a library engine and a mapping of library IDs to measure IDs for
 * library and measure evaluation.
 */
public class MultiLibraryIdMeasureEngineDetails {
    private final LibraryEngine libraryEngine;
    private final ListMultimap<VersionedIdentifier, MeasureReportDef> libraryIdToMeasureDef;

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

    public List<MeasureReportDef> getMeasureDefsForLibrary(VersionedIdentifier libraryId) {
        return libraryIdToMeasureDef.get(libraryId);
    }

    public static Builder builder(LibraryEngine engine) {
        return new Builder(engine);
    }

    public List<MeasureReportDef> getAllMeasureDefs() {
        return List.copyOf(libraryIdToMeasureDef.values());
    }

    public static class Builder {
        private final LibraryEngine libraryEngine;
        private final ImmutableListMultimap.Builder<VersionedIdentifier, MeasureReportDef>
                libraryIdToMeasureDefBuilder = ImmutableListMultimap.builder();

        public Builder(LibraryEngine libraryEngine) {
            this.libraryEngine = libraryEngine;
        }

        public Builder addLibraryIdToMeasureId(VersionedIdentifier libraryId, MeasureReportDef measureDef) {
            libraryIdToMeasureDefBuilder.put(libraryId, measureDef);
            return this;
        }

        public MultiLibraryIdMeasureEngineDetails build() {
            return new MultiLibraryIdMeasureEngineDetails(this);
        }
    }
}
