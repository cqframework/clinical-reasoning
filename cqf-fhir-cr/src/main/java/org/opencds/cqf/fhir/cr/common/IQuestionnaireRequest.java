package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.QuestionnaireAdapter;

public interface IQuestionnaireRequest extends IOperationRequest {
    IBaseResource getQuestionnaire();

    QuestionnaireAdapter getQuestionnaireAdapter();

    default void addQuestionnaireItem(IBaseBackboneElement item) {
        getModelResolver().setValue(getQuestionnaire(), "item", Collections.singletonList(item));
    }

    default void addLaunchContextExtensions(List<IBaseExtension<?, ?>> launchContextExts) {
        if (launchContextExts != null && !launchContextExts.isEmpty()) {
            launchContextExts.forEach(e -> {
                var code = e.getExtension().stream()
                        .map(c -> (IBaseExtension<?, ?>) c)
                        .filter(c -> c.getUrl().equals("name"))
                        .map(c -> resolvePathString(c.getValue(), "code"))
                        .findFirst()
                        .orElse(null);
                var exists =
                        getQuestionnaireAdapter()
                                .getExtensionsByUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .stream()
                                .anyMatch(lc -> lc.getExtension().stream()
                                        .map(c -> (IBaseExtension<?, ?>) c)
                                        .anyMatch(c -> c.getUrl().equals("name")
                                                && resolvePathString(c.getValue(), "code")
                                                        .equals(code)));
                if (!exists) {
                    getQuestionnaireAdapter().addExtension(e);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    default void addCqlLibraryExtension(String library) {
        var libraryRef = StringUtils.isNotBlank(library) ? library : getDefaultLibraryUrl();
        if (StringUtils.isNotBlank(libraryRef)
                && getExtensionsByUrl(getQuestionnaire(), Constants.CQF_LIBRARY).stream()
                        .noneMatch(e -> ((IPrimitiveType<String>) e.getValue())
                                .getValueAsString()
                                .equals(libraryRef))) {
            // ((IDomainResource) getQuestionnaire()).addExtension() .add(buildReferenceExt(getFhirVersion(),
            // cqfLibrary(libraryRef), false));
            var libraryExt = ((IDomainResource) getQuestionnaire()).addExtension();
            libraryExt.setUrl(Constants.CQF_LIBRARY);
            libraryExt.setValue(canonicalTypeForVersion(getFhirVersion(), libraryRef));
        }
    }

    default List<IBaseBackboneElement> getItems(IBase base) {
        return resolvePathList(base, "item", IBaseBackboneElement.class);
    }

    default boolean hasItems(IBase base) {
        return !getItems(base).isEmpty();
    }

    default String getItemLinkId(IBaseBackboneElement item) {
        return resolvePathString(item, "linkId");
    }
}
