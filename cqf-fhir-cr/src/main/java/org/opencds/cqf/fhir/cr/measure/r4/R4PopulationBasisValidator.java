package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

/**
 * Validates group populations and stratifiers against population basis-es for R4.
 * Provides R4-specific FHIR type mappings; all validation logic lives in
 * {@link PopulationBasisValidator} default methods.
 */
public class R4PopulationBasisValidator implements PopulationBasisValidator {

    private static final Set<String> ALLOWED_STRATIFIER_VALUE_TYPES = new HashSet<>(Arrays.asList(
            // hack to get around HAPI enums
            "org.hl7.fhir.r4.model.AdministrativeGender",
            CodeableConcept.class.getName(),
            Quantity.class.getName(),
            Range.class.getName(),
            Reference.class.getName(),
            Coding.class.getName(),
            Enumeration.class.getName(),
            Boolean.class.getName(),
            // added Integer and String for examples like age or gender
            Integer.class.getName(),
            String.class.getName(),
            // CQL type returned by some stratifier expression that don't map neatly to FHIR types
            Code.class.getName()));

    private static final String FHIR_MODEL_PACKAGE = "org.hl7.fhir.r4.model.";

    private static final List<String> RESOURCE_TYPE_NAMES =
            Arrays.stream(ResourceType.values()).map(ResourceType::name).toList();

    @Override
    public Set<String> allowedStratifierValueTypes() {
        return ALLOWED_STRATIFIER_VALUE_TYPES;
    }

    @Override
    public Optional<String> extractFhirResourceType(String groupPopulationBasisCode) {
        return resolveResourceType(groupPopulationBasisCode, RESOURCE_TYPE_NAMES, FHIR_MODEL_PACKAGE);
    }
}
