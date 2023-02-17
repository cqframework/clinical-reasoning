package org.opencds.cqf.cql.evaluator;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.cql.evaluator.fhir.util.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.cql.engine.execution.Context;

public class LibraryEngine {
    private Context cqlEngine;
    private Repository repository;
    private FhirContext fhirContext;

    public LibraryEngine(FhirContext fhirContext, Repository repository) {
        this.fhirContext = fhirContext;
        this.repository = repository;
        initContext();
    }

    private void initContext() {
       cqlEngine = Contexts.forRepository(this.fhirContext, null, repository);
    }

    public Parameters evaluate(
            String url,
            String subject,
            String[] expression,
            Parameters parameters) {
        /*
        var library = repository.search(Library.class, Searches.byUrl(url)).first();
        return cqlEngine.evaluate(new VersionedIdentifier().withName(library.name).withVersion(library.version),
                expressions, parameters, new Context("Patient", subjectId));
        */
        return null;
    }



}