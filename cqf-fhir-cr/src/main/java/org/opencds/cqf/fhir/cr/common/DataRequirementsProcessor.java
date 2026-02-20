package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.visitor.DataRequirementsVisitor;
import org.opencds.cqf.fhir.cr.visitor.PublishStrategy;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class DataRequirementsProcessor implements IDataRequirementsProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DataRequirementsProcessor.class);

    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final DataRequirementsVisitor dataRequirementsVisitor;
    private IPublishProcessor publishProcessor;

    public DataRequirementsProcessor(IRepository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public DataRequirementsProcessor(IRepository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        dataRequirementsVisitor = new DataRequirementsVisitor(this.repository, evaluationSettings);
    }

    /**
     * Sets the package downloader for the data requirements visitor.
     * Public to allow tests to inject a mock downloader.
     */
    public void setPackageDownloader(org.opencds.cqf.fhir.cr.visitor.PackageDownloader packageDownloader) {
        dataRequirementsVisitor.setPackageDownloader(packageDownloader);
    }

    /**
     * Sets the publish processor for persisting dependencies.
     * Public to allow injection of custom processor (e.g., for testing).
     */
    public void setPublishProcessor(IPublishProcessor publishProcessor) {
        this.publishProcessor = publishProcessor;
    }

    @Override
    public IBaseResource getDataRequirements(IBaseResource resource, IBaseParameters parameters) {
        return getDataRequirements(resource, parameters, false);
    }

    @Override
    public IBaseResource getDataRequirements(
            IBaseResource resource, IBaseParameters parameters, boolean persistDependencies) {
        // Execute data requirements analysis
        IBaseResource moduleDefinitionLibrary = (IBaseResource) dataRequirementsVisitor.visit(
                IAdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter((IDomainResource) resource),
                parameters == null ? newParameters(repository.fhirContext()) : parameters);

        // If persistDependencies is true, persist collected resources using $publish operation
        if (persistDependencies) {
            logger.info("persistDependencies=true, initiating persistence of collected resources");

            List<IBaseResource> collectedResources = dataRequirementsVisitor.getCollectedResources();

            if (collectedResources.isEmpty()) {
                logger.warn("No resources collected for persistence - this may indicate missing package data");
            } else {
                try {
                    // Use PublishProcessor if provided, otherwise create one
                    IPublishProcessor processor =
                            publishProcessor != null ? publishProcessor : new PublishProcessor(repository);

                    // Create publish strategy with default configuration
                    PublishStrategy strategy = new PublishStrategy(repository, processor);

                    // Persist IG and dependencies (IG should be first in collected resources)
                    IBaseResource implementationGuide = resource; // The IG resource we're processing
                    PublishStrategy.PublishResult result = strategy.publish(implementationGuide, collectedResources);

                    if (result.isSuccess()) {
                        logger.info(
                                "Successfully persisted {} resources for ImplementationGuide",
                                result.getResourceCount());
                    } else {
                        logger.error("Failed to persist dependencies: {}", result.getErrorMessage());
                        throw new RuntimeException("Dependency persistence failed: " + result.getErrorMessage());
                    }
                } catch (PublishStrategy.PublishException e) {
                    logger.error("Error persisting dependencies: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to persist ImplementationGuide dependencies", e);
                }
            }
        }

        return moduleDefinitionLibrary;
    }
}
