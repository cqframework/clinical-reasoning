package org.opencds.cqf.fhir.cr.hapi.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CqlThreadFactoryTest {

    @Test
    void testNewThread() throws InterruptedException {
        final List<String> strings = new ArrayList<>();
        final Runnable runnable = () -> strings.add("one");
        final CqlThreadFactory cqlThreadFactory = new CqlThreadFactory();
        final Thread thread = cqlThreadFactory.newThread(runnable);
        assertNotNull(thread);
        assertTrue(strings.isEmpty());
        thread.start();
        thread.join();
        assertEquals(1, strings.size());
        assertThat(strings, contains("one"));
    }
}
