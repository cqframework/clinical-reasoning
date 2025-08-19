package org.opencds.cqf.fhir.cr.hapi.common;

import static ca.uhn.fhir.context.FhirVersionEnum.R4;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.runtime.Code;

@ExtendWith(MockitoExtension.class)
class CodeCacheResourceChangeListenerTest {

    @Mock
    private DaoRegistry myDaoRegistry;

    @Mock
    private IFhirResourceDao<?> myValueSetDao;

    @Mock
    private Map<String, List<Code>> myGlobalValueSetCache;

    private final IIdType myValueSetId = IdHelper.getIdType(R4, "ValueSet", "1");
    private final IIdType myPatientId = IdHelper.getIdType(R4, "Patient", "2");
    private CodeCacheResourceChangeListener myListener;

    @BeforeEach
    void beforeEach() {
        when(myDaoRegistry.getResourceDao("ValueSet")).thenReturn(myValueSetDao);
        when(myDaoRegistry.getFhirContext()).thenReturn(FhirContext.forR4());

        myListener = new CodeCacheResourceChangeListener(myDaoRegistry, myGlobalValueSetCache);
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
    void testHandleChange_whenResourceNotValueSet_isIdempotent() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myPatientId));

        myListener.handleChange(resourceChangeEvent);

        assertNoInteractions();
    }

    @Test
    void testHandleChange_whenValueSetResourceNotFoundInRepository_clearsValueSetCache() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myValueSetId));
        when(myValueSetDao.read(eq(myValueSetId.toUnqualifiedVersionless()), any()))
                .thenThrow(new ResourceNotFoundException("Not found"));

        myListener.handleChange(resourceChangeEvent);

        Mockito.verify(myGlobalValueSetCache).clear();
    }

    @Test
    void testHandleChange_whenValueSetNotFoundInCache() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myValueSetId));

        ValueSet valueSet = new ValueSet().setUrl("acme.org/myValueset");

        when(myValueSetDao.read(eq(myValueSetId.toUnqualifiedVersionless()), any()))
                .thenReturn(valueSet);
        when(myGlobalValueSetCache.keySet()).thenReturn(Set.of("http://some.other.valueset.url"));

        myListener.handleChange(resourceChangeEvent);

        Mockito.verify(myGlobalValueSetCache, never()).remove(any());
    }

    @Test
    void testHandleChange_whenResourceIsValueSet_removesResourceFromCache() {
        IResourceChangeEvent resourceChangeEvent = Mockito.mock(IResourceChangeEvent.class);
        when(resourceChangeEvent.getUpdatedResourceIds()).thenReturn(List.of(myValueSetId));

        ValueSet valueSet = new ValueSet().setUrl("acme.org/myValueset");

        when(myValueSetDao.read(eq(myValueSetId.toUnqualifiedVersionless()), any()))
                .thenReturn(valueSet);
        when(myGlobalValueSetCache.keySet()).thenReturn(Set.of(valueSet.getUrl()));

        myListener.handleChange(resourceChangeEvent);

        Mockito.verify(myGlobalValueSetCache).remove(valueSet.getUrl());
    }

    private void assertNoInteractions() {
        verifyNoInteractions(myValueSetDao);
        verifyNoInteractions(myGlobalValueSetCache);
    }
}
