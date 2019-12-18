package com.alphora.cql.service.util;

import java.util.ArrayList;

import org.opencds.cqf.cql.runtime.Code;

import ca.uhn.fhir.context.FhirContext;

public class CodeUtil {

    public static ArrayList<Code> getCodesFromCodeableConceptsObject(Object codes, FhirContext fhirContext) {
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return getDSTU3CodesFromCodeableConceptsObject(codes);
            case R4:
                return getR4CodesFromCodeableConceptsObject(codes);
            default:
                throw new IllegalArgumentException("Unknown fhir version: " + fhirContext.getVersion().getVersion().getFhirVersionString());
        }
    }


    // RuntimeResourceDefinition definition = fhirContext.getResourceDefinition("ValueSet");
	// BaseRuntimeChildDefinition childByName = definition.getChildByName("ValueSet.compose.include");
	// childByName.getAccessor().getValues(valueSet);
    private static ArrayList<Code> getDSTU3CodesFromCodeableConceptsObject(Object codes) {
        ArrayList<Code> elmCodes = new ArrayList<Code>();
        if (codes instanceof Iterable) {
            for (Object codeObj : (Iterable) codes) {
                elmCodes.addAll(getSTU3CodesFromCodeableConcept((org.hl7.fhir.dstu3.model.CodeableConcept)codeObj));
            }
        } else if (codes instanceof org.hl7.fhir.dstu3.model.CodeableConcept) {
            elmCodes.addAll(getSTU3CodesFromCodeableConcept((org.hl7.fhir.dstu3.model.CodeableConcept)codes));
        }
        else if (codes instanceof org.hl7.fhir.dstu3.model.Coding) {
            org.hl7.fhir.dstu3.model.Coding code = (org.hl7.fhir.dstu3.model.Coding) codes;
            elmCodes.add(new Code().withCode(code.getCode()).withSystem(code.getSystem()));
        }
        return elmCodes;
    }

    private static ArrayList<Code> getSTU3CodesFromCodeableConcept(org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept) {
        ArrayList<Code> elmCodes = new ArrayList<Code>();
        Iterable<org.hl7.fhir.dstu3.model.Coding> conceptCodes = codeableConcept.getCoding();
        for (org.hl7.fhir.dstu3.model.Coding code : conceptCodes) {
            elmCodes.add(new Code().withCode(code.getCode()).withSystem(code.getSystem()));
        }
        return elmCodes;
    }

    private static ArrayList<Code> getR4CodesFromCodeableConceptsObject(Object codes) {
        ArrayList<Code> elmCodes = new ArrayList<Code>();
        if (codes instanceof Iterable) {
            for (Object codeObj : (Iterable) codes) {
                Iterable<org.hl7.fhir.r4.model.Coding> conceptCodes = ((org.hl7.fhir.r4.model.CodeableConcept) codeObj).getCoding();
                for (org.hl7.fhir.r4.model.Coding code : conceptCodes) {
                    elmCodes.add(new Code().withCode(code.getCode()).withSystem(code.getSystem()));
                }
            }
        } else if (codes instanceof org.hl7.fhir.r4.model.CodeableConcept) {
            Iterable<org.hl7.fhir.r4.model.Coding> conceptCodes = ((org.hl7.fhir.r4.model.CodeableConcept) codes).getCoding();
            for (org.hl7.fhir.r4.model.Coding code : conceptCodes) {
                elmCodes.add(new Code().withCode(code.getCode()).withSystem(code.getSystem()));
            }
        }
        else if (codes instanceof org.hl7.fhir.r4.model.Coding) {
            org.hl7.fhir.r4.model.Coding code = (org.hl7.fhir.r4.model.Coding) codes;
            elmCodes.add(new Code().withCode(code.getCode()).withSystem(code.getSystem()));
        }
        return elmCodes;
    }

        
}