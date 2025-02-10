package org.opencds.cqf.fhir.cql;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.cqframework.fhir.npm.NpmPackageManager;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.utilities.npm.NpmPackage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// LUKETODO:  find a proper home for this later
public class NpmProcessorOptimized {
    private final FhirVersionHolder fhirVersionHolder;
    private final Map<String, NpmPackage> npmPackageByName;
    private final List<NamespaceInfo> namespaceInfos;

    public static final NpmProcessorOptimized EMPTY  = new NpmProcessorOptimized(FhirVersionHolder.EMPTY, Map.of(), List.of());

    public static NpmProcessorOptimized fromIgContext(@Nullable IGContext igContext) {
        if (igContext == null) {
            return EMPTY;
        }

        final NpmPackageManager npmPackageManager = new NpmPackageManager(igContext.getSourceIg());

        return new NpmProcessorOptimized(
             FhirVersionHolder.fromVersionString(igContext.getFhirVersion()),
            getPackagesByName(npmPackageManager),
            getNamespaceInfos(npmPackageManager)
        );
    }

    public static NpmProcessorOptimized fromParams(FhirVersionEnum fhirVersionEnum, List<NpmPackage> npmPackages, List<NamespaceInfo> namespaceInfos) {
        return new NpmProcessorOptimized(
            FhirVersionHolder.fromEnum(fhirVersionEnum),
            getPackagesByName(npmPackages),
            namespaceInfos
        );
    }

    public static NpmProcessorOptimized copy(NpmProcessorOptimized other) {
        return new NpmProcessorOptimized(
            other.fhirVersionHolder,
            other.npmPackageByName,
            other.namespaceInfos
        );
    }

    public NpmProcessorOptimized copy() {
        return new NpmProcessorOptimized(
                this.fhirVersionHolder,
                this.npmPackageByName,
                this.namespaceInfos
        );
    }

    private static Map<String, NpmPackage> getPackagesByName(@Nonnull NpmPackageManager npmPackageManager) {
        return npmPackageManager.getNpmList()
            .stream()
            .collect(
                Collectors.toMap(NpmPackage::name,
                npmPackage -> npmPackage));
    }

    private static Map<String, NpmPackage> getPackagesByName(@Nonnull List<NpmPackage> npmPackages) {
        return npmPackages.stream()
            .collect(
                Collectors.toMap(NpmPackage::name,
                    npmPackage -> npmPackage));
    }

    private static List<NamespaceInfo> getNamespaceInfos(@Nonnull NpmPackageManager npmPackageManager) {
        return npmPackageManager.getNpmList()
            .stream()
            .filter(p -> p.name() != null && !p.name().isEmpty() && p.canonical() != null && !p.canonical().isEmpty())
            .map(npmPackage -> new NamespaceInfo(npmPackage.name(), npmPackage.canonical()))
            .toList();
    }

    protected NpmProcessorOptimized(FhirVersionHolder fhirVersionHolder, Map<String, NpmPackage> npmPackageByName, List<NamespaceInfo> namespaceInfos) {
        this.fhirVersionHolder = fhirVersionHolder;
        this.npmPackageByName = npmPackageByName;
        this.namespaceInfos = namespaceInfos;
    }

    public FhirVersionEnum getFhirVersion() {
        return fhirVersionHolder.fhirVersionEnum;
    }

    public String getFhirVersionString() {
        return fhirVersionHolder.getFhirVersionString();
    }

    public List<NpmPackage> getNpmList() {
        return npmPackageByName.values().stream().toList();
    }

    public List<NamespaceInfo> getNamespaces() {
        return namespaceInfos;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    private static class FhirVersionHolder {
        public static final FhirVersionHolder EMPTY = new FhirVersionHolder(null, null);

        @Nullable
        private final FhirVersionEnum fhirVersionEnum;
        @Nullable
        private final String fhirVersionString;

        public static FhirVersionHolder fromEnum(@Nonnull FhirVersionEnum fhirVersionEnum) {
            return new FhirVersionHolder(fhirVersionEnum, null);
        }

        public static FhirVersionHolder fromVersionString(@Nonnull String fhirVersionString) {
            return new FhirVersionHolder(tryToResolveFhirVersion(fhirVersionString), fhirVersionString);
        }

        @Nullable
        private static FhirVersionEnum tryToResolveFhirVersion(@Nonnull String fhirVersionString) {
            for (FhirVersionEnum fhirVersionEnum : FhirVersionEnum.values()) {
                if (fhirVersionString.startsWith(fhirVersionEnum.getFhirVersionString())) {
                    return fhirVersionEnum;
                }
            }

            return null;
        }

        private FhirVersionHolder(@Nullable FhirVersionEnum fhirVersionEnum, @Nullable String fhirVersionString) {
            this.fhirVersionEnum = fhirVersionEnum;
            this.fhirVersionString = fhirVersionString;
        }

        public String getFhirVersionString() {
            return Optional.ofNullable(fhirVersionEnum)
                .map(FhirVersionEnum::getFhirVersionString)
                .orElse(fhirVersionString);
        }
    }
}
