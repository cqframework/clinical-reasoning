# PRPs: Planned Refactoring Proposals

This directory contains the implementation plan for the Unified Measure Service Architecture with MeasureDef/MeasureReportDef Separation.

## Quick Start

1. **Read the index**: Start with [00-INDEX.md](00-INDEX.md) for the complete overview
2. **Follow the phases**: PRPs are organized into 4 phases (0A-0E, 1-3, 4-5, 6)
3. **Implement sequentially**: Each PRP depends on previous ones

## File Structure

```
PRPs/
├── 00-INDEX.md                              # Master index and overview
│
├── Phase 1: MeasureDef/MeasureReportDef Separation (Foundation)
│   ├── PRP-0A-create-measure-report-def.md         # Create mutable ReportDef classes
│   ├── PRP-0B-refactor-measure-def-immutable.md    # Make Def classes immutable
│   ├── PRP-0C-update-measure-evaluator.md          # Wire in MeasureReportDef
│   ├── PRP-0D-update-multi-subject-evaluator.md    # Update multi-subject evaluator
│   └── PRP-0E-update-test-frameworks.md            # Support Def/ReportDef assertions
│
├── Phase 2: Unified Service Architecture (R4)
│   ├── PRP-1-create-r4-unified-service.md          # Create R4UnifiedMeasureService
│   ├── PRP-2-update-r4-hapi-providers.md           # Wire into HAPI providers
│   └── PRP-3-refactor-r4-tests.md                  # Migrate R4 tests
│
├── Phase 3: Unified Service Architecture (DSTU3)
│   ├── PRP-4-create-dstu3-unified-service.md       # Create Dstu3UnifiedMeasureService
│   └── PRP-5-update-dstu3-hapi-providers.md        # Wire DSTU3 providers
│
└── Phase 4: Workflow Separation
    └── PRP-6-implement-workflow-separation.md      # Split into Workflow 1 & 2
```

## Implementation Order

**Recommended sequence:**
1. Phase 1 (PRPs 0A → 0B → 0C → 0D → 0E) - Foundation
2. Phase 2 (PRPs 1 → 2 → 3) - R4 Unified Service
3. Phase 3 (PRPs 4 → 5) - DSTU3 Unified Service
4. Phase 4 (PRP 6) - Workflow Separation

**Total estimated effort:** ~15-20 days across all phases

## Key Concepts

### MeasureDef (Immutable)
- Pure FHIR Measure structure
- Created by builders, frozen after construction
- Represents "what to evaluate"

### MeasureReportDef (Mutable)
- Contains MeasureDef reference + evaluation state
- Holds scores, populations, resources, counts
- Represents "evaluation results"

### Unified Service
- Single API for both single and multi-measure evaluation
- Single-measure = thin wrapper (< 20 lines) over multi-measure
- Multi-measure = all real logic

## Documentation

- **Main plan**: `/Users/lukedegruchy/.claude/plans/typed-juggling-elephant.md`
- **Each PRP**: Detailed specification with:
  - Goal
  - Dependencies
  - Files to create/modify
  - Code examples
  - Success criteria
  - Estimated effort

## Status

**Current**: Planning complete, ready for implementation
**Next**: Begin PRP-0A implementation after user approval
