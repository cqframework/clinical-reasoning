package org.opencds.cqf.fhir.cql;

import jakarta.annotation.Nullable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;

// LUKETODO:  find a proper home for this later
// LUKETODO:  javadoc
public class NpmResourceHolder {
    public static final NpmResourceHolder EMPTY = new NpmResourceHolder(null, null);

    @Nullable
    private final Measure measure;

    @Nullable
    private final Library mainLibrary;

    public NpmResourceHolder(@Nullable Measure measure, @Nullable Library mainLibrary) {
        this.measure = measure;
        this.mainLibrary = mainLibrary;
    }

    public Optional<Measure> getMeasure() {
        return Optional.ofNullable(measure);
    }

    public Optional<Library> getOptMainLibrary() {
        return Optional.ofNullable(mainLibrary);
    }
}
