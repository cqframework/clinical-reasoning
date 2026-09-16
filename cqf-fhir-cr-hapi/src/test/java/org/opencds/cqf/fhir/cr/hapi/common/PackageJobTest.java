package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.common.PackageJob.Snapshot;
import org.opencds.cqf.fhir.cr.hapi.common.PackageJob.Status;

class PackageJobTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-02T10:15:30Z");
    private static final Instant FINISHED_AT = Instant.parse("2026-07-02T10:16:45Z");

    @Test
    void constructor_storesIdAndCreatedAt() {
        var job = new PackageJob("job-1", CREATED_AT);

        assertEquals("job-1", job.getId());
        assertSame(CREATED_AT, job.getCreatedAt());
    }

    @Test
    void initialSnapshot_isAcceptedAndNotDone() {
        var job = new PackageJob("job-1", CREATED_AT);

        var snapshot = job.snapshot();
        assertEquals(Status.ACCEPTED, snapshot.status());
        assertNull(snapshot.result());
        assertNull(snapshot.error());
        assertNull(snapshot.finishedAt());
        assertFalse(job.isDone());
        assertFalse(snapshot.isDone());
    }

    @Test
    void markInProgress_isInProgressAndNotDone() {
        var job = new PackageJob("job-1", CREATED_AT);

        job.markInProgress();

        var snapshot = job.snapshot();
        assertEquals(Status.IN_PROGRESS, snapshot.status());
        assertNull(snapshot.result());
        assertNull(snapshot.error());
        assertNull(snapshot.finishedAt());
        assertFalse(job.isDone());
        assertFalse(snapshot.isDone());
    }

    @Test
    void markCompleted_capturesResultAndFinishedAt_andIsDone() {
        var job = new PackageJob("job-1", CREATED_AT);
        var bundle = new Bundle();

        job.markCompleted(bundle, FINISHED_AT);

        var snapshot = job.snapshot();
        assertEquals(Status.COMPLETED, snapshot.status());
        assertSame(bundle, snapshot.result());
        assertNull(snapshot.error());
        assertSame(FINISHED_AT, snapshot.finishedAt());
        assertTrue(job.isDone());
        assertTrue(snapshot.isDone());
    }

    @Test
    void markFailed_capturesErrorAndFinishedAt_andIsDone() {
        var job = new PackageJob("job-1", CREATED_AT);

        job.markFailed("boom", FINISHED_AT);

        var snapshot = job.snapshot();
        assertEquals(Status.FAILED, snapshot.status());
        assertNull(snapshot.result());
        assertEquals("boom", snapshot.error());
        assertSame(FINISHED_AT, snapshot.finishedAt());
        assertTrue(job.isDone());
        assertTrue(snapshot.isDone());
    }

    @Test
    void snapshotIsDone_trueOnlyForTerminalStatuses() {
        assertFalse(new Snapshot(Status.ACCEPTED, null, null, null).isDone());
        assertFalse(new Snapshot(Status.IN_PROGRESS, null, null, null).isDone());
        assertTrue(new Snapshot(Status.COMPLETED, new Bundle(), null, FINISHED_AT).isDone());
        assertTrue(new Snapshot(Status.FAILED, null, "boom", FINISHED_AT).isDone());
    }
}
