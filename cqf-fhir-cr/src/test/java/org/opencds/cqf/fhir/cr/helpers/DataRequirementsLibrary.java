package org.opencds.cqf.fhir.cr.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;

public class DataRequirementsLibrary {
    final IBaseResource library;
    final LibraryAdapter libraryAdapter;

    public DataRequirementsLibrary(IBaseResource library) {
        this.library = library;
        libraryAdapter = (LibraryAdapter) AdapterFactory.createAdapterForResource(library);
    }

    public DataRequirementsLibrary hasDataRequirements(int count) {
        assertEquals(count, libraryAdapter.getDataRequirement().size());
        return this;
    }
}
