package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.visitor.DataRequirementsVisitor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class DataRequirementsProcessor implements IDataRequirementsProcessor {
    protected final Repository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final DataRequirementsVisitor dataRequirementsVisitor;

    public DataRequirementsProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public DataRequirementsProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        dataRequirementsVisitor = new DataRequirementsVisitor(this.repository, evaluationSettings);
    }

    @Override
    public IBaseResource getDataRequirements(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseResource) dataRequirementsVisitor.visit(
                IAdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter((IDomainResource) resource),
                parameters == null ? newParameters(repository.fhirContext()) : parameters);
    }
}
