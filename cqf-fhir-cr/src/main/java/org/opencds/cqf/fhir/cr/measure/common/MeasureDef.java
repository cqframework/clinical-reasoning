package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
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

    @Nullable
    private final String description;

    @Nullable
    private final String language;

    @Nullable
    private final String implicitRules;

    @Nullable
    private final CodeDef measureImprovementNotation;

    public static MeasureDef fromIdAndUrl(IIdType idType, @Nullable String url) {
        return new MeasureDef(idType, url, null, List.of(), List.of());
    }

    public MeasureDef(IIdType idType, @Nullable String url, String version, List<GroupDef> groups, List<SdeDef> sdes) {
        this(idType, url, version, groups, sdes, null, null, null, null);
    }

    public MeasureDef(
            IIdType idType,
            @Nullable String url,
            String version,
            List<GroupDef> groups,
            List<SdeDef> sdes,
            @Nullable String description,
            @Nullable String language,
            @Nullable String implicitRules) {
        this(idType, url, version, groups, sdes, description, language, implicitRules, null);
    }

    public MeasureDef(
            IIdType idType,
            @Nullable String url,
            String version,
            List<GroupDef> groups,
            List<SdeDef> sdes,
            @Nullable String description,
            @Nullable String language,
            @Nullable String implicitRules,
            @Nullable CodeDef measureImprovementNotation) {
        this.idType = idType;
        this.url = url;
        this.version = version;
        this.groups = groups;
        this.sdes = sdes;
        this.description = description;
        this.language = language;
        this.implicitRules = implicitRules;
        this.measureImprovementNotation = measureImprovementNotation;
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

    @Nullable
    public String description() {
        return this.description;
    }

    @Nullable
    public String language() {
        return this.language;
    }

    @Nullable
    public String implicitRules() {
        return this.implicitRules;
    }

    /**
     * The measure-level improvement notation, if explicitly set on the Measure resource.
     * Null when the Measure had no explicit improvement notation (groups may still have defaults).
     */
    @Nullable
    public CodeDef measureImprovementNotation() {
        return this.measureImprovementNotation;
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
