package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureResolutionException;
import org.opencds.cqf.fhir.cr.measure.common.MeasureValidationException;
import org.opencds.cqf.fhir.cr.measure.common.ResolvedMeasure;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * Resolves R4 Measure resources into version-agnostic {@link ResolvedMeasure} instances,
 * converts FHIR Parameters to CQL parameter maps, and maps evaluation types to report types.
 *
 * <p>Extracted from {@link R4MeasureProcessor} so that services needing only resolution
 * (e.g. {@link R4MeasureService}) do not depend on the full processor.</p>
 */
public class R4MeasureResolver {

    private final IRepository repository;

    public R4MeasureResolver(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Builds a version-agnostic {@link ResolvedMeasure} from an R4 Measure resource.
     * Validates the measure (library, unique IDs, SDEs), builds the MeasureDef, and resolves
     * the library identifier.
     */
    public ResolvedMeasure buildResolvedMeasure(Measure measure) {
        checkMeasureLibrary(measure);
        R4MeasureDefBuilder.triggerFirstPassValidation(List.of(measure));
        var measureDef = new R4MeasureDefBuilder().build(measure);
        var libraryId = getLibraryVersionIdentifier(measure);
        return new ResolvedMeasure(measureDef, libraryId, measure.getUrl());
    }

    /**
     * Converts a FHIR {@link Parameters} resource into a CQL parameter map.
     *
     * <p>Symmetric with {@link org.opencds.cqf.fhir.cr.measure.dstu3.Dstu3MeasureResolver#resolveParameterMap}.
     * Not extracted because {@code Parameters.getParameter()} returns version-specific component types
     * with no shared interface for {@code hasResource()/getValue()/getName()}.
     *
     * @param parameters FHIR Parameters resource (may be null)
     * @return map of parameter name to resolved value; empty map if parameters is null
     */
    public Map<String, Object> resolveParameterMap(@Nullable Parameters parameters) {
        if (parameters == null) {
            return Map.of();
        }

        Map<String, Object> parameterMap = new HashMap<>();
        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
        parameters.getParameter().forEach(param -> {
            Object value;
            if (param.hasResource()) {
                value = param.getResource();
            } else {
                value = param.getValue();
                if (value instanceof IPrimitiveType<?> type) {
                    // TODO: handle Code, CodeableConcept, Quantity, etc
                    // resolves Date/Time values
                    value = modelResolver.toJavaPrimitive(type.getValue(), value);
                }
            }
            if (parameterMap.containsKey(param.getName())) {
                if (parameterMap.get(param.getName()) instanceof List) {
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        var list = (List<Object>) parameterMap.get(param.getName());
                        list.add(value);
                    }
                } else {
                    // We need a mutable list here, otherwise, retrieving the list above will fail with
                    // UnsupportedOperationException
                    parameterMap.put(
                            param.getName(), new ArrayList<>(Arrays.asList(parameterMap.get(param.getName()), value)));
                }
            } else {
                parameterMap.put(param.getName(), value);
            }
        });
        return parameterMap;
    }

    /**
     * Maps a {@link MeasureEvalType} to the corresponding R4 {@link MeasureReportType}.
     */
    public MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType, String measureUrl) {
        return switch (measureEvalType) {
            case SUBJECT -> MeasureReportType.INDIVIDUAL;
            case SUBJECTLIST -> MeasureReportType.SUBJECTLIST;
            case POPULATION -> MeasureReportType.SUMMARY;
            default ->
                throw new MeasureValidationException("Unsupported MeasureEvalType: %s for Measure: %s"
                        .formatted(measureEvalType.toCode(), measureUrl));
        };
    }

    void checkMeasureLibrary(Measure measure) {
        if (!measure.hasLibrary()) {
            throw new MeasureValidationException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }
    }

    /**
     * Extracts the Library version identifier from the Measure's primary library reference.
     *
     * @param measure resource that has desired Library
     * @return version identifier of Library
     */
    private VersionedIdentifier getLibraryVersionIdentifier(Measure measure) {
        if (measure == null) {
            throw new MeasureValidationException("Measure provided is null");
        }

        if (!measure.hasLibrary() || measure.getLibrary().isEmpty()) {
            throw new MeasureValidationException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }

        var url = measure.getLibrary().get(0).asStringValue();

        Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
        if (b.getEntry().isEmpty()) {
            var errorMsg = "Unable to find Library with url: %s".formatted(url);
            throw new MeasureResolutionException(errorMsg);
        }
        return VersionedIdentifiers.forUrl(url);
    }
}
