package org.opencds.cqf.fhir.cr.visitor.r5;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import org.hl7.fhir.r5.model.UsageContext;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public class PackageVisitor {
    private PackageVisitor() {}

    private static final String CRMI_INTENDED_USAGE_CONTEXT_URL =
            "http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-intendedUsageContext";

    public static void applyManifestUsageContextsToValueSets(
            List<IKnowledgeArtifactAdapter> valueSetResources, List<IDependencyInfo> dependencies) {

        for (IKnowledgeArtifactAdapter adapter : valueSetResources) {
            ValueSet valueSet = (ValueSet) adapter.get();

            // Build canonical string for matching (url + optional version)
            String canonical = valueSet.getUrl();
            if (valueSet.hasVersion()) {
                canonical += "|" + valueSet.getVersion();
            }

            // Find dependencies that reference this ValueSet
            String finalCanonical = canonical;
            dependencies.stream()
                    .filter(dep -> finalCanonical.equals(dep.getReference()))
                    .forEach(dep ->
                            // Look for crmi-intendedUsageContext extensions
                            dep.getExtension().stream()
                                    .filter(ext -> CRMI_INTENDED_USAGE_CONTEXT_URL.equals(ext.getUrl()))
                                    .forEach(ext -> {
                                        // Get the valueUsageContext from the extension
                                        if (ext.getValue() instanceof UsageContext usageContext) {
                                            // Only add if not already present
                                            boolean alreadyExists = valueSet.getUseContext().stream()
                                                    .anyMatch(existing -> existing.equalsDeep(usageContext));
                                            if (!alreadyExists) {
                                                valueSet.addUseContext(usageContext);
                                            }
                                        } else {
                                            throw new InvalidRequestException(
                                                    "crmi-intendedUsageContext extension does not have a valueUsageContext value");
                                        }
                                    }));
        }
    }
}
