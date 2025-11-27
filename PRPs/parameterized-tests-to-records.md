# PRP: Convert Parameterized Tests to Use Record Classes

## Executive Summary

**Objective**: Refactor all JUnit 5 parameterized tests to use Java records instead of `Arguments.of()`, improving type safety, readability, and maintainability.

**Scope**:
- 23 test files across 4 Maven modules
- 40 `@ParameterizedTest` annotations
- 26 `@MethodSource` provider methods
- Zero existing record-based test patterns (greenfield establishment of new standard)

**Effort**: Systematic refactoring with immediate validation after each module

**Impact**: Establishes modern testing pattern for codebase, improves IDE support, reduces parameter ordering errors

---

## Context & Background

### Current State (Problem)

The codebase currently uses JUnit 5's `Arguments.of()` pattern for parameterized tests:

```java
private static Stream<Arguments> resolveRequestDateWithTimeParams() {
    return Stream.of(
            Arguments.of(
                    "2019-01-17T12:30:00",
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    ZoneId.systemDefault()),
            Arguments.of(
                    "2019-01-01T22:00:00.0-06:00",
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    ZoneId.of("America/Chicago"))
    );
}

@ParameterizedTest
@MethodSource("resolveRequestDateWithTimeParams")
void resolveRequestDateWithTime(
        String date, LocalDateTime expectedStartTime, LocalDateTime expectedEndTime, ZoneId zoneId) {
    var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
    assertNotNull(resolvedDateStart);
    final DateTime expectedDateStart = getDateTimeForZoneId(expectedStartTime, zoneId);
    assertDateTimesEqual(expectedDateStart, resolvedDateStart);
    // ...
}
```

**Pain Points:**
1. **No type safety**: Parameters are positional; easy to reorder incorrectly
2. **Poor readability**: `Arguments.of()` doesn't convey parameter meaning
3. **Limited IDE support**: No autocomplete for parameter names in test data
4. **Maintenance burden**: Adding/removing parameters requires careful position tracking
5. **Unclear intent**: Test data lacks semantic meaning without referring to method signature

### Desired State (Solution)

Convert to record-based parameterized tests:

```java
private record ResolveRequestDateWithTimeParams(
    String date,
    LocalDateTime expectedStartTime,
    LocalDateTime expectedEndTime,
    ZoneId zoneId
) {}

private static Stream<ResolveRequestDateWithTimeParams> resolveRequestDateWithTimeParams() {
    return Stream.of(
            new ResolveRequestDateWithTimeParams(
                    "2019-01-17T12:30:00",
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    ZoneId.systemDefault()),
            new ResolveRequestDateWithTimeParams(
                    "2019-01-01T22:00:00.0-06:00",
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    ZoneId.of("America/Chicago"))
    );
}

@ParameterizedTest
@MethodSource("resolveRequestDateWithTimeParams")
void resolveRequestDateWithTime(ResolveRequestDateWithTimeParams params) {
    var resolvedDateStart = DateHelper.resolveRequestDate(params.date(), true);
    assertNotNull(resolvedDateStart);
    final DateTime expectedDateStart = getDateTimeForZoneId(params.expectedStartTime(), params.zoneId());
    assertDateTimesEqual(expectedDateStart, resolvedDateStart);
    // ...
}
```

**Benefits:**
1. **Type safety**: Compiler enforces correct parameter types and order
2. **Named parameters**: `params.date()` is self-documenting
3. **IDE support**: Full autocomplete on `params.<TAB>`
4. **Easier refactoring**: Add/remove fields in one place (the record)
5. **Better errors**: Compilation failures instead of runtime test failures
6. **Modern Java**: Leverages Java 17 records (stable feature)

---

## Technical Foundation

### Environment
- **Java Version**: 17 (records fully supported, introduced in Java 14, stable in Java 16)
- **JUnit Version**: 5.10.2 (supports record-based parameters)
- **Build Tool**: Maven 3.x
- **Project Structure**: Multi-module Maven project

### Modules Affected
1. `cqf-fhir-cr` - Core clinical reasoning (most tests)
2. `cqf-fhir-cr-hapi` - HAPI FHIR integration
3. `cqf-fhir-cr-cli` - CLI module
4. `cqf-fhir-utility` - Utility module

### Java Records Primer

Records (Java 14+) are immutable data carriers with:
- Automatic constructor
- Automatic accessor methods (not getters - e.g., `name()` not `getName()`)
- Automatic `equals()`, `hashCode()`, `toString()`
- Compact, readable syntax

