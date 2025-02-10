package org.opencds.cqf.fhir.cql;

import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;

import java.util.List;
import java.util.Optional;

// LUKETODO:  find a proper home for this later
public class NpmResourceHolder {
    public static final NpmResourceHolder EMPTY = new NpmResourceHolder(null, List.of());

    public static NpmResourceHolder noLibraries(Measure measure) {
        return new NpmResourceHolder(measure, List.of());
    }

    public static NpmResourceHolder of(Measure measure, Library... libraries) {
        return new NpmResourceHolder(measure, List.of(libraries));
    }

    @Nullable
    private Measure measure;
    private List<Library> libraries;

    private NpmResourceHolder(@Nullable Measure theMeasure, List<Library> theLibraries) {
        measure = theMeasure;
        libraries = theLibraries;
    }

    public Optional<Measure> getMeasure() {
        return Optional.ofNullable(measure);
    }

    public List<Library> getLibraries() {
        return libraries;
    }
}
