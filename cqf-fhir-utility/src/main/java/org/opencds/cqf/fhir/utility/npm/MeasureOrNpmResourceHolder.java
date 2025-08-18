package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

// LUKETODO:  top level record
// LUKETODO:  docs:  the measure in question is either found in the DB or derived from NPM
public final class MeasureOrNpmResourceHolder {

    @Nullable
    private final Measure measure;

    private final NpmResourceHolder npmResourceHolder;

    public static MeasureOrNpmResourceHolder measureOnly(Measure measure) {
        return new MeasureOrNpmResourceHolder(measure, NpmResourceHolder.EMPTY);
    }

    public static MeasureOrNpmResourceHolder npmOnly(NpmResourceHolder npmResourceHolder) {
        return new MeasureOrNpmResourceHolder(null, npmResourceHolder);
    }

    private MeasureOrNpmResourceHolder(@Nullable Measure measure, NpmResourceHolder npmResourceHolder) {
        if (measure == null && (NpmResourceHolder.EMPTY == npmResourceHolder)) {
            throw new InternalErrorException("Measure and NpmResourceHolder cannot both be null");
        }
        this.measure = measure;
        this.npmResourceHolder = npmResourceHolder;
    }

    public boolean hasNpmLibrary() {
        return Optional.ofNullable(npmResourceHolder)
                .flatMap(NpmResourceHolder::getOptMainLibrary)
                .isPresent();
    }

    // LUKETODO: calls to hasLibrary are always inverted
    public boolean hasLibrary() {
        if (measure == null && (NpmResourceHolder.EMPTY == npmResourceHolder)) {
            throw new InvalidRequestException("Measure and NpmResourceHolder cannot both be null");
        }

        if (measure != null) {
            return measure.hasLibrary();
        }

        return npmResourceHolder.getOptMainLibrary().isPresent();
    }

    public String getMainLibraryUrl() {
        if (measure == null && (NpmResourceHolder.EMPTY == npmResourceHolder)) {
            throw new InvalidRequestException("Measure and NpmResourceHolder cannot both be null");
        }

        if (measure != null) {
            final List<CanonicalType> libraryUrls = measure.getLibrary();

            if (libraryUrls.isEmpty()) {
                throw new InvalidRequestException(
                        "Measure does not have any library urls specified: %s".formatted(measure.getUrl()));
            }

            return libraryUrls.get(0).asStringValue();
        }

        return npmResourceHolder
                .getOptMainLibrary()
                .map(ILibraryAdapter::getUrl)
                .orElse(null); // LUKETODO:  is this wise?
    }

    public IIdType getMeasureIdElement() {
        return getMeasure().getIdElement();
    }

    public boolean hasMeasureUrl() {
        return getMeasure().hasUrl();
    }

    public String getMeasureUrl() {
        return getMeasure().getUrl();
    }

    public Measure getMeasure() {
        var optMeasureFromNpm = Optional.ofNullable(npmResourceHolder).flatMap(NpmResourceHolder::getMeasure);

        if (optMeasureFromNpm.isPresent() && optMeasureFromNpm.get().get() instanceof Measure measureFromNpm) {
            return measureFromNpm;
        }

        return measure;
    }

    public NpmResourceHolder npmResourceHolder() {
        return npmResourceHolder;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (MeasureOrNpmResourceHolder) obj;
        return Objects.equals(this.measure, that.measure)
                && Objects.equals(this.npmResourceHolder, that.npmResourceHolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(measure, npmResourceHolder);
    }

    @Override
    public String toString() {
        return "MeasurePlusNpmResourceHolder[" + "measure="
                + measure + ", " + "npmResourceHolders="
                + npmResourceHolder + ']';
    }
}
