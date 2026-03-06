#!/usr/bin/env python3
"""
Check for Condition and Procedure resources missing subject/patient references.
"""

import json
from pathlib import Path

TESTS_DIR = Path("cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/QICore/input/tests")

def check_resources_for_patient_ref(resource_type):
    """Check all resources of a given type for missing patient/subject references."""
    res_dir = TESTS_DIR / resource_type.lower()
    if not res_dir.exists():
        print(f"Directory not found: {res_dir}")
        return []

    missing = []
    for res_file in sorted(res_dir.glob("*.json")):
        with open(res_file, 'r') as f:
            try:
                resource = json.load(f)
            except json.JSONDecodeError:
                print(f"  Error parsing: {res_file}")
                continue

        # Check for subject or patient reference
        has_subject = "subject" in resource and resource["subject"].get("reference")
        has_patient = "patient" in resource and resource["patient"].get("reference")

        if not has_subject and not has_patient:
            missing.append(res_file.stem)

    return missing


def main():
    resource_types = ["Condition", "Procedure", "Observation", "MedicationRequest",
                      "MedicationAdministration", "ServiceRequest", "DiagnosticReport"]

    total_missing = 0
    for res_type in resource_types:
        missing = check_resources_for_patient_ref(res_type)
        if missing:
            print(f"\n{res_type}: {len(missing)} resources MISSING subject/patient reference")
            for res_id in missing:
                print(f"  {res_type}/{res_id}")
            total_missing += len(missing)
        else:
            res_dir = TESTS_DIR / res_type.lower()
            if res_dir.exists():
                count = len(list(res_dir.glob("*.json")))
                print(f"{res_type}: OK ({count} resources all have references)")

    print(f"\n{'='*60}")
    print(f"TOTAL: {total_missing} resources missing subject/patient reference")


if __name__ == "__main__":
    main()