**Syntax:**
```java
private record TestParams(String input, int expected) {}

// Equivalent to:
private static final class TestParams {
    private final String input;
    private final int expected;

    public TestParams(String input, int expected) {
        this.input = input;
        this.expected = expected;
    }

    public String input() { return input; }
    public int expected() { return expected; }
    // + equals, hashCode, toString
}
```

**With Annotations:**
```java
private record TestParams(
    @Nullable String optionalValue,
    String requiredValue
) {}
```

---

## Implementation Blueprint

### High-Level Approach

Process modules sequentially (to validate incrementally):
1. `cqf-fhir-utility` (2 files, simplest)
2. `cqf-fhir-cr-cli` (1 file)
3. `cqf-fhir-cr` (12 files, core module)
4. `cqf-fhir-cr-hapi` (8 files, most complex)

**Per-File Strategy:**
1. Identify all `@ParameterizedTest` methods
2. For each parameterized test:
   - Create private inner record class
   - Update `@MethodSource` method to return `Stream<RecordType>`
   - Replace `Arguments.of()` with record constructor calls
   - Update test method signature to accept record parameter
   - Refactor test body to use `params.field()` accessors
3. Ensure compilation
4. Run tests for that file
5. Move to next file

### Naming Conventions

**Record Class Names:**
- Pattern: PascalCase of method name + "Params"
- Examples:
  - `resolveRequestDateWithTimeParams()` → `ResolveRequestDateWithTimeParams`
  - `getStartZonedDateTime_happyPath_params()` → `GetStartZonedDateTimeHappyPathParams`
  - `validateGroupBasisTypeErrorPathParams()` → `ValidateGroupBasisTypeErrorPathParams`

**Record Placement:**
- Location: Top of test class, after static constants and before test methods
- Order: Group all record definitions together
- Visibility: Always `private` (never used outside test class)

**Method Signature:**
- Before: `void testMethod(Type1 arg1, Type2 arg2, Type3 arg3)`
- After: `void testMethod(RecordType params)`
- Single parameter: the record instance

**Test Body Refactoring:**
- Before: `arg1`, `arg2`, `arg3`
- After: `params.arg1()`, `params.arg2()`, `params.arg3()`
- Use accessor methods, not field access

---

## Detailed Examples

### Example 1: Simple Parameters (4 parameters)

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/helper/DateHelperTest.java:22-55`

**BEFORE:**
```java
private static Stream<Arguments> resolveRequestDateWithTimeParams() {
    return Stream.of(
            Arguments.of(
                    "2019-01-17T12:30:00",
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    ZoneId.systemDefault()),
            Arguments.of(
                    "2019-01-01T22:00:00.0-06:00",
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    ZoneId.of("America/Chicago")),
            Arguments.of(
                    "2017-01-01T00:00:00.000Z",
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    ZoneOffset.UTC));
}

@ParameterizedTest
@MethodSource("resolveRequestDateWithTimeParams")
void resolveRequestDateWithTime(
        String date, LocalDateTime expectedStartTime, LocalDateTime expectedEndTime, ZoneId zoneId) {
    var resolvedDateStart = DateHelper.resolveRequestDate(date, true);
    assertNotNull(resolvedDateStart);
    final DateTime expectedDateStart = getDateTimeForZoneId(expectedStartTime, zoneId);
    assertDateTimesEqual(expectedDateStart, resolvedDateStart);

    var resolvedDateEnd = DateHelper.resolveRequestDate(date, false);
    assertNotNull(resolvedDateEnd);
    assertEquals(resolvedDateStart, resolvedDateEnd);
    final DateTime expectedDateEnd = getDateTimeForZoneId(expectedEndTime, zoneId);
    assertDateTimesEqual(expectedDateEnd, resolvedDateEnd);
}
```

**AFTER:**
```java
private record ResolveRequestDateWithTimeParams(
    String date,
    LocalDateTime expectedStartTime,
    LocalDateTime expectedEndTime,
    ZoneId zoneId
) {}

private static Stream<ResolveRequestDateWithTimeParams> resolveRequestDateWithTimeParams() {
    return Stream.of(
            new ResolveRequestDateWithTimeParams(
                    "2019-01-17T12:30:00",
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 17, 12, 30, 0),
                    ZoneId.systemDefault()),
            new ResolveRequestDateWithTimeParams(
                    "2019-01-01T22:00:00.0-06:00",
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    LocalDateTime.of(2019, Month.JANUARY, 1, 22, 0, 0),
                    ZoneId.of("America/Chicago")),
            new ResolveRequestDateWithTimeParams(
                    "2017-01-01T00:00:00.000Z",
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0, 0),
                    ZoneOffset.UTC));
}

