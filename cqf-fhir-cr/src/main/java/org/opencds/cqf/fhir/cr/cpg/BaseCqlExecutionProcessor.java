package org.opencds.cqf.fhir.cr.cpg;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;

public class BaseCqlExecutionProcessor {

    public List<Pair<String, String>> resolveIncludedLibraries(List<?> includedLibraries) {
        if (includedLibraries != null) {
            List<Pair<String, String>> libraries = new ArrayList<>();
            String name = null;
            String url = null;
            for (Object parameters : includedLibraries) {
                if (parameters instanceof org.hl7.fhir.dstu3.model.Parameters) {
                    for (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent parameterComponent :
                            ((org.hl7.fhir.dstu3.model.Parameters) parameters).getParameter()) {
                        if (parameterComponent.getName().equalsIgnoreCase("url")) {
                            url = parameterComponent.getValue().primitiveValue();
                        }
                        if (parameterComponent.getName().equalsIgnoreCase("name")) {
                            name = parameterComponent.getValue().primitiveValue();
                        }
                    }
                } else {
                    for (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent parameterComponent :
                            ((org.hl7.fhir.r4.model.Parameters) parameters).getParameter()) {
                        if (parameterComponent.getName().equalsIgnoreCase("url")) {
                            url = parameterComponent.getValue().primitiveValue();
                        }
                        if (parameterComponent.getName().equalsIgnoreCase("name")) {
                            name = parameterComponent.getValue().primitiveValue();
                        }
                    }
                }
                libraries.add(Pair.of(url, name));
            }
            return libraries;
        }
        return null;
    }

    public VersionedIdentifier resolveLibraryIdentifier(
            String content, IBaseResource library, LibraryManager libraryManager) {

        if (!StringUtils.isBlank(content)) {
            var translatedLibrary =
                    CqlTranslator.fromText(content, libraryManager).getTranslatedLibrary();
            return new VersionedIdentifier()
                    .withId(translatedLibrary.getIdentifier().getId())
                    .withVersion(translatedLibrary.getIdentifier().getVersion());
        } else if (library == null) {
            return null;
        } else {
            Library r4Library = (Library) library;
            return new VersionedIdentifier()
                    .withId(
                            r4Library.hasUrl()
                                    ? Canonicals.getIdPart(r4Library.getUrl())
                                    : r4Library.hasName() ? r4Library.getName() : null)
                    .withVersion(
                            r4Library.hasVersion()
                                    ? r4Library.getVersion()
                                    : r4Library.hasUrl() ? Canonicals.getVersion(r4Library.getUrl()) : null);
        }
    }

    public IBaseOperationOutcome createIssue(String severity, String details, Repository repository) {
        if (repository.fhirContext().getVersion().getVersion() == FhirVersionEnum.DSTU3) {
            return new org.hl7.fhir.dstu3.model.OperationOutcome()
                    .addIssue(new org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent()
                            .setSeverity(org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity.fromCode(severity))
                            .setDetails(new org.hl7.fhir.dstu3.model.CodeableConcept().setText(details)));
        } else {
            return new org.hl7.fhir.r4.model.OperationOutcome()
                    .addIssue(new org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent()
                            .setSeverity(org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.fromCode(severity))
                            .setDetails(new org.hl7.fhir.r4.model.CodeableConcept().setText(details)));
        }
    }

    // expressionEvaluator
    // libraryEvaluator

}
