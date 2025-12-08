package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.hl7.fhir.instance.model.api.IIdType;

public class MeasureDef {

    private final IIdType idType;

    @Nullable
    private final String url;

    private final String version;
    private final List<GroupDef> groups;
    private final List<SdeDef> sdes;
    private final List<String> errors;

    public static MeasureDef fromIdAndUrl(IIdType idType, @Nullable String url) {
        return new MeasureDef(idType, url, null, List.of(), List.of());
    }

    public MeasureDef(IIdType idType, @Nullable String url, String version, List<GroupDef> groups, List<SdeDef> sdes) {
        this.idType = idType;
        this.url = url;
        this.version = version;
        this.groups = groups;
        this.sdes = sdes;

        this.errors = new ArrayList<>();
    }

    // This is the raw unqualified ID (ex: for "Measure/measure123", we return "measure123"
    public String id() {
        return this.idType.toUnqualifiedVersionless().getIdPart();
    }

    // This is the qualified FHIR ID (ex: for "Measure/measure123" or "measure123",
    // we return "Measure/measure123"
    public String idWithFhirResourceQualifier() {
        return this.idType.toUnqualifiedVersionless().getValueAsString();
    }

    @Nullable
    public String url() {
        return this.url;
    }

    public String version() {
        return this.version;
    }

    public List<SdeDef> sdes() {
        return this.sdes;
    }

    public List<GroupDef> groups() {
        return this.groups;
    }

    public List<String> errors() {
        return this.errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    // We need to limit the contract of equality to id, url, and version only
    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MeasureDef that = (MeasureDef) other;
        return Objects.equals(idType, that.idType)
                && Objects.equals(url, that.url)
                && Objects.equals(version, that.version);
    }

    // We need to limit the contract of equality to id, url, and version only
    @Override
    public int hashCode() {
        return Objects.hash(idType, url, version);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MeasureDef.class.getSimpleName() + "[", "]")
                .add("idType='" + idType.getValueAsString() + "'")
                .add("url='" + url + "'")
                .add("version='" + version + "'")
                .add("groups=" + groups.size())
                .add("sdes=" + sdes.size())
                .add("errors=" + errors)
                .toString();
    }

    /**
     * Creates a deep copy snapshot of this MeasureDef for preservation/testing.
     * <p>
     * This method performs a recursive deep copy of the entire MeasureDef object graph:
     * <ul>
     *   <li>All collections (Lists, Sets, Maps) are deep copied</li>
     *   <li>FHIR resource objects are shared (shallow copied)</li>
     *   <li>Mutable fields (scores) are value copied</li>
     * </ul>
     * <p>
     * Intended for use by testing frameworks to capture MeasureDef state after evaluation
     * for analysis, comparison, or serialization. This operation has minimal performance
     * impact as it creates ~100-500 object allocations (< 10ms for typical measures).
     * <p>
     * <strong>Thread Safety:</strong> This method is NOT thread-safe. Do not call while
     * the MeasureDef is being mutated by evaluation/scoring operations.
     * <p>
     * <strong>FHIR Resource Sharing:</strong> FHIR resource objects referenced in
     * PopulationDef collections are shared between the original and snapshot. Mutations
     * to FHIR resources will be visible in both instances.
     *
     * @return A new MeasureDef instance containing deep copies of all collections but
     *         sharing FHIR resource references with the original
     */
    public MeasureDef createSnapshot() {
        // Deep copy groups (recursively snapshots all nested Defs)
        List<GroupDef> snapshotGroups =
                groups.stream().map(GroupDef::createSnapshot).toList();

        // Deep copy SDEs
        List<SdeDef> snapshotSdes = sdes.stream().map(SdeDef::createSnapshot).toList();

        // Create new MeasureDef with snapshot collections
        MeasureDef snapshot = new MeasureDef(idType, url, version, snapshotGroups, snapshotSdes);

        // Deep copy errors list (mutable ArrayList)
        snapshot.errors.addAll(this.errors);

        return snapshot;
    }
}