@ParameterizedTest
@MethodSource("resolveRequestDateWithTimeParams")
void resolveRequestDateWithTime(ResolveRequestDateWithTimeParams params) {
    var resolvedDateStart = DateHelper.resolveRequestDate(params.date(), true);
    assertNotNull(resolvedDateStart);
    final DateTime expectedDateStart = getDateTimeForZoneId(params.expectedStartTime(), params.zoneId());
    assertDateTimesEqual(expectedDateStart, resolvedDateStart);

    var resolvedDateEnd = DateHelper.resolveRequestDate(params.date(), false);
    assertNotNull(resolvedDateEnd);
    assertEquals(resolvedDateStart, resolvedDateEnd);
    final DateTime expectedDateEnd = getDateTimeForZoneId(params.expectedEndTime(), params.zoneId());
    assertDateTimesEqual(expectedDateEnd, resolvedDateEnd);
}
```

**Key Changes:**
1. Created `ResolveRequestDateWithTimeParams` record with 4 fields
2. Changed return type from `Stream<Arguments>` to `Stream<ResolveRequestDateWithTimeParams>`
3. Replaced `Arguments.of()` with `new ResolveRequestDateWithTimeParams()`
4. Changed method signature from 4 parameters to 1 record parameter
5. Updated test body: `date` → `params.date()`, `expectedStartTime` → `params.expectedStartTime()`, etc.

---

### Example 2: Complex Parameters with Domain Objects (2 parameters)

**File**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4PopulationBasisValidatorTest.java:76-161`

**BEFORE:**
```java
private static Stream<Arguments> validateGroupBasisTypeHappyPathParams() {
    return Stream.of(
            Arguments.of(
                    buildGroupDef(
                            Basis.BOOLEAN,
                            buildPopulationDefs(INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                            buildStratifierDefs(
                                    EXPRESSION_INITIALPOPULATION, EXPRESSION_DENOMINATOR, EXPRESSION_NUMERATOR)),
                    buildEvaluationResult(Map.of(
                            EXPRESSION_INITIALPOPULATION,
                            Boolean.TRUE,
                            EXPRESSION_DENOMINATOR,
                            Boolean.TRUE,
                            EXPRESSION_NUMERATOR,
                            Boolean.TRUE))),
            Arguments.of(
                    buildGroupDef(
                            Basis.ENCOUNTER,
                            buildPopulationDefs(INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                            buildStratifierDefs(
                                    EXPRESSION_INITIALPOPULATION, EXPRESSION_DENOMINATOR, EXPRESSION_NUMERATOR)),
                    buildEvaluationResult(Map.of(
                            EXPRESSION_INITIALPOPULATION,
                            ENCOUNTER,
                            EXPRESSION_DENOMINATOR,
                            ENCOUNTER,
                            EXPRESSION_NUMERATOR,
                            ENCOUNTER)))
            // ... 4 more Arguments.of() calls
    );
}

@ParameterizedTest
@MethodSource("validateGroupBasisTypeHappyPathParams")
void validateGroupBasisTypeHappyPath(GroupDef groupDef, EvaluationResult evaluationResult) {
    testSubject.validateGroupPopulations(MEASURE_DEF, groupDef, evaluationResult);
}
```

**AFTER:**
```java
private record ValidateGroupBasisTypeHappyPathParams(
    GroupDef groupDef,
    EvaluationResult evaluationResult
) {}

private static Stream<ValidateGroupBasisTypeHappyPathParams> validateGroupBasisTypeHappyPathParams() {
    return Stream.of(
            new ValidateGroupBasisTypeHappyPathParams(
                    buildGroupDef(
                            Basis.BOOLEAN,
                            buildPopulationDefs(INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                            buildStratifierDefs(
                                    EXPRESSION_INITIALPOPULATION, EXPRESSION_DENOMINATOR, EXPRESSION_NUMERATOR)),
                    buildEvaluationResult(Map.of(
                            EXPRESSION_INITIALPOPULATION,
                            Boolean.TRUE,
                            EXPRESSION_DENOMINATOR,
                            Boolean.TRUE,
                            EXPRESSION_NUMERATOR,
                            Boolean.TRUE))),
            new ValidateGroupBasisTypeHappyPathParams(
                    buildGroupDef(
                            Basis.ENCOUNTER,
                            buildPopulationDefs(INITIALPOPULATION, DENOMINATOR, NUMERATOR),
                            buildStratifierDefs(
                                    EXPRESSION_INITIALPOPULATION, EXPRESSION_DENOMINATOR, EXPRESSION_NUMERATOR)),
                    buildEvaluationResult(Map.of(
                            EXPRESSION_INITIALPOPULATION,
                            ENCOUNTER,
                            EXPRESSION_DENOMINATOR,
                            ENCOUNTER,
                            EXPRESSION_NUMERATOR,
                            ENCOUNTER)))
            // ... 4 more record constructor calls
    );
}

@ParameterizedTest
@MethodSource("validateGroupBasisTypeHappyPathParams")
void validateGroupBasisTypeHappyPath(ValidateGroupBasisTypeHappyPathParams params) {
    testSubject.validateGroupPopulations(MEASURE_DEF, params.groupDef(), params.evaluationResult());
}
```

