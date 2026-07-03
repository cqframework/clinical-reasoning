package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class PackageJobServiceTest {

    private static final long TIMEOUT_MS = 5_000;
    private static final long POLL_MS = 20;

    /** Poll until the job finishes; fail if it does not complete within the timeout. */
    private static void awaitDone(PackageJobService service, String jobId) {
        var deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            var job = service.get(jobId);
            if (job != null && job.isDone()) {
                return;
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrupted while waiting for job " + jobId);
            }
        }
        fail("job " + jobId + " did not finish within " + TIMEOUT_MS + "ms");
    }

    @Test
    void submitAndComplete_reachesCompletedWithSuppliedBundle() {
        var service = new PackageJobService(1, Duration.ofMinutes(1));
        try {
            var bundle = new Bundle();
            Supplier<IBaseBundle> work = () -> bundle;

            var id = service.submit(work);
            assertNotNull(id);

            var job = service.get(id);
            assertNotNull(job);

            awaitDone(service, id);

            var snapshot = service.get(id).snapshot();
            assertEquals(PackageJob.Status.COMPLETED, snapshot.status());
            assertSame(bundle, snapshot.result());
            assertNull(snapshot.error());
            assertNotNull(snapshot.finishedAt());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void workThatThrowsWithMessage_endsFailedWithThatMessage() {
        var service = new PackageJobService(1, Duration.ofMinutes(1));
        try {
            Supplier<IBaseBundle> work = () -> {
                throw new RuntimeException("problem");
            };

            var id = service.submit(work);
            awaitDone(service, id);

            var snapshot = service.get(id).snapshot();
            assertEquals(PackageJob.Status.FAILED, snapshot.status());
            assertEquals("problem", snapshot.error());
            assertNull(snapshot.result());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void workThatThrowsWithNullMessage_endsFailedWithSimpleClassName() {
        var service = new PackageJobService(1, Duration.ofMinutes(1));
        try {
            Supplier<IBaseBundle> work = () -> {
                throw new RuntimeException();
            };

            var id = service.submit(work);
            awaitDone(service, id);

            var snapshot = service.get(id).snapshot();
            assertEquals(PackageJob.Status.FAILED, snapshot.status());
            assertEquals("RuntimeException", snapshot.error());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void getUnknownId_returnsNull() {
        var service = new PackageJobService(1, Duration.ofMinutes(1));
        try {
            assertNull(service.get("no-such-job"));
        } finally {
            service.shutdown();
        }
    }

    @Test
    void defaultConstructor_canSubmitAndComplete() {
        var service = new PackageJobService();
        try {
            var bundle = new Bundle();
            var id = service.submit(() -> bundle);
            awaitDone(service, id);

            var snapshot = service.get(id).snapshot();
            assertEquals(PackageJob.Status.COMPLETED, snapshot.status());
            assertSame(bundle, snapshot.result());
        } finally {
            service.shutdown();
        }
    }

    @Test
    void submit_purgesExpiredFinishedJobs() throws Exception {
        var service = new PackageJobService(1, Duration.ofMillis(1));
        try {
            var firstId = service.submit(() -> new Bundle());
            awaitDone(service, firstId);
            assertNotNull(service.get(firstId));

            // Let the first job age past the (very short) retention window.
            Thread.sleep(20);

            // submit() triggers purgeExpired(), which should evict the aged, finished first job.
            var secondId = service.submit(() -> new Bundle());

            assertNull(service.get(firstId), "expired finished job should have been purged");
            assertNotNull(service.get(secondId), "freshly submitted job should be present");

            awaitDone(service, secondId);
        } finally {
            service.shutdown();
        }
    }

    @Test
    void purge_keepsRunningAndRecentlyFinishedJobs() throws Exception {
        // Long retention so a finished job stays "recent" (isBefore(cutoff) == false).
        var service = new PackageJobService(1, Duration.ofHours(1));
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        try {
            // A finished-but-recent job.
            var finishedId = service.submit(() -> new Bundle());
            awaitDone(service, finishedId);

            // A job that blocks the single worker, so it stays IN_PROGRESS (isDone() == false).
            var runningId = service.submit(() -> {
                started.countDown();
                try {
                    release.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new Bundle();
            });
            assertTrue(started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "blocking job should start");

            // submit() triggers purgeExpired() while one job is finished-but-recent and another is
            // still running; neither is eligible for eviction.
            var thirdId = service.submit(() -> new Bundle());

            assertNotNull(service.get(finishedId), "recently finished job should be kept");
            assertNotNull(service.get(runningId), "in-progress job should be kept");
            assertNotNull(service.get(thirdId));
        } finally {
            release.countDown();
            service.shutdown();
        }
    }

    @Test
    void shutdown_isSafeToCallTwice() {
        var service = new PackageJobService(1, Duration.ofMinutes(1));
        service.shutdown();
        // A second shutdown() must not throw.
        service.shutdown();
        assertTrue(true);
    }
}
