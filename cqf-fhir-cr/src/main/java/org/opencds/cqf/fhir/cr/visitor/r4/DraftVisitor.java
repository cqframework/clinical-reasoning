package org.opencds.cqf.fhir.cr.visitor.r4;

import java.util.ArrayList;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

public class DraftVisitor {

    private DraftVisitor() {}

    /**
     * Reverses {@link ReleaseVisitor#captureInputExpansionParams}. Restores the author-specified
     * input-exp-params back onto exp-params (overwriting any processing-time values), then removes the
     * input-exp-params contained resource and the extension referencing it. No-op if the artifact does
     * not have an input expansion parameters extension.
     */
    public static void restoreInputExpansionParams(IKnowledgeArtifactAdapter rootAdapter) {
        var rootResource = (DomainResource) rootAdapter.get();
        var inputExpansionParamsExtension = rootResource.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS);
        if (inputExpansionParamsExtension == null) {
            return;
        }

        var reference = ((Reference) inputExpansionParamsExtension.getValue()).getReference();
        var inputExpansionParams = rootResource.getContained().stream()
                .filter(contained -> reference.equals("#" + contained.getId()))
                .filter(Parameters.class::isInstance)
                .map(Parameters.class::cast)
                .findFirst();

        // exp-params is set if input-exp-params is actually found
        inputExpansionParams.ifPresent(inputParams -> {
            var expansionParams =
                    (Parameters) rootAdapter.getExpansionParameters().orElseThrow();
            expansionParams.setParameter(new ArrayList<>(inputParams.getParameter()));
        });

        // cleanup of input-exp-params
        rootResource.getContained().removeIf(contained -> reference.equals("#" + contained.getId()));
        rootResource.getExtension().removeIf(ext -> Constants.CQF_INPUT_EXPANSION_PARAMETERS.equals(ext.getUrl()));
    }
}