**Key Changes:**
1. Created `ValidateGroupBasisTypeHappyPathParams` with 2 complex type fields
2. Updated return type and constructor calls
3. Test body now uses `params.groupDef()` and `params.evaluationResult()`

---

### Example 3: Parameters with @Nullable Annotations

Many tests use `@Nullable` annotations on parameters for optional values or error path testing. These must be preserved on record components.

**Pattern:**
```java
// BEFORE
@ParameterizedTest
@MethodSource("someMethodParams")
void someMethod(@Nullable String optional, String required, @Nullable Integer maybeNull) {
    // test body
}

// AFTER
private record SomeMethodParams(
    @Nullable String optional,
    String required,
    @Nullable Integer maybeNull
) {}

@ParameterizedTest
@MethodSource("someMethodParams")
void someMethod(SomeMethodParams params) {
    // Use params.optional(), params.required(), params.maybeNull()
}
```

**Real Example**: `StringTimePeriodHandlerTest.java:78-161` has multiple tests with `@Nullable` timezone parameters:

```java
// BEFORE
void getStartZonedDateTime_happyPath(
        @Nullable String timezone, String theInputPeriodStart, ZonedDateTime expectedResult) {
    // ...
}

// AFTER
private record GetStartZonedDateTimeHappyPathParams(
    @Nullable String timezone,
    String theInputPeriodStart,
    ZonedDateTime expectedResult
) {}

void getStartZonedDateTime_happyPath(GetStartZonedDateTimeHappyPathParams params) {
    // Use params.timezone(), params.theInputPeriodStart(), params.expectedResult()
}
```

---

## File-by-File Implementation Plan

### Module 1: cqf-fhir-utility (2 files)

#### 1.1 TerminologyServerClientTest.java
- **Path**: `cqf-fhir-utility/src/test/java/org/opencds/cqf/fhir/utility/client/TerminologyServerClientTest.java`
- **Parameterized tests**: 1
- **Complexity**: Low (basic parameters)

#### 1.2 IgRepositoryBadDataTest.java
- **Path**: `cqf-fhir-utility/src/test/java/org/opencds/cqf/fhir/utility/repository/ig/IgRepositoryBadDataTest.java`
- **Parameterized tests**: 1
- **Complexity**: Low

**Validation after module 1:**
```bash
mvn test -pl cqf-fhir-utility
```

---

### Module 2: cqf-fhir-cr-cli (1 file)

#### 2.1 CliTest.java
- **Path**: `cqf-fhir-cr-cli/src/test/java/org/opencds/cqf/fhir/cr/cli/CliTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium (CLI argument testing)

**Validation after module 2:**
```bash
mvn test -pl cqf-fhir-cr-cli
```

---

### Module 3: cqf-fhir-cr (12 files - core module)

#### 3.1 DateHelperTest.java ⭐ REFERENCE EXAMPLE
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/helper/DateHelperTest.java`
- **Parameterized tests**: 1
- **Record**: `ResolveRequestDateWithTimeParams`
- **Complexity**: Low (4 simple parameters)
- **Note**: Use this as reference pattern for simple tests

