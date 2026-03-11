# Architecture Guidelines

This document defines architectural principles for the clinical-reasoning library, with emphasis on module boundaries and domain purity.

## Module Dependency Graph

```
cqf-fhir-utility          (FHIR adapters, repositories, helpers)
    ↑
cqf-fhir-cql              (CQL engine integration, LibraryEngine)
    ↑
cqf-fhir-cr               (clinical reasoning operations: Measure, PlanDefinition, Questionnaire, etc.)
    ↑                ↑
cqf-fhir-cr-hapi      cqf-fhir-cr-cli
(HAPI FHIR server      (command-line interface,
 integration layer)      no REST context)
```

Arrows point from dependee to depender. A class should only reference concepts appropriate to the module it lives in.

## Domain Boundary Purity

### The Principle

As you cross boundaries in an application, the primary domain model shifts. Each module has its own "frame" — the set of domain concepts that are natural and comprehensible within that module:

| Module | Frame | Primary Domain Concepts |
|--------|-------|------------------------|
| `cqf-fhir-cr` | Clinical reasoning algorithms | FHIR resources, CQL expressions, Measure scoring, PlanDefinition logic |
| `cqf-fhir-cql` | CQL evaluation | CQL libraries, evaluation contexts, terminology |
| `cqf-fhir-utility` | FHIR infrastructure | Adapters, repositories, search, canonicals |
| `cqf-fhir-cr-hapi` | HAPI FHIR server integration | REST operations, request handling, HTTP semantics |
| `cqf-fhir-cr-cli` | Command-line execution | CLI arguments, file I/O, exit codes |

**Rule: Keep the number of domain concepts small within each module.** A module that mixes clinical reasoning logic with HTTP status codes and REST exception types becomes harder to understand, test, and reuse.

**Rule: Transition between frames happens at boundaries.** The boundary surface — where one module's API is integrated into another — is where translation logic belongs. Keep all transition logic colocated at the boundary.

### Forbidden Imports in Core Modules

The following modules MUST NOT import from `ca.uhn.fhir.rest.server.exceptions`:

- `cqf-fhir-cr`
- `cqf-fhir-cql`
- `cqf-fhir-utility`

These modules are used by the CLI, by Android clients, and by other non-REST consumers. REST/HTTP concepts do not belong here.

### Exception Handling Pattern

**Core modules** throw domain-appropriate exceptions:

- `IllegalArgumentException` — caller passed invalid input (null, empty, wrong type)
- `IllegalStateException` — an internal invariant was violated (missing configuration, inconsistent state)
- `NullPointerException` via `Objects.requireNonNull()` — for required non-null parameters
- `UnsupportedOperationException` — for operations not supported in a given context/version

These are standard Java exceptions that communicate *what went wrong* without encoding *how to respond to a client*.

**Integration modules** (`cqf-fhir-cr-hapi`) catch domain exceptions and translate them to REST exceptions at the boundary:

```java
// CORRECT: Translation at the boundary (in cqf-fhir-cr-hapi)
@Operation(name = "$evaluate-measure", type = Measure.class)
public MeasureReport evaluateMeasure(...) {
    try {
        return measureProcessor.evaluateMeasure(request);
    } catch (IllegalArgumentException e) {
        throw new InvalidRequestException(e.getMessage(), e);    // HTTP 400
    } catch (IllegalStateException e) {
        throw new InternalErrorException(e.getMessage(), e);     // HTTP 500
    }
}
```

```java
// WRONG: REST exceptions thrown from core logic (in cqf-fhir-cr)
public class CqlEvaluationRequest {
    public CqlEvaluationRequest(...) {
        if (expression == null && content == null) {
            // BAD: This class is used by CLI too — it should not know about HTTP 400
            throw new InvalidRequestException("expression or content required");
        }
    }
}
```

```java
// CORRECT: Domain exception from core logic (in cqf-fhir-cr)
public class CqlEvaluationRequest {
    public CqlEvaluationRequest(...) {
        if (expression == null && content == null) {
            // GOOD: Standard Java — the boundary layer translates this to HTTP 400
            throw new IllegalArgumentException(
                "The $cql operation requires the expression parameter and/or content parameter");
        }
    }
}
```

### Where the Boundary Lives

For clinical-reasoning, the boundary is `cqf-fhir-cr-hapi`. Every operation provider class in this module is a boundary surface. These providers call into `cqf-fhir-cr` processors/services and translate exceptions. The translation can be centralized via a shared utility or handled per-provider.

### Centralized Translation Utility

To avoid repetitive try/catch blocks, use a shared boundary helper in `cqf-fhir-cr-hapi`:

```java
package org.opencds.cqf.fhir.cr.hapi.common;

public final class CrExceptionTranslator {
    private CrExceptionTranslator() {}

    public static <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        } catch (UnsupportedOperationException e) {
            throw new NotImplementedOperationException(e.getMessage(), e);
        }
        // IllegalStateException and other RuntimeExceptions intentionally not caught:
        // HAPI RestfulServer wraps them as InternalErrorException (HTTP 500) by default
    }
}
```

Usage in providers:

```java
@Operation(name = "$evaluate-measure", type = Measure.class)
public MeasureReport evaluateMeasure(...) {
    return CrExceptionTranslator.execute(
        () -> factory.create(requestDetails).evaluateMeasure(request));
}
```

### Why Not Just Let RestfulServer Handle It?

HAPI's `RestfulServer` already wraps unknown exceptions as `InternalErrorException` (HTTP 500). Explicit translation is still needed because:

1. **Client errors become 400s instead of 500s.** An `IllegalArgumentException` for bad user input should be HTTP 400, not 500. Only explicit translation achieves this.
2. **OperationOutcome messages are preserved.** REST exceptions include the message in the FHIR OperationOutcome response body. Generic wrapping may lose context.
3. **CLI and non-REST consumers get clean stack traces.** Domain exceptions are standard Java — they work naturally in any context.

## Enforcing the Boundary

### ArchUnit Test

Add an ArchUnit test in `cqf-fhir-cr` to prevent REST exception imports in core modules:

```java
@AnalyzeClasses(packages = "org.opencds.cqf.fhir.cr")
class ArchitectureTest {
    @ArchTest
    static final ArchRule coreModuleMustNotUseRestExceptions =
        noClasses()
            .that().resideInAPackage("org.opencds.cqf.fhir.cr..")
            .should().dependOnClassesThat()
            .resideInAPackage("ca.uhn.fhir.rest.server.exceptions..");
}
```
