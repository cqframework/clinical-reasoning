package org.opencds.cqf.fhir.cr.hapi.common;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory registry and executor for asynchronous {@code $package} jobs.
 *
 * <p>This implements the server side of the
 * <a href="https://hl7.org/fhir/async.html">FHIR Asynchronous Request Pattern</a> for the
 * {@code $package} operation: work is submitted here, executed on a bounded worker pool, and the
 * resulting Bundle (or error) is retained for later retrieval by the {@code $package-status} poll
 * endpoint.
 *
 * <p>State is held in a {@link ConcurrentHashMap} and is therefore process-local.
 */
public class PackageJobService {

    private static final Logger ourLog = LoggerFactory.getLogger(PackageJobService.class);

    private static final int DEFAULT_THREAD_COUNT = 10;
    private static final Duration DEFAULT_RETENTION = Duration.ofHours(1);
    private static final Duration SHUTDOWN_GRACE = Duration.ofSeconds(30);

    private final ConcurrentHashMap<String, PackageJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final Duration retention;

    public PackageJobService() {
        this(DEFAULT_THREAD_COUNT, DEFAULT_RETENTION);
    }

    public PackageJobService(int threadCount, Duration retention) {
        this.retention = retention;
        this.executor = Executors.newFixedThreadPool(threadCount, runnable -> {
            var thread = new Thread(runnable, "package-async-worker");
            // Daemon threads so a forgotten job never blocks JVM shutdown. Lifecycle cleanup is
            // driven by {@link #shutdown()} (invoked by Spring via @PreDestroy on context close),
            // rather than a JVM shutdown hook — a hook would pin this instance for the life of the
            // JVM and would not run on context close/redeploy, leaking the pool.
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Submit package work for asynchronous execution.
     *
     * @param work produces the packaged Bundle; must be self-contained (it runs on a worker thread
     *     after the originating request has returned, so it must not reference the live request or
     *     response objects)
     * @return the generated job id, used to build the poll URL
     */
    public String submit(Supplier<IBaseBundle> work) {
        purgeExpired();
        var id = UUID.randomUUID().toString();
        var job = new PackageJob(id, Instant.now());
        jobs.put(id, job);
        executor.submit(() -> run(job, work));
        return id;
    }

    public PackageJob get(String jobId) {
        return jobs.get(jobId);
    }

    private void run(PackageJob job, Supplier<IBaseBundle> work) {
        job.markInProgress();
        try {
            job.markCompleted(work.get(), Instant.now());
        } catch (Exception e) {
            ourLog.error("Asynchronous $package job {} failed", job.getId(), e);
            var message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            job.markFailed(message, Instant.now());
        }
    }

    /** Drop finished jobs whose results have outlived the retention window. */
    private void purgeExpired() {
        var cutoff = Instant.now().minus(retention);
        jobs.values().removeIf(job -> {
            var finishedAt = job.snapshot().finishedAt();
            return job.isDone() && finishedAt != null && finishedAt.isBefore(cutoff);
        });
    }

    /**
     * Shut down the worker pool. Invoked by Spring on context close via {@link PreDestroy}.
     */
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_GRACE.toSeconds(), TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
