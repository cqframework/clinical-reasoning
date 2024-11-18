package org.opencds.cqf.fhir.cr.visitor.dstu3;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.EnumFactory;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.exceptions.FHIRException;

public class CRMIReleaseExperimentalBehavior {
    public enum CRMIReleaseExperimentalBehaviorCodes {
        /**
         * The repository should throw an error if a specification which is not Experimental references Experimental components.
         */
        ERROR,
        /**
         * The repository should warn if a specification which is not Experimental references Experimental components.
         */
        WARN,
        /**
         * The repository does not need to consider the state of Experimental.
         */
        NONE,
        /**
         * added to help the parsers with the generic types
         */
        NULL;

        public static CRMIReleaseExperimentalBehaviorCodes fromCode(String codeString) throws FHIRException {
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            switch (codeString) {
                case "error":
                    return ERROR;
                case "warn":
                    return WARN;
                case "none":
                    return NONE;

                default:
                    throw new IllegalArgumentException(
                            "Unknown CRMIReleaseExperimentalBehaviorCode '" + codeString + "'");
            }
        }

        public String toCode() {
            switch (this) {
                case ERROR:
                    return "error";
                case WARN:
                    return "warn";
                case NONE:
                    return "none";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getSystem() {
            switch (this) {
                case ERROR, WARN, NONE:
                    return "http://hl7.org/fhir/uv/crmi/CodeSystem/crmi-release-experimental-behavior-codes";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDefinition() {
            switch (this) {
                case ERROR:
                    return "The repository should throw an error if a specification which is not Experimental references Experimental components.";
                case WARN:
                    return "The repository should warn if a specification which is not Experimental references Experimental components.";
                case NONE:
                    return "The repository does not need to consider the state of Experimental.";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch (this) {
                case ERROR:
                    return "Error";
                case WARN:
                    return "Warn";
                case NONE:
                    return "None";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }
    }

    public static class CRMIReleaseExperimentalBehaviorCodesEnumFactory
            implements EnumFactory<CRMIReleaseExperimentalBehaviorCodes> {
        public CRMIReleaseExperimentalBehaviorCodes fromCode(String codeString) throws IllegalArgumentException {
            return CRMIReleaseExperimentalBehaviorCodes.fromCode(codeString);
        }

        public Enumeration<CRMIReleaseExperimentalBehaviorCodes> fromType(Base code) throws FHIRException {
            if (code == null) {
                return null;
            }
            if (code.isEmpty()) {
                return new Enumeration<>(this);
            }
            String codeString = ((PrimitiveType<?>) code).asStringValue();
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            return new Enumeration<>(this, CRMIReleaseExperimentalBehaviorCodes.fromCode(codeString));
        }

        public String toCode(CRMIReleaseExperimentalBehaviorCodes code) {
            return code.toCode();
        }

        public String toSystem(CRMIReleaseExperimentalBehaviorCodes code) {
            return code.getSystem();
        }
    }
}
