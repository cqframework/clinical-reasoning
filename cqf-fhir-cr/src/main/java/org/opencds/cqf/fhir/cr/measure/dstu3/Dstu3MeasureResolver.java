package org.opencds.cqf.fhir.cr.measure.dstu3;

import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureValidationException;
import org.opencds.cqf.fhir.cr.measure.common.ResolvedMeasure;

/**
 * Resolves DSTU3 Measure resources into version-agnostic domain types.
 *
 * <p>Extracted from {@link Dstu3MeasureProcessor} so that {@link Dstu3MeasureService}
 * can perform resolution without instantiating the full legacy processor.</p>
 */
public class Dstu3MeasureResolver {

    private final IRepository repository;

    public Dstu3MeasureResolver(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Builds a version-agnostic {@link ResolvedMeasure} from a DSTU3 Measure resource.
     */
    ResolvedMeasure buildResolvedMeasure(Measure measure) {
        checkMeasureLibrary(measure);
        var measureDef = new Dstu3MeasureDefBuilder().build(measure);
        var libraryId = getLibraryVersionIdentifier(measure);
        return new ResolvedMeasure(measureDef, libraryId, measure.getUrl());
    }

    /**
     * Converts DSTU3 FHIR Parameters to a CQL parameter map.
     *
     * <p>Symmetric with {@link org.opencds.cqf.fhir.cr.measure.r4.R4MeasureResolver#resolveParameterMap}.
     * Not extracted because {@code Parameters.getParameter()} returns version-specific component types
     * with no shared interface for {@code hasResource()/getValue()/getName()}.
     */
    Map<String, Object> resolveParameterMap(Parameters parameters) {
        if (parameters == null) {
            return Map.of();
        }
        Map<String, Object> parameterMap = new HashMap<>();
        Dstu3FhirModelResolver modelResolver = new Dstu3FhirModelResolver();
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
                if (parameterMap.get(param.getName()) instanceof java.util.List) {
                    if (value != null) {
                        @SuppressWarnings("unchecked")
                        var list = (java.util.List<Object>) parameterMap.get(param.getName());
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
     * Maps a {@link MeasureEvalType} to the corresponding DSTU3 {@link MeasureReportType}.
     */
    MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
        return switch (measureEvalType) {
            case PATIENT, SUBJECT -> MeasureReportType.INDIVIDUAL;
            case PATIENTLIST, SUBJECTLIST -> MeasureReportType.PATIENTLIST;
            case POPULATION -> MeasureReportType.SUMMARY;
        };
    }

    private VersionedIdentifier getLibraryVersionIdentifier(Measure measure) {
        var reference = measure.getLibrary().get(0);
        var library = repository.read(Library.class, reference.getReferenceElement());
        return new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion());
    }

    private void checkMeasureLibrary(Measure measure) {
        if (!measure.hasLibrary()) {
            throw new MeasureValidationException(
                    "Measure %s does not have a primary library specified".formatted(measure.getUrl()));
        }
    }
}
