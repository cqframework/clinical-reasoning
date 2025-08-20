package org.opencds.cqf.fhir.cr.hapi.common;

import static ca.uhn.fhir.context.FhirVersionEnum.R4;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElmCacheResourceChangeListenerTest {

    @Mock
    private DaoRegistry myDaoRegistry;

    @Mock
    private IFhirResourceDao<?> myLibraryDao;

    @Mock
    private Map<VersionedIdentifier, CompiledLibrary> myGlobalLibraryCache;

    ElmCacheResourceChangeListener myListener;

    private final IIdType myLibraryId = IdHelper.getIdType(R4, "Library", "1").withVersion("1");
    private final IIdType myPatientId = IdHelper.getIdType(R4, "Patient", "2");

    @BeforeEach
    void beforeEach() {
        when(myDaoRegistry.getResourceDao("Library")).thenReturn(myLibraryDao);
        when(myDaoRegistry.getFhirContext()).thenReturn(FhirContext.forR4());

        myListener = new ElmCacheResourceChangeListener(myDaoRegistry, myGlobalLibraryCache);
    }

    @Test
    void testNonExceptionGeneratingInvalidScenarios_isIdempotent() {
        myListener.handleChange(null);

        assertNoInteractions();

        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(null);

        myListener.handleChange(resourceChangeEvent);
        assertNoInteractions();

        myListener.handleInit(null);
        assertNoInteractions();
    }

    @Test
    void testHandleChange_whenResourceNotALibrary_isIdempotent() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myPatientId));

        myListener.handleChange(resourceChangeEvent);

        assertNoInteractions();
    }

    @Test
    void testHandleChange_whenLibraryResourceNotFound_clearsLibraryCache() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myLibraryId));
        when(myLibraryDao.read(eq(myLibraryId), any())).thenThrow(new ResourceNotFoundException("Not found"));

        myListener.handleChange(resourceChangeEvent);

        Mockito.verify(myGlobalLibraryCache).clear();
    }

    @Test
    void testHandleChange_whenResourceIsLibrary_removesResourceFromCache() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myLibraryId));

        Library library = new Library().setName("Library").setVersion(myLibraryId.getVersionIdPart());
        when(myLibraryDao.read(eq(myLibraryId), any())).thenReturn(library);

        VersionedIdentifier vid =
                new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion());

        myListener.handleChange(resourceChangeEvent);

        Mockito.verify(myGlobalLibraryCache).remove(vid);
    }

    private void assertNoInteractions() {
        verifyNoInteractions(myLibraryDao);
        verifyNoInteractions(myGlobalLibraryCache);
    }
}