#### 3.2 R4PopulationBasisValidatorTest.java ⭐ REFERENCE EXAMPLE
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4PopulationBasisValidatorTest.java`
- **Parameterized tests**: 3 tests
  - `validateGroupBasisTypeHappyPathParams` → `ValidateGroupBasisTypeHappyPathParams`
  - `validateGroupBasisTypeErrorPathParams` → `ValidateGroupBasisTypeErrorPathParams`
  - (one more test to identify)
- **Complexity**: High (complex domain objects, multiple test scenarios)
- **Note**: Use this as reference pattern for complex domain object tests

#### 3.3 MeasureDefBuilderTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureDefBuilderTest.java`
- **Parameterized tests**: Multiple (3-5 tests)
- **Complexity**: High (5+ parameters per test)

#### 3.4 PackageVisitorTests.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/visitor/r4/PackageVisitorTests.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 3.5 R4MeasureServiceUtilsTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/utils/R4MeasureServiceUtilsTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 3.6 R4RepositorySubjectProviderTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4RepositorySubjectProviderTest.java`
- **Parameterized tests**: 1-2
- **Complexity**: Medium (repository/data access testing)

#### 3.7 R4DateHelperTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/R4DateHelperTest.java`
- **Parameterized tests**: 1-2
- **Complexity**: Low-Medium (date/time handling)

#### 3.8 MeasureMultipleSdeTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureMultipleSdeTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 3.9 MeasureProcessorCQLParameterTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureProcessorCQLParameterTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 3.10 MeasurePeriodValidatorTest.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/common/MeasurePeriodValidatorTest.java`
- **Parameterized tests**: 1-2
- **Complexity**: Medium (validation logic)

#### 3.11 MeasureOperationParameterConverterTests.java
- **Path**: `cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/MeasureOperationParameterConverterTests.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

**Validation after module 3:**
```bash
mvn test -pl cqf-fhir-cr
```

---

### Module 4: cqf-fhir-cr-hapi (8 files - HAPI integration)

#### 4.1 StringTimePeriodHandlerTest.java ⭐ MOST COMPLEX
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/common/StringTimePeriodHandlerTest.java`
- **Parameterized tests**: 4 tests with 50+ test cases total
  - `getStartZonedDateTime_happyPath_params` → `GetStartZonedDateTimeHappyPathParams`
  - `getEndZonedDateTime_errorPaths_params` → `GetEndZonedDateTimeErrorPathsParams`
  - (2 more tests to identify)
- **Complexity**: Very High (extensive test coverage, @Nullable parameters)
- **Note**: Most test cases in the entire refactoring

#### 4.2 CrDiscoveryServiceR4Test.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/cdshooks/discovery/CrDiscoveryServiceR4Test.java`
- **Parameterized tests**: 1
- **Complexity**: Medium (CDS Hooks testing)

#### 4.3 CrDiscoveryServiceDstu3Test.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/cdshooks/discovery/CrDiscoveryServiceDstu3Test.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 4.4 CrDiscoveryServiceR5Test.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/cdshooks/discovery/CrDiscoveryServiceR5Test.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 4.5 ResponseEncoderMethodResolveIndicatorTest.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/cdshooks/ResponseEncoderMethodResolveIndicatorTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 4.6 ClinicalIntelligenceHapiFhirRepositoryTest.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/repository/ClinicalIntelligenceHapiFhirRepositoryTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

#### 4.7 IdHelperTest.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/common/IdHelperTest.java`
- **Parameterized tests**: 1
- **Complexity**: Low

#### 4.8 CanonicalHelperTest.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/common/CanonicalHelperTest.java`
- **Parameterized tests**: 1
- **Complexity**: Low

#### 4.9 CdsCrUtilsTest.java
- **Path**: `cqf-fhir-cr-hapi/src/test/java/org/opencds/cqf/fhir/cr/hapi/cdshooks/CdsCrUtilsTest.java`
- **Parameterized tests**: 1
- **Complexity**: Medium

**Validation after module 4:**
```bash
mvn test -pl cqf-fhir-cr-hapi
```

---

## Validation Strategy

### Per-File Validation
After refactoring each file:
```bash
# Compile the test file
mvn test-compile -pl <module-name>

# Run the specific test class
mvn test -Dtest=<TestClassName> -pl <module-name>
```

### Per-Module Validation
After completing all files in a module:
```bash
# Run all tests in the module
mvn test -pl <module-name>

# Apply code formatting
mvn spotless:apply -pl <module-name>

# Verify checkstyle compliance (if enabled)
mvn checkstyle:check -pl <module-name>
```

### Final Validation (All Modules)
After completing all refactoring:
```bash
# Clean build from root
mvn clean compile

# Run all tests
mvn test

# Apply spotless to entire project
mvn spotless:apply

# Verify checkstyle
mvn checkstyle:check

# Full build with all checks
mvn clean install
```

