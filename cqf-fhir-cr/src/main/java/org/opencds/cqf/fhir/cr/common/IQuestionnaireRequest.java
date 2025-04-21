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
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;

/**
 * This interface exposes common functionality across Operations that use Questionnaires
 */
public interface IQuestionnaireRequest extends ICqlOperationRequest {
    IBaseResource getQuestionnaire();

    IQuestionnaireAdapter getQuestionnaireAdapter();

    default void addQuestionnaireItem(IBaseBackboneElement item) {
        getModelResolver().setValue(getQuestionnaire(), "item", Collections.singletonList(item));
    }

    default <T extends IBaseExtension<?, ?>> void addLaunchContextExtensions(List<T> launchContextExts) {
        if (launchContextExts != null && !launchContextExts.isEmpty()) {
            launchContextExts.forEach(e -> {
                var code = e.getExtension().stream()
                        .map(c -> (IBaseExtension<?, ?>) c)
                        .filter(c -> c.getUrl().equals("name"))
                        .map(c -> resolvePathString(c.getValue(), "code"))
                        .findFirst()
                        .orElse(null);
                if (StringUtils.isNotBlank(code)) {
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
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    default void addCqlLibraryExtension() {
        getReferencedLibraries().values().forEach(library -> {
            if (StringUtils.isNotBlank(library)
                    && getExtensionsByUrl(getQuestionnaire(), Constants.CQF_LIBRARY).stream()
                            .noneMatch(e -> ((IPrimitiveType<String>) e.getValue())
                                    .getValueAsString()
                                    .equals(library))) {
                var libraryExt = ((IDomainResource) getQuestionnaire()).addExtension();
                libraryExt.setUrl(Constants.CQF_LIBRARY);
                libraryExt.setValue(canonicalTypeForVersion(getFhirVersion(), library));
            }
        });
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
