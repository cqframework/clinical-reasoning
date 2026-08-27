package org.opencds.cqf.fhir.cr.hapi.common;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * State for a single asynchronous {@code $package} invocation, shared between the request thread
 * (which creates and polls it) and a worker thread (which executes the operation and records the
 * outcome).
 *
 * <p>All mutable state is held in a single immutable {@link Snapshot} published through one
 * {@link AtomicReference}. Each transition swaps in a new snapshot atomically, so a reader always
 * observes a consistent {@code (status, result, error, finishedAt)} combination.
 */
public class PackageJob {

    public enum Status {
        /** Accepted and queued, not yet started. */
        ACCEPTED,
        /** Currently executing on a worker thread. */
        IN_PROGRESS,
        /** Finished successfully; {@link Snapshot#result()} holds the packaged Bundle. */
        COMPLETED,
        /** Finished with an error; {@link Snapshot#error()} holds the message. */
        FAILED
    }

    /** Immutable point-in-time view of the job's mutable state. */
    public record Snapshot(Status status, IBaseBundle result, String error, Instant finishedAt) {
        public boolean isDone() {
            return status == Status.COMPLETED || status == Status.FAILED;
        }
    }

    private final String id;
    private final Instant createdAt;
    private final AtomicReference<Snapshot> snapshot =
            new AtomicReference<>(new Snapshot(Status.ACCEPTED, null, null, null));

    public PackageJob(String id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** The current consistent snapshot of the job's state. */
    public Snapshot snapshot() {
        return snapshot.get();
    }

    public boolean isDone() {
        return snapshot.get().isDone();
    }

    void markInProgress() {
        snapshot.set(new Snapshot(Status.IN_PROGRESS, null, null, null));
    }

    void markCompleted(IBaseBundle result, Instant finishedAt) {
        snapshot.set(new Snapshot(Status.COMPLETED, result, null, finishedAt));
    }

    void markFailed(String error, Instant finishedAt) {
        snapshot.set(new Snapshot(Status.FAILED, null, error, finishedAt));
    }
}
