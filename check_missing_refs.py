#!/usr/bin/env python3
"""
Check for resources missing subject/patient references in failing tests.
"""

import json
from pathlib import Path

MEASUREREPORT_DIR = Path("cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/QICore/input/tests/measurereport")
TESTS_DIR = Path("cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/QICore/input/tests")

# Failing test MeasureReport IDs
FAILING_TESTS = [
    "2a364e88-7272-444d-a264-e931bba5391e",
    "a6399df7-7d9a-45da-a64b-97f695646ce6",
    "4138e2f8-7c51-4cbf-82b7-9983b775991a",
    "0fb98a8a-a7ac-49a3-a1bd-e042373dc1c6",
    "b0513b24-8789-4c07-a13d-322d9defbeb8",
    "699e12b2-26d4-43a8-add0-bcdd6629fe88",
    "a821b7fb-7913-45e4-82e2-cf232818d643",
    "eb7ec114-0c95-4e73-98ad-772a8197ffff",
    "ac67c1e3-d0df-4745-bc85-d4ec0a18e8f3",
    "1f48c160-8aba-4e86-bd5d-c5c4bdef1afd",
    "9eeadd82-4599-4b8b-95a5-f1d59697b451",
    "af8c832f-f1ad-407a-9751-575339d08367",
    "e66fcfe4-57f5-4259-bb05-540d4f6a864c",
    "81d2ade5-fa91-428c-b39f-3f0b8b7b2c16",
    "f2a7180d-acd8-4394-acdd-8959d861ef65",
    "ee5db0d0-8af1-4521-a060-aed5b026e194",
    "6244d8f6-995c-4a0e-9d86-9c3abfc3fcb7",
    "a754b13e-2ef7-4c69-a205-f9af9a9a089e",
    "1e896d30-3808-482a-b8a3-51198a58d4a6",
    "2e186c68-d7f4-4b2e-9f8a-e73c79905e7e",
    "6c210a7d-98b1-4d37-a268-45d14a7e7b1d",
]

def get_subject_from_report(report_id):
    """Get subject patient ID from MeasureReport."""
    report_file = MEASUREREPORT_DIR / f"{report_id}.json"
    if not report_file.exists():
        return None

    with open(report_file, 'r') as f:
        report = json.load(f)

    for contained in report.get("contained", []):
        if contained.get("resourceType") == "Parameters":
            for param in contained.get("parameter", []):
                if param.get("name") == "subject":
                    return param.get("valueString", "")
    return None


def get_evaluated_resources(report_id):
    """Get list of evaluated resources from MeasureReport."""
    report_file = MEASUREREPORT_DIR / f"{report_id}.json"
    if not report_file.exists():
        return []

    with open(report_file, 'r') as f:
        report = json.load(f)

    resources = []
    for eval_res in report.get("evaluatedResource", []):
        ref = eval_res.get("reference", "")
        if "/" in ref:
            res_type, res_id = ref.split("/", 1)
            resources.append((res_type, res_id))
    return resources


def check_resource_for_patient_ref(res_type, res_id, expected_patient_id):
    """Check if a resource has the correct patient/subject reference."""
    # Find the resource file
    res_type_lower = res_type.lower()
    res_file = TESTS_DIR / res_type_lower / f"{res_id}.json"

    if not res_file.exists():
        return None, f"File not found: {res_file}"

    with open(res_file, 'r') as f:
        resource = json.load(f)

    # Check for subject or patient reference
    subject_ref = None
    patient_ref = None

    if "subject" in resource:
        subject_ref = resource["subject"].get("reference", "")
    if "patient" in resource:
        patient_ref = resource["patient"].get("reference", "")

    expected_ref = f"Patient/{expected_patient_id}"

    if subject_ref:
        if subject_ref == expected_ref:
            return True, f"subject: {subject_ref}"
        else:
            return False, f"subject mismatch: has '{subject_ref}', expected '{expected_ref}'"
    elif patient_ref:
        if patient_ref == expected_ref:
            return True, f"patient: {patient_ref}"
        else:
            return False, f"patient mismatch: has '{patient_ref}', expected '{expected_ref}'"
    else:
        return False, "MISSING subject/patient reference"


def main():
    missing_refs = []
    mismatched_refs = []

    for report_id in FAILING_TESTS:
        patient_id = get_subject_from_report(report_id)
        if not patient_id:
            print(f"Could not find patient for report {report_id}")
            continue

        print(f"\n=== Report: {report_id} ===")
        print(f"Patient: {patient_id}")

        resources = get_evaluated_resources(report_id)
        for res_type, res_id in resources:
            if res_type == "Patient":
                continue  # Skip patient resources

            has_ref, msg = check_resource_for_patient_ref(res_type, res_id, patient_id)

            if has_ref is None:
                print(f"  {res_type}/{res_id}: {msg}")
            elif has_ref:
                pass  # print(f"  {res_type}/{res_id}: OK - {msg}")
            else:
                print(f"  {res_type}/{res_id}: {msg}")
                if "MISSING" in msg:
                    missing_refs.append((report_id, patient_id, res_type, res_id))
                else:
                    mismatched_refs.append((report_id, patient_id, res_type, res_id, msg))

    print("\n" + "="*60)
    print(f"SUMMARY: {len(missing_refs)} resources MISSING subject/patient reference")
    print(f"SUMMARY: {len(mismatched_refs)} resources with MISMATCHED references")
    print("="*60)

    if missing_refs:
        print("\nMISSING REFERENCES:")
        for report_id, patient_id, res_type, res_id in missing_refs:
            print(f"  {res_type}/{res_id} (patient: {patient_id})")

    if mismatched_refs:
        print("\nMISMATCHED REFERENCES:")
        for report_id, patient_id, res_type, res_id, msg in mismatched_refs:
            print(f"  {res_type}/{res_id} - {msg}")


if __name__ == "__main__":
    main()
