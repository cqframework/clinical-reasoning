package com.alphora.cql.service;

import org.opencds.cqf.cql.execution.EvaluationResult;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.execution.CqlEngine;
import org.opencds.cqf.cql.execution.LibraryLoader;

public class Service {
    public static EvaluationResult evaluate(ServiceParameters parameters) throws IOException, JAXBException {

        if (!parameters.contextParameters.isEmpty()) {
            throw new NotImplementedException("Context Parameters are not yet implemented.");
        }

        if (!parameters.parameters.isEmpty()) {
            throw new NotImplementedException("Parameters are not yet implemented.");
        }

        if (!parameters.modelUris.isEmpty()) {
            throw new NotImplementedException("Data Providers are not yet implemented.");
        }

        if (parameters.libraryPath != null) {
            throw new NotImplementedException("Libraries Path is not yet implemented.");
        }

        if (parameters.terminologyUri != null) {
            throw new NotImplementedException("Terminology Provider is not yet implemented.");
        }

        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = Helpers.toLibraryManager(modelManager, parameters.libraries);
        LibraryLoader libraryLoader = new LibraryManagerLibraryLoader(libraryManager);

        CqlEngine engine = new CqlEngine(libraryLoader);

        return engine.evaluate(Helpers.toExecutionIdentifier(parameters.libraryName, null));
    }
}