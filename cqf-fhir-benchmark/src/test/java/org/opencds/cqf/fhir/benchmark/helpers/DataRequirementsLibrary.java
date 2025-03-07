package org.opencds.cqf.fhir.benchmark.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public class DataRequirementsLibrary {
    final IBaseResource library;
    final ILibraryAdapter libraryAdapter;

    public DataRequirementsLibrary(IBaseResource library) {
        this.library = library;
        libraryAdapter = (ILibraryAdapter) IAdapterFactory.createAdapterForResource(library);
    }

    public DataRequirementsLibrary hasDataRequirements(int count) {
        assertEquals(count, libraryAdapter.getDataRequirement().size());
        return this;
    }

    public IBaseResource getLibrary() {
        return library;
    }
}
