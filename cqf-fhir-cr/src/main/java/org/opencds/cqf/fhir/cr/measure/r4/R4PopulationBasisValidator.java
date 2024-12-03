package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import java.util.Map;
import java.util.stream.Collectors;

// LUKETODO: javadoc
public class R4PopulationBasisValidator {

    enum PopulationBasis {
        NONE,
        BOOLEAN,
        ENCOUNTER,
        NON_ENCOUNTER_RESOURCE
    }

    public void validateStratifierBasisType(Measure measure, Map<String, CriteriaResult> subjectValues, GroupDef groupDef) {
        var isBooleanBasisDirect = groupDef.isBooleanBasis();
        var isBooleanBasisindirect = groupDef.getPopulationBasis().code().equals("boolean");
        var isBooleanBasisToUse = isBooleanBasisindirect;

        System.out.printf("boolean basis direct: %s, boolean basis indirect: %s\n", isBooleanBasisDirect, isBooleanBasisindirect);

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasisToUse) {
            var list = subjectValues.values().stream()
                .filter(x -> x.rawValue() instanceof Resource)
                .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new InvalidRequestException(
                    "stratifier expression criteria results must match the same type as population for Measure: "
                        + measure.getUrl());
            }
        }
    }


    // LUKETODO:  javadoc
    protected void validateGroupBasisType(Map<String, CriteriaResult> subjectValues, boolean isBooleanBasis) {

        // LUKETODO:  validate boolean basis doesn't match CriteriaResults

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasis) {
            var list = subjectValues.values().stream()
                .filter(x -> x.rawValue() instanceof Resource)
                .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new IllegalArgumentException(
                    "stratifier expression criteria results must match the same type as population.");
            }
        }
    }

    /**
     *
     * Resource result --> Patient Key, Resource result --> can intersect on patient for Boolean basis, can't for Resource
     * boolean result --> Patient Key, Boolean result --> can intersect on Patient
     * code result --> Patient Key, Code result --> can intersect on Patient
     */
    protected void validateStratifierBasisType(Map<String, CriteriaResult> subjectValues, boolean isBooleanBasis) {

        // LUKETODO:  validate boolean basis doesn't match CriteriaResults

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasis) {
            var list = subjectValues.values().stream()
                .filter(x -> x.rawValue() instanceof Resource)
                .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new IllegalArgumentException(
                    "stratifier expression criteria results must match the same type as population.");
            }
        }
    }
}
