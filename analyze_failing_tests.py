#!/usr/bin/env python3
"""
Analyze remaining failing tests to understand expected vs actual populations.
"""

import json
from pathlib import Path

MEASUREREPORT_DIR = Path("cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/QICore/input/tests/measurereport")

FAILING_TESTS = [
    "699e12b2-26d4-43a8-add0-bcdd6629fe88",
    "eb7ec114-0c95-4e73-98ad-772a8197ffff",
    "ac67c1e3-d0df-4745-bc85-d4ec0a18e8f3",
    "81d2ade5-fa91-428c-b39f-3f0b8b7b2c16",
    "f2a7180d-acd8-4394-acdd-8959d861ef65",
    "ee5db0d0-8af1-4521-a060-aed5b026e194",
    "2e186c68-d7f4-4b2e-9f8a-e73c79905e7e",
]

def get_test_description(report):
    """Extract the test case description from the MeasureReport."""
    for ext in report.get("extension", []):
        if ext.get("url") == "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-testCaseDescription":
            return ext.get("valueMarkdown", "")
    return ""

def get_subject_from_report(report):
    """Get subject patient ID from MeasureReport."""
    for contained in report.get("contained", []):
        if contained.get("resourceType") == "Parameters":
            for param in contained.get("parameter", []):
                if param.get("name") == "subject":
                    return param.get("valueString", "")
    return None

def get_populations(group):
    """Extract population counts from a group."""
    populations = {}
    for pop in group.get("population", []):
        code = None
        for coding in pop.get("code", {}).get("coding", []):
            if coding.get("system") == "http://terminology.hl7.org/CodeSystem/measure-population":
                code = coding.get("code")
                break
        if code:
            populations[code] = pop.get("count", 0)
    return populations

def get_evaluated_resources(report):
    """Get list of evaluated resources from MeasureReport."""
    resources = []
    for eval_res in report.get("evaluatedResource", []):
        ref = eval_res.get("reference", "")
        resources.append(ref)
    return resources

def get_measure_id(report):
    """Extract measure ID from measure URL."""
    measure_url = report.get("measure", "")
    return measure_url.split("/")[-1] if "/" in measure_url else measure_url

def main():
    for report_id in FAILING_TESTS:
        report_file = MEASUREREPORT_DIR / f"{report_id}.json"
        if not report_file.exists():
            print(f"Report not found: {report_id}")
            continue

        with open(report_file, 'r') as f:
            report = json.load(f)

        measure_id = get_measure_id(report)
        patient_id = get_subject_from_report(report)
        description = get_test_description(report)
        resources = get_evaluated_resources(report)

        groups = report.get("group", [])
        if groups:
            populations = get_populations(groups[0])

        print(f"\n{'='*70}")
        print(f"Report: {report_id}")
        print(f"Measure: {measure_id}")
        print(f"Patient: {patient_id}")
        print(f"Description: {description}")
        print(f"\nExpected Populations:")
        for pop, count in populations.items():
            print(f"  {pop}: {count}")
        print(f"\nEvaluated Resources ({len(resources)}):")
        for res in resources:
            print(f"  {res}")


if __name__ == "__main__":
    main()
