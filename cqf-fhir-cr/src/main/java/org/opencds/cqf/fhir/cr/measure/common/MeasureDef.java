package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class MeasureDef {

    private final String id;

    @Nullable
    private final String url;

    private final String version;
    private final List<GroupDef> groups;
    private final List<SdeDef> sdes;
    private final List<String> errors;

    public static MeasureDef fromIdAndUrl(String id, @Nullable String url) {
        return new MeasureDef(id, url, null, List.of(), List.of());
    }

    public MeasureDef(String id, String url, String version, List<GroupDef> groups, List<SdeDef> sdes) {
        this.id = id;
        this.url = url;
        this.version = version;
        this.groups = groups;
        this.sdes = sdes;

        this.errors = new ArrayList<>();
    }

    // LUKETODO:  handle FHIR IDs that are qualified and history versioned/etc
    public String id() {
        return this.id;
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
}
