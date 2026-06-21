package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Immutable context provided to {@link MeasureDefValidator} implementations during pre-evaluation
 * validation. Bundles together the domain-level {@link MeasureDef}, the raw FHIR Measure resource,
 * the {@link IRepository} for probing resource availability, and any user-supplied parameters.
 */
public record MeasureDefValidationContext(
        MeasureDef measureDef, IBaseResource measure, IRepository repository, Map<String, Object> parameters) {

    public MeasureDefValidationContext(
            MeasureDef measureDef,
            IBaseResource measure,
            IRepository repository,
            @Nullable Map<String, Object> parameters) {
        this.measureDef = measureDef;
        this.measure = measure;
        this.repository = repository;
        this.parameters = parameters != null ? parameters : Map.of();
    }

    public MeasureDefValidationContext(MeasureDef measureDef, IBaseResource measure, IRepository repository) {
        this(measureDef, measure, repository, null);
    }
}