### Validation Checklist
- [ ] All test files compile without errors
- [ ] All parameterized tests pass with same results as before
- [ ] No new warnings introduced
- [ ] Spotless formatting applied successfully
- [ ] Checkstyle passes (if applicable)
- [ ] No `Arguments` imports remain in refactored files
- [ ] All `@ParameterizedTest` methods use record parameters
- [ ] All `@MethodSource` methods return `Stream<RecordType>`

---

## Gotchas & Edge Cases

### 1. @Nullable Annotations
**Issue**: Many tests use `@Nullable` on parameters for optional values or error testing.

**Solution**: Preserve `@Nullable` on record components:
```java
private record TestParams(
    @Nullable String optional,
    String required
) {}
```

**Files with @Nullable**: Approximately 30% of parameterized tests, especially:
- `StringTimePeriodHandlerTest.java` (timezone parameters)
- Error path tests across multiple files

---

### 2. Complex Parameter Types
**Issue**: Some tests use complex types (Maps, Lists, FHIR resources).

**Solution**: Records handle complex types naturally:
```java
private record ComplexParams(
    List<String> items,
    Map<String, Object> data,
    GroupDef groupDef
) {}
```

**No special handling needed** - records accept any type.

---

### 3. Many Parameters (6+)
**Issue**: Some tests have 6-12 parameters, making records verbose.

**Solution**: This is actually a **benefit** of records - they make the parameter set explicit and type-safe. Do not try to "optimize" by grouping parameters artificially.

**Example**: `MeasureDefBuilderTest.java` has tests with 5+ parameters - keep them all in the record.

---

### 4. Multiple Parameterized Tests in Same Class
**Issue**: Many test classes have 2-4 parameterized tests, each needing its own record.

**Solution**: Create one record per parameterized test, named after the method:
```java
class MyTest {
    private record FirstTestParams(...) {}
    private record SecondTestParams(...) {}
    private record ThirdTestParams(...) {}

    @ParameterizedTest
    @MethodSource("firstTestParams")
    void firstTest(FirstTestParams params) { ... }

    @ParameterizedTest
    @MethodSource("secondTestParams")
    void secondTest(SecondTestParams params) { ... }
}
```

**Files with multiple tests**:
- `R4PopulationBasisValidatorTest.java` (3 tests)
- `StringTimePeriodHandlerTest.java` (4 tests)
- `MeasureDefBuilderTest.java` (3-5 tests)

---

### 5. Snake Case vs Camel Case Method Names
**Issue**: Some methods use snake_case (e.g., `getStartZonedDateTime_happyPath_params`).

**Solution**: Convert to PascalCase for record name:
- `getStartZonedDateTime_happyPath_params()` → `GetStartZonedDateTimeHappyPathParams`
- `validateGroupBasisTypeErrorPathParams()` → `ValidateGroupBasisTypeErrorPathParams`

**Rule**: Remove underscores, capitalize first letter of each word segment, add "Params" suffix.

---

### 6. Import Changes
**Issue**: After refactoring, some imports become unused.

**Before:**
```java
import org.junit.jupiter.params.provider.Arguments;
```

**After:**
```java
// Arguments import no longer needed, can be removed
```

**Action**: Remove unused `Arguments` imports after refactoring each file. Spotless and IDE auto-import cleanup will handle this.

---

### 7. Test Body Refactoring Patterns

**Pattern 1: Simple parameter access**
```java
// BEFORE
void test(String input, int expected) {
    assertEquals(expected, process(input));
}

// AFTER
void test(TestParams params) {
    assertEquals(params.expected(), process(params.input()));
}
```

**Pattern 2: Storing parameters in local variables** (keep pattern if improves readability)
```java
// BEFORE
void test(String input, int expected, boolean flag) {
    var result = process(input, flag);
    assertEquals(expected, result);
}

// AFTER - Option 1: Direct access
void test(TestParams params) {
    var result = process(params.input(), params.flag());
    assertEquals(params.expected(), result);
}

// AFTER - Option 2: Local variables (if it improves readability)
void test(TestParams params) {
    String input = params.input();
    int expected = params.expected();
    boolean flag = params.flag();

    var result = process(input, flag);
    assertEquals(expected, result);
}
```

**Guideline**: Use local variables if test body is long (10+ lines) or parameters are used multiple times. Otherwise, use direct accessor calls.

---

### 8. Record Placement in File
**Best Practice**: Place all record definitions at the top of the test class, after static constants/fields and before test methods.

