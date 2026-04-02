package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class CreateChangelogProcessor implements ICreateChangelogProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CreateChangelogProcessor.class);
    private final FhirVersionEnum fhirVersion;
    private final IAdapterFactory adapterFactory;

    public CreateChangelogProcessor(IRepository repository) {
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
        this.adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
    }

    @Override
    public IBaseResource createChangelog(
            IBaseResource source, IBaseResource target, IBaseResource terminologyEndpoint) {
        logger.info("Unable to perform $create-changelog outside of HAPI context");
        return adapterFactory
                .createParameters((IBaseParameters) Resources.newBaseForVersion("Parameters", this.fhirVersion))
                .get();
    }
}
