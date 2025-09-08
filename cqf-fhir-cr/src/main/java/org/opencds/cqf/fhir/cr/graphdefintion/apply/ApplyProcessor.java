package org.opencds.cqf.fhir.cr.graphdefintion.apply;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ApplyProcessor implements IApplyProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ApplyProcessor.class);
    protected final IRepository repository;
    protected final ModelResolver modelResolver;
    protected final ExtensionProcessor extensionProcessor;

    public ApplyProcessor(IRepository repository, ModelResolver modelResolver) {
        this.repository = repository;
        this.modelResolver = modelResolver;
        extensionProcessor = new ExtensionProcessor();
    }

    @Override
    public IBaseResource apply(ApplyRequest request) {

        return BundleHelper.newBundle(repository.fhirContext().getVersion().getVersion());
    }
}
