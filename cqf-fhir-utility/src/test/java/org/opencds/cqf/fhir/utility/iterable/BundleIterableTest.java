package org.opencds.cqf.fhir.utility.iterable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class BundleIterableTest {

    private IRepository mockRepo() {
        var repo = mock(IRepository.class);
        when(repo.fhirContext()).thenReturn(FhirContext.forR4Cached());
        return repo;
    }

    @Test
    void emptyBundleIterator() {
        var repo = mockRepo();
        var bundle = new Bundle();
        var iter = new BundleIterator<>(repo, bundle);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    void singleEntryIterator() {
        var repo = mockRepo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("p1"));
        var iter = new BundleIterator<>(repo, bundle);
        assertTrue(iter.hasNext());
        var entry = iter.next();
        assertEquals("p1", entry.getResource().getIdElement().getIdPart());
        assertFalse(iter.hasNext());
    }

    @Test
    void iteratorWithPaging() {
        var repo = mockRepo();
        var bundle1 = new Bundle();
        bundle1.addEntry().setResource(new Patient().setId("p1"));
        bundle1.addLink().setRelation("next").setUrl("http://example.com/next");

        var bundle2 = new Bundle();
        bundle2.addEntry().setResource(new Patient().setId("p2"));
        when(repo.link(eq(Bundle.class), any(String.class))).thenReturn(bundle2);

        var iter = new BundleIterator<>(repo, bundle1);
        var results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next().getResource().getIdElement().getIdPart());
        }
        assertEquals(2, results.size());
    }

    @Test
    void iteratorWithNullNextBundle() {
        var repo = mockRepo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("p1"));
        bundle.addLink().setRelation("next").setUrl("http://example.com/next");
        when(repo.link(eq(Bundle.class), any(String.class))).thenReturn(null);

        var iter = new BundleIterator<>(repo, bundle);
        assertTrue(iter.hasNext());
        iter.next();
        assertFalse(iter.hasNext());
    }

    @Test
    void bundleIterableForEach() {
        var repo = mockRepo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("p1"));
        bundle.addEntry().setResource(new Patient().setId("p2"));

        var iterable = new BundleIterable<>(repo, bundle);
        var ids = new ArrayList<String>();
        for (var entry : iterable) {
            ids.add(entry.getResource().getIdElement().getIdPart());
        }
        assertEquals(2, ids.size());
    }

    @Test
    void bundleIterableToStream() {
        var repo = mockRepo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("p1"));
        var iterable = new BundleIterable<>(repo, bundle);
        assertEquals(1, iterable.toStream().count());
    }

    @Test
    void bundleMappingIterator() {
        var repo = mockRepo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("p1"));
        var mappingIter = new BundleMappingIterator<>(
                repo, bundle, e -> e.getResource().getIdElement().getIdPart());
        assertTrue(mappingIter.hasNext());
        assertEquals("p1", mappingIter.next());
        assertFalse(mappingIter.hasNext());
        assertThrows(NoSuchElementException.class, mappingIter::next);
    }

    @Test
    void bundleMappingIterableToStream() {
        var repo = mockRepo();
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("p1"));
        var iterable = new BundleMappingIterable<>(
                repo, bundle, e -> e.getResource().getIdElement().getIdPart());
        assertEquals(1, iterable.toStream().count());
    }

    @Test
    void streamIterable() {
        var stream = Stream.of("a", "b", "c");
        var iterable = new StreamIterable<>(stream);
        var results = new ArrayList<String>();
        iterable.iterator().forEachRemaining(results::add);
        assertEquals(3, results.size());
    }
}
