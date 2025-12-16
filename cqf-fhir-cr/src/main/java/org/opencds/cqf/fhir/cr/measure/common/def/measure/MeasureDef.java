package org.opencds.cqf.fhir.cr.measure.common.def.measure;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * Immutable definition of a FHIR Measure resource structure.
 * Contains only the measure's structural metadata (id, url, version, groups, SDEs).
 * Does NOT contain evaluation state - use MeasureReportDef for that.
 */
public class MeasureDef {

    private final IIdType idType;

    @Nullable
    private final String url;

    private final String version;
    private final List<GroupDef> groups;
    private final List<SdeDef> sdes;

    public static MeasureDef fromIdAndUrl(IIdType idType, @Nullable String url) {
        return new MeasureDef(idType, url, null, List.of(), List.of());
    }

    public MeasureDef(IIdType idType, @Nullable String url, String version, List<GroupDef> groups, List<SdeDef> sdes) {
        this.idType = idType;
        this.url = url;
        this.version = version;
        this.groups = List.copyOf(groups); // Defensive copy for immutability
        this.sdes = List.copyOf(sdes); // Defensive copy for immutability
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
        return this.sdes; // Already unmodifiable from List.copyOf()
    }

    public List<GroupDef> groups() {
        return this.groups; // Already unmodifiable from List.copyOf()
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
                .toString();
    }
}
