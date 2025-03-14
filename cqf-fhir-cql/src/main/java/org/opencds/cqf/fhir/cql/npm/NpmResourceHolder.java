package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;

// LUKETODO:  find a proper home for this later
// LUKETODO:  javadoc
public class NpmResourceHolder {
    public static final NpmResourceHolder EMPTY = new NpmResourceHolder(null, null, List.of());

    @Nullable
    private final Measure measure;

    @Nullable
    private final Library mainLibrary;

    private final List<NamespaceInfo> namespaceInfos;

    public NpmResourceHolder(
            @Nullable Measure measure, @Nullable Library mainLibrary, List<NamespaceInfo> namespaceInfos) {
        this.measure = measure;
        this.mainLibrary = mainLibrary;
        this.namespaceInfos = namespaceInfos;
    }

    public Optional<Measure> getMeasure() {
        return Optional.ofNullable(measure);
    }

    public Optional<Library> getOptMainLibrary() {
        return Optional.ofNullable(mainLibrary);
    }

    public List<NamespaceInfo> getNamespaceInfos() {
        return namespaceInfos;
    }
}
