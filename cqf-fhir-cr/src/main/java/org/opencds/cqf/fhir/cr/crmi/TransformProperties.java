package org.opencds.cqf.fhir.cr.crmi;

public class TransformProperties {

    private TransformProperties() {}

    public static final String usPHTriggeringVSProfile =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset";
    public static final String usPHTriggeringVSLibProfile =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset-library";
    public static final String ersdVSLibProfile =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-valueset-library";
    public static final String ersdVSProfile = "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-valueset";
    public static final String ersdPlanDefinitionProfile =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/ersd-plandefinition";
    public static final String usPHPlanDefinitionProfile =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-plandefinition";
    public static final String usPHSpecLibProfile =
            "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-specification-library";
    public static final String usPHUsageContextType = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
    public static final String hl7UsageContextType = "http://terminology.hl7.org/CodeSystem/usage-context-type";
    public static final String usPHUsageContext = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";
    public static final String crmiManifestLibrary =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-manifestlibrary";
    public static final String crmiIsOwned = "http://hl7.org/fhir/StructureDefinition/artifact-isOwned";
    public static final String vsmCondition = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-condition";
    public static final String vsmPriority = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-priority";
    public static final String VSM_PRIORITY_CODE = "priority";
    public static final String CRMI_INTENDED_USAGE_CONTEXT_EXT_URL =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-intendedUsageContext";
    public static final String authoritativeSourceExtUrl =
            "http://hl7.org/fhir/StructureDefinition/valueset-authoritativeSource";
    public static final String vsacUrl = "https://cts.nlm.nih.gov/fhir";
    public static final String valueSetGrouperProfile =
            "http://aphl.org/fhir/vsm/StructureDefinition/vsm-groupervalueset";
    public static final String leafValueSetConditionProfile =
            "http://aphl.org/fhir/vsm/StructureDefinition/vsm-conditionvalueset";
    public static final String leafValueSetVsmHostedProfile =
            "http://aphl.org/fhir/vsm/StructureDefinition/vsm-hostedvalueset";
    public static final String grouperUsageContextCodeURL = "http://aphl.org/fhir/vsm/ValueSet/usage-context-type";
    public static final String grouperUsageContextCodableConceptSystemURL =
            "http://aphl.org/fhir/vsm/ValueSet/usage-context-value";
    public static final String grouperType = "grouper-type";
    public static final String modelGrouper = "model-grouper";
}
