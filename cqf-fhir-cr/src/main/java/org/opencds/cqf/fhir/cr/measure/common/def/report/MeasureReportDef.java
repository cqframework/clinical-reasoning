package org.opencds.cqf.fhir.cr.measure.common.def.report;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.MeasureDef;

public class MeasureReportDef {

    private final MeasureDef measureDef; // Reference to immutable measure structure
    private final List<GroupReportDef> groups;
    private final List<SdeReportDef> sdes;
    private final List<String> errors;

    /**
     * Factory method to create MeasureReportDef from id and url (for testing).
     * Creates a minimal MeasureDef internally.
     */
    public static MeasureReportDef fromIdAndUrl(IIdType idType, @Nullable String url) {
        MeasureDef measureDef = MeasureDef.fromIdAndUrl(idType, url);
        return fromMeasureDef(measureDef);
    }

    /**
     * Factory method to create MeasureReportDef from immutable MeasureDef.
     * The MeasureReportDef will have empty mutable state (errors list initially empty).
     */
    public static MeasureReportDef fromMeasureDef(MeasureDef measureDef) {
        List<GroupReportDef> groupReportDefs =
                measureDef.groups().stream().map(GroupReportDef::fromGroupDef).toList();
        List<SdeReportDef> sdeReportDefs =
                measureDef.sdes().stream().map(SdeReportDef::fromSdeDef).toList();
        return new MeasureReportDef(measureDef, groupReportDefs, sdeReportDefs);
    }

    /**
     * Constructor for creating MeasureReportDef with a MeasureDef reference.
     * This is the primary constructor for production use.
     */
    public MeasureReportDef(MeasureDef measureDef, List<GroupReportDef> groups, List<SdeReportDef> sdes) {
        this.measureDef = measureDef;
        this.groups = groups;
        this.sdes = sdes;
        this.errors = new ArrayList<>();
    }

    /**
     * Test-only constructor for creating MeasureReportDef with explicit structural data.
     * Creates a minimal MeasureDef internally. Use fromMeasureDef() for production code.
     */
    public MeasureReportDef(
            IIdType idType,
            @Nullable String url,
            String version,
            List<GroupReportDef> groups,
            List<SdeReportDef> sdes) {
        // Create minimal MeasureDef with empty groups/sdes (structural definition)
        this.measureDef = new MeasureDef(idType, url, version, List.of(), List.of());
        this.groups = groups;
        this.sdes = sdes;
        this.errors = new ArrayList<>();
    }

    // Accessor for immutable measure structure
    public MeasureDef measureDef() {
        return this.measureDef;
    }

    // Delegate structural queries to measureDef
    // This is the raw unqualified ID (ex: for "Measure/measure123", we return "measure123"
    public String id() {
        return measureDef.id();
    }

    // This is the qualified FHIR ID (ex: for "Measure/measure123" or "measure123",
    // we return "Measure/measure123"
    public String idWithFhirResourceQualifier() {
        return measureDef.idWithFhirResourceQualifier();
    }

    @Nullable
    public String url() {
        return measureDef.url();
    }

    public String version() {
        return measureDef.version();
    }

    public List<SdeReportDef> sdes() {
        return this.sdes;
    }

    public List<GroupReportDef> groups() {
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
        MeasureReportDef that = (MeasureReportDef) other;
        // Delegate to measureDef equality (which compares id, url, version)
        return Objects.equals(measureDef, that.measureDef);
    }

    // We need to limit the contract of equality to id, url, and version only
    @Override
    public int hashCode() {
        // Delegate to measureDef hashCode (which hashes id, url, version)
        return Objects.hash(measureDef);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MeasureReportDef.class.getSimpleName() + "[", "]")
                .add("id='" + id() + "'")
                .add("url='" + url() + "'")
                .add("version='" + version() + "'")
                .add("groups=" + groups.size())
                .add("sdes=" + sdes.size())
                .add("errors=" + errors)
                .toString();
    }
}
