package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;

// LUKETODO:  or, do we really care about anything that deals with MeasureReports?  Maybe we just draw a box around
// anything that deals directly with the CQL engine or non-measure report code?
// LUKETODO:  consider R4/DSTU3/etc Measure to MeasureDef adapter classes
// LUKETODO: consider a Builder, because adding a massive constructor will not scale
// LUKETODO: the MeasureDef will need to perhaps encapsulate part of the FHIR-version specific resource for conversions
// such as getImprovementNotation()
public class MeasureDef implements IDef {

    private final String id;
    private final String url;
    private final String version;
    private String language;
    private String description;
    private String implicitRules;
    private final List<GroupDef> groups;
    private final List<SdeDef> sdes;
    private final List<String> errors;
    private final List<LibraryDef> libraryDefs;

    public static MeasureDef fromIdAndUrl(String id, String url) {
        return new MeasureDef(id, url, null, List.of(), List.of(), List.of());
    }

    public MeasureDef(
            String id,
            String url,
            String version,
            List<GroupDef> groups,
            List<SdeDef> sdes,
            List<LibraryDef> libraryDefs) {
        this.id = id;
        this.url = url;
        this.version = version;
        this.groups = groups;
        this.sdes = sdes;
        this.libraryDefs = libraryDefs;

        this.errors = new ArrayList<>();
    }

    public String id() {
        return this.id;
    }

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

    public List<LibraryDef> libraryDefs() {
        return this.libraryDefs;
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
        return Objects.equals(id, that.id) && Objects.equals(url, that.url) && Objects.equals(version, that.version);
    }

    // We need to limit the contract of equality to id, url, and version only
    @Override
    public int hashCode() {
        return Objects.hash(id, url, version);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MeasureDef.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("url='" + url + "'")
                .add("version='" + version + "'")
                .add("groups=" + groups.size())
                .add("sdes=" + sdes.size())
                .add("errors=" + errors)
                .toString();
    }

    public String getUrlForMeasureReport() {
        if (StringUtils.isNotBlank(url()) && !url().contains("|") && hasVersion()) {
            return url() + "|" + version();
        }
        return url();
    }

    private boolean hasVersion() {
        return StringUtils.isNotBlank(this.version);
    }

    public String getImplicitRules() {
        return implicitRules;
    }

    // LUKETODO: need to represent an ImprovementNotation in a way the MeasureReport builder can take
}
