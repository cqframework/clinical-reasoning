package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;

public interface r4AdapterFactory extends AdapterFactory {
    default IBaseLibraryAdapter createLibrary(IBaseResource library) {
        return new LibraryAdapter(library);
    }
    public r4LibraryAdapter createLibrary(Library library);
}