**Example Structure:**
```java
class MyTest {
    // Static constants
    private static final String CONSTANT = "value";

    // Record definitions (grouped together)
    private record FirstTestParams(...) {}
    private record SecondTestParams(...) {}
    private record ThirdTestParams(...) {}

    // Fields
    private MyService service;

    // Setup/teardown
    @BeforeEach void setup() { ... }

    // Method source providers
    private static Stream<FirstTestParams> firstTestParams() { ... }
    private static Stream<SecondTestParams> secondTestParams() { ... }

    // Test methods
    @ParameterizedTest
    @MethodSource("firstTestParams")
    void firstTest(FirstTestParams params) { ... }

    @ParameterizedTest
    @MethodSource("secondTestParams")
    void secondTest(SecondTestParams params) { ... }

    // Helper methods
    private void assertSomething(...) { ... }
}
```

---

## Task Breakdown

Execute in this order:

### Phase 1: Module cqf-fhir-utility (Start here)
- [ ] Refactor `TerminologyServerClientTest.java`
- [ ] Refactor `IgRepositoryBadDataTest.java`
- [ ] Run: `mvn test -pl cqf-fhir-utility`
- [ ] Run: `mvn spotless:apply -pl cqf-fhir-utility`

### Phase 2: Module cqf-fhir-cr-cli
- [ ] Refactor `CliTest.java`
- [ ] Run: `mvn test -pl cqf-fhir-cr-cli`
- [ ] Run: `mvn spotless:apply -pl cqf-fhir-cr-cli`

### Phase 3: Module cqf-fhir-cr (Core - largest module)
- [ ] Refactor `DateHelperTest.java` (use as reference example)
- [ ] Refactor `R4PopulationBasisValidatorTest.java` (3 tests)
- [ ] Refactor `MeasureDefBuilderTest.java` (multiple tests)
- [ ] Refactor `PackageVisitorTests.java`
- [ ] Refactor `R4MeasureServiceUtilsTest.java`
- [ ] Refactor `R4RepositorySubjectProviderTest.java`
- [ ] Refactor `R4DateHelperTest.java`
- [ ] Refactor `MeasureMultipleSdeTest.java`
- [ ] Refactor `MeasureProcessorCQLParameterTest.java`
- [ ] Refactor `MeasurePeriodValidatorTest.java`
- [ ] Refactor `MeasureOperationParameterConverterTests.java`
- [ ] Run: `mvn test -pl cqf-fhir-cr`
- [ ] Run: `mvn spotless:apply -pl cqf-fhir-cr`

### Phase 4: Module cqf-fhir-cr-hapi (HAPI integration)
- [ ] Refactor `IdHelperTest.java` (start simple)
- [ ] Refactor `CanonicalHelperTest.java`
- [ ] Refactor `CdsCrUtilsTest.java`
- [ ] Refactor `CrDiscoveryServiceR4Test.java`
- [ ] Refactor `CrDiscoveryServiceDstu3Test.java`
- [ ] Refactor `CrDiscoveryServiceR5Test.java`
- [ ] Refactor `ResponseEncoderMethodResolveIndicatorTest.java`
- [ ] Refactor `ClinicalIntelligenceHapiFhirRepositoryTest.java`
- [ ] Refactor `StringTimePeriodHandlerTest.java` (4 tests, 50+ cases - save for last)
- [ ] Run: `mvn test -pl cqf-fhir-cr-hapi`
- [ ] Run: `mvn spotless:apply -pl cqf-fhir-cr-hapi`

### Phase 5: Final Validation
- [ ] Run: `mvn clean compile` (ensure everything compiles)
- [ ] Run: `mvn test` (all tests pass)
- [ ] Run: `mvn spotless:apply` (format entire project)
- [ ] Run: `mvn checkstyle:check` (verify style compliance)
- [ ] Review: No `Arguments` imports remain in refactored files
- [ ] Review: All `@ParameterizedTest` methods use record parameters
- [ ] Review: All `@MethodSource` methods return `Stream<RecordType>`

### Phase 6: Commit (DO NOT PUSH)
- [ ] Generate commit message following project conventions
- [ ] Commit with message (do not push)

---

## Success Criteria

### Functional Requirements
- [ ] All 40 parameterized tests converted to use records
- [ ] All 26 `@MethodSource` methods return `Stream<RecordType>`
- [ ] Zero `Arguments.of()` calls remain in refactored tests
- [ ] All tests pass with identical results to before refactoring

