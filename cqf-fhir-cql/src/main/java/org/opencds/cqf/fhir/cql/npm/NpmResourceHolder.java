package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.utilities.npm.NpmPackage;

// LUKETODO:  find a proper home for this later
// LUKETODO:  javadoc
public class NpmResourceHolder {
    public static final NpmResourceHolder EMPTY = new NpmResourceHolder(null, null, List.of());

    @Nullable
    private final Measure measure;

    @Nullable
    private final Library mainLibrary;

    private final List<NpmPackage> npmPackages;

    public NpmResourceHolder(@Nullable Measure measure, @Nullable Library mainLibrary, List<NpmPackage> npmPackages) {
        this.measure = measure;
        this.mainLibrary = mainLibrary;
        this.npmPackages = npmPackages;
    }

    public Optional<Measure> getMeasure() {
        return Optional.ofNullable(measure);
    }

    public Optional<Library> getOptMainLibrary() {
        return Optional.ofNullable(mainLibrary);
    }

    public boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier) {
        return doesLibraryMatch(versionedIdentifier.getId());
    }

    public boolean doesLibraryMatch(ModelIdentifier modelIdentifier) {
        return doesLibraryMatch(modelIdentifier.getId());
    }

    public String toUrl(VersionedIdentifier versionedIdentifier) {
        return null;
    }

    public String toUrl(ModelIdentifier modelIdentifier) {
        return null;
    }

    public List<NamespaceInfo> getNamespaceInfos() {
        return npmPackages.stream().map(this::getNamespaceInfo).toList();
    }

    @Nonnull
    private NamespaceInfo getNamespaceInfo(NpmPackage npmPackage) {
        // LUKETODO:  do we get a
        return new NamespaceInfo(npmPackage.name(), npmPackage.canonical());
    }

    private static final String TEXT_CQL = "text/cql";

    boolean doesLibraryMatch(String id) {
        if (mainLibrary == null) {
            return false;
        }

        if (mainLibrary.getIdPart().equals(id)) {
            final Optional<Attachment> optCqlData = mainLibrary.getContent().stream()
                    .filter(content -> TEXT_CQL.equals(content.getContentType()))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }
}
