package org.opencds.cqf.fhir.cr.visitor.dstu3;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.EnumFactory;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.exceptions.FHIRException;

public class CRMIReleaseVersionBehavior {
    public enum CRMIReleaseVersionBehaviorCodes {
        /**
         * The version provided will be applied to the root artifact and all owned components if a version is not specified.
         */
        DEFAULT,
        /**
         * If the root artifact has a specified version different from the version passed to the operation, an error will be returned.
         */
        CHECK,
        /**
         * The version provided will be applied to the root artifact and all owned components, regardless of whether or not a version was already specified.
         */
        FORCE,
        /**
         * added to help the parsers with the generic types
         */
        NULL;

        public static CRMIReleaseVersionBehaviorCodes fromCode(String codeString) throws FHIRException {
            if (StringUtils.isBlank(codeString)) {
                return null;
            }
            switch (codeString) {
                case "default":
                    return DEFAULT;
                case "check":
                    return CHECK;
                case "force":
                    return FORCE;

                default:
                    throw new FHIRException("Unknown CRMIReleaseVersionBehaviorCodes '" + codeString + "'");
            }
        }

        public String toCode() {
            switch (this) {
                case DEFAULT:
                    return "default";
                case CHECK:
                    return "check";
                case FORCE:
                    return "force";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getSystem() {
            switch (this) {
                case DEFAULT:
                case CHECK:
                case FORCE:
                    return "http://hl7.org/fhir/uv/crmi/ValueSet/crmi-release-version-behavior";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDefinition() {
            switch (this) {
                case DEFAULT:
                    return "The version provided will be applied to the root artifact and all owned components if a version is not specified.";
                case CHECK:
                    return "If the root artifact has a specified version different from the version passed to the operation, an error will be returned.";
                case FORCE:
                    return "The version provided will be applied to the root artifact and all owned components, regardless of whether or not a version was already specified.";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }

        public String getDisplay() {
            switch (this) {
                case DEFAULT:
                    return "Default";
                case CHECK:
                    return "Check";
                case FORCE:
                    return "Force";
                case NULL:
                    return null;
                default:
                    return "?";
            }
        }
    }

    public static class CRMIReleaseVersionBehaviorCodesEnumFactory
            implements EnumFactory<CRMIReleaseVersionBehaviorCodes> {
        public CRMIReleaseVersionBehaviorCodes fromCode(String codeString) throws IllegalArgumentException {
            return CRMIReleaseVersionBehaviorCodes.fromCode(codeString);
        }

        public Enumeration<CRMIReleaseVersionBehaviorCodes> fromType(Base code) throws FHIRException {
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
            return new Enumeration<>(this, CRMIReleaseVersionBehaviorCodes.fromCode(codeString));
        }

        public String toCode(CRMIReleaseVersionBehaviorCodes code) {
            return code.toCode();
        }

        public String toSystem(CRMIReleaseVersionBehaviorCodes code) {
            return code.getSystem();
        }
    }
}