### Code Quality Requirements
- [ ] All record classes follow naming convention (PascalCase + "Params")
- [ ] All records are `private` inner classes
- [ ] `@Nullable` annotations preserved on record components where applicable
- [ ] Test bodies use `params.field()` accessor pattern
- [ ] Unused `Arguments` imports removed
- [ ] Spotless formatting applied successfully
- [ ] Checkstyle passes (no new violations)

### Compilation Requirements
- [ ] `mvn clean compile` succeeds
- [ ] No compilation errors or warnings
- [ ] All modules compile independently

### Testing Requirements
- [ ] `mvn test` passes for each module
- [ ] `mvn test` passes for entire project
- [ ] No test failures or errors
- [ ] Test execution time remains similar (no performance regression)

---

## Self-Validation Checklist (For AI Agent)

Before marking this PRP as complete, verify:

### Per-File Checklist
For each of the 23 files refactored:
- [ ] Identified all `@ParameterizedTest` methods in file
- [ ] Created one record per parameterized test
- [ ] Record naming follows convention (PascalCase + "Params")
- [ ] Record is `private` inner class
- [ ] Preserved `@Nullable` annotations on record components
- [ ] Updated `@MethodSource` return type to `Stream<RecordType>`
- [ ] Replaced all `Arguments.of()` with record constructors
- [ ] Updated test method signature to accept record parameter
- [ ] Refactored test body to use `params.field()` accessors
- [ ] Removed unused `Arguments` import
- [ ] File compiles: `mvn test-compile -Dtest=<ClassName> -pl <module>`
- [ ] Tests pass: `mvn test -Dtest=<ClassName> -pl <module>`

### Per-Module Checklist
For each of the 4 modules:
- [ ] All files in module refactored
- [ ] Module compiles: `mvn test-compile -pl <module>`
- [ ] All tests pass: `mvn test -pl <module>`
- [ ] Spotless applied: `mvn spotless:apply -pl <module>`

### Project-Wide Checklist
- [ ] All 23 files refactored
- [ ] All 40 parameterized tests converted
- [ ] All 26 method sources return `Stream<RecordType>`
- [ ] Zero `Arguments.of()` calls remain
- [ ] Project compiles: `mvn clean compile`
- [ ] All tests pass: `mvn test`
- [ ] Spotless applied: `mvn spotless:apply`
- [ ] Checkstyle passes: `mvn checkstyle:check`
- [ ] Commit message generated

### Quality Checklist
- [ ] No new compiler warnings
- [ ] No new test failures
- [ ] No new checkstyle violations
- [ ] Test execution time similar to before
- [ ] Code readability improved (subjective, but records should be clearer)

---

## References & Documentation

### Java Records
- **Official Java 17 Documentation**: https://docs.oracle.com/en/java/javase/17/language/records.html
- **JEP 395 (Records)**: https://openjdk.org/jeps/395
- **Records Tutorial**: https://www.baeldung.com/java-record-keyword

### JUnit 5 Parameterized Tests
- **Official Guide**: https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests
- **MethodSource Documentation**: https://junit.org/junit5/docs/current/api/org.junit.jupiter.params/org/junit/jupiter/params/provider/MethodSource.html
- **Best Practices**: https://www.baeldung.com/parameterized-tests-junit-5

### Related Patterns
- **Records as Test Data Carriers**: Common pattern in modern Java testing (see Spring Framework 6+ tests)
- **Type-Safe Test Parameters**: Industry best practice since Java 14+

---

## Confidence Score: 9/10

### Why 9/10?
- **Clear requirements**: Acceptance criteria are explicit and measurable
- **Proven pattern**: Records are stable Java 17 feature, widely used
- **Comprehensive research**: All 40 tests identified, patterns documented
- **Executable validation**: Maven commands provide immediate feedback
- **Low risk**: Refactoring is purely structural, no logic changes
- **Strong examples**: Before/after examples for simple and complex cases

### Risks (-1 point)
- **Volume**: 23 files is significant; requires systematic execution
- **Edge cases**: @Nullable handling requires attention in ~30% of tests
- **Test complexity**: `StringTimePeriodHandlerTest.java` has 50+ test cases

### Mitigation
- **Incremental validation**: Validate after each module (4 checkpoints)
- **Start simple**: Begin with `cqf-fhir-utility` (2 simple files)
- **Reference examples**: Use `DateHelperTest.java` as template
- **Systematic approach**: Follow task breakdown strictly

**Expected outcome**: One-pass implementation success with high confidence.
