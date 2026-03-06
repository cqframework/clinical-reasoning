#!/usr/bin/env python3
"""
Regenerate MeasureEvaluationQICoreTest.java with status assertions and measure scores
from the source MeasureReport files, calculating correct scores based on improvementNotation.
"""

import json
import os
from pathlib import Path
from decimal import Decimal, ROUND_HALF_UP

MEASUREREPORT_DIR = Path("cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/QICore/input/tests/measurereport")
MEASURE_DIR = Path("cqf-fhir-cr/src/test/resources/org/opencds/cqf/fhir/cr/measure/r4/QICore/input/resources/measure")
OUTPUT_FILE = Path("cqf-fhir-cr/src/test/java/org/opencds/cqf/fhir/cr/measure/r4/MeasureEvaluationQICoreTest.java")

# Map measure URLs to IDs
MEASURE_ID_MAP = {
    "https://madie.cms.gov/Measure/CMS125FHIRBreastCancerScreen": "CMS125FHIRBreastCancerScreen",
    "https://madie.cms.gov/Measure/CMSFHIR529HybridHospitalWideReadmission": "CMSFHIR529HybridHospitalWideReadmission",
    "https://madie.cms.gov/Measure/CMS816FHIRHHHypo": "CMS816FHIRHHHypo",
}

# Map measure IDs to their improvementNotation and scoring type
# Loaded from measure resources
MEASURE_INFO = {}

def load_measure_info():
    """Load improvementNotation and scoring info from Measure resources."""
    for measure_file in MEASURE_DIR.glob("*.json"):
        with open(measure_file, 'r') as f:
            measure = json.load(f)
            measure_id = measure.get("id", "")

            # Get scoring type from profiles or scoring element
            scoring_type = None
            profiles = measure.get("meta", {}).get("profile", [])
            for profile in profiles:
                if "proportion-measure" in profile:
                    scoring_type = "proportion"
                    break
                elif "cohort-measure" in profile:
                    scoring_type = "cohort"
                    break
                elif "ratio-measure" in profile:
                    scoring_type = "ratio"
                    break
                elif "continuous-variable-measure" in profile:
                    scoring_type = "continuous-variable"
                    break

            # Get improvement notation from measure-level or group-level extensions
            improvement_notation = None

            # Check measure-level improvementNotation element
            if measure.get("improvementNotation"):
                imp_coding = measure["improvementNotation"].get("coding", [])
                for coding in imp_coding:
                    if coding.get("code") in ["increase", "decrease"]:
                        improvement_notation = coding["code"]
                        break

            # Check group-level extensions
            if not improvement_notation:
                for group in measure.get("group", []):
                    for ext in group.get("extension", []):
                        if ext.get("url") == "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-improvementNotation":
                            value_cc = ext.get("valueCodeableConcept", {})
                            for coding in value_cc.get("coding", []):
                                if coding.get("code") in ["increase", "decrease"]:
                                    improvement_notation = coding["code"]
                                    break
                    if improvement_notation:
                        break

            MEASURE_INFO[measure_id] = {
                "improvementNotation": improvement_notation,
                "scoringType": scoring_type
            }
            print(f"  Loaded measure {measure_id}: improvementNotation={improvement_notation}, scoring={scoring_type}")


def get_test_description(report):
    """Extract the test case description from the MeasureReport."""
    for ext in report.get("extension", []):
        if ext.get("url") == "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-testCaseDescription":
            desc = ext.get("valueMarkdown", "")
            # Clean up for Java comment
            return desc.replace("*/", "* /").replace("\n", " ").strip()
    return ""


def get_subject(report):
    """Extract the subject patient ID from the MeasureReport."""
    for contained in report.get("contained", []):
        if contained.get("resourceType") == "Parameters":
            for param in contained.get("parameter", []):
                if param.get("name") == "subject":
                    return param.get("valueString", "")
    return ""


def get_period(report):
    """Extract the measurement period from the MeasureReport."""
    period = report.get("period", {})
    return period.get("start", ""), period.get("end", "")


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


def calculate_proportion_score(populations, improvement_notation):
    """
    Calculate the correct proportion score based on populations and improvementNotation.

    For proportion measures:
    - score = (numerator - numerator-exclusion) / (denominator - denominator-exclusion - denominator-exception)

    Score handling based on improvementNotation:
    - If denominator (after exclusions) = 0: no score (return None)
    - If improvementNotation = "increase": score = numerator / denominator
    - If improvementNotation = "decrease": score = 1 - (numerator / denominator)

    Special cases:
    - When denominator-exclusion removes all subjects: no score (performance not met is excluded)
    """
    numerator = populations.get("numerator", 0)
    numerator_exclusion = populations.get("numerator-exclusion", 0)
    denominator = populations.get("denominator", 0)
    denominator_exclusion = populations.get("denominator-exclusion", 0)
    denominator_exception = populations.get("denominator-exception", 0)

    # Calculate effective denominator
    effective_denominator = denominator - denominator_exclusion - denominator_exception

    # If effective denominator is 0 or negative, no score
    if effective_denominator <= 0:
        return None

    # Calculate effective numerator
    effective_numerator = numerator - numerator_exclusion

    # Base score calculation
    base_score = effective_numerator / effective_denominator

    # Apply improvement notation
    if improvement_notation == "decrease":
        # For decrease measures, score = 1 - base_score
        # e.g., CMS816 - fewer hypoglycemic events is better
        score = 1.0 - base_score
    else:
        # For increase measures (default), score = base_score
        # e.g., CMS125 - more screenings is better
        score = base_score

    return score


def format_score(score):
    """Format a score value for Java assertion."""
    if score is None:
        return None
    # Format as string that matches Java BigDecimal toString
    if isinstance(score, float):
        if score == int(score):
            return f"{int(score)}.0"
        else:
            # Round to reasonable precision
            result = str(round(score, 16)).rstrip('0')
            if result.endswith('.'):
                result += '0'
            return result
    return str(score)


def generate_test_method(report_id, report):
    """Generate a single test method from a MeasureReport."""
    measure_url = report.get("measure", "")
    measure_id = MEASURE_ID_MAP.get(measure_url, "")
    if not measure_id:
        print(f"  WARNING: Unknown measure URL: {measure_url}")
        return None

    subject = get_subject(report)
    if not subject:
        print(f"  WARNING: No subject found in {report_id}")
        return None

    period_start, period_end = get_period(report)
    description = get_test_description(report)

    groups = report.get("group", [])
    if not groups:
        print(f"  WARNING: No groups found in {report_id}")
        return None

    # Get first group data
    first_group = groups[0]
    populations = get_populations(first_group)

    # Get measure info
    measure_info = MEASURE_INFO.get(measure_id, {})
    improvement_notation = measure_info.get("improvementNotation")
    scoring_type = measure_info.get("scoringType")

    # Calculate the correct score based on improvementNotation
    if scoring_type == "proportion":
        calculated_score = calculate_proportion_score(populations, improvement_notation)
    elif scoring_type == "cohort":
        # Cohort measures don't have scores
        calculated_score = None
    else:
        # Default: use the score from the source if available
        source_score = first_group.get("measureScore", {}).get("value")
        calculated_score = source_score

    # Build the test method
    method_name = f"test_{report_id.replace('-', '_')}"

    lines = []

    # Add Javadoc comment if description exists
    if description:
        lines.append(f"    /**")
        lines.append(f"     * {description}")
        lines.append(f"     */")

    lines.append(f"    @Test")
    lines.append(f"    void {method_name}() {{")
    lines.append(f"        given.when()")
    lines.append(f'                .measureId("{measure_id}")')
    lines.append(f'                .subject("Patient/{subject}")')
    lines.append(f'                .periodStart("{period_start}")')
    lines.append(f'                .periodEnd("{period_end}")')
    lines.append(f"                .evaluate()")
    lines.append(f"                .then()")
    lines.append(f"                .report()")
    lines.append(f"                .hasStatus(MeasureReportStatus.COMPLETE)")
    lines.append(f"                .firstGroup()")

    # Add population assertions in a consistent order
    pop_order = ["initial-population", "denominator", "denominator-exclusion", "denominator-exception", "numerator", "numerator-exclusion"]
    for pop_code in pop_order:
        if pop_code in populations:
            lines.append(f'                .population("{pop_code}")')
            lines.append(f"                .hasCount({populations[pop_code]})")
            lines.append(f"                .up()")

    # Add measure score assertion if calculated
    formatted_score = format_score(calculated_score)
    if formatted_score is not None:
        lines.append(f'                .hasScore("{formatted_score}")')

    lines.append(f"                .up()")
    lines.append(f"                .report();")
    lines.append(f"    }}")

    return "\n".join(lines)


def main():
    """Main function to regenerate the test class."""
    print("Loading Measure information...")
    load_measure_info()

    print("\nRegenerating MeasureEvaluationQICoreTest.java...")

    # Collect all MeasureReports
    reports = []
    for json_file in sorted(MEASUREREPORT_DIR.glob("*.json")):
        with open(json_file, 'r') as f:
            try:
                report = json.load(f)
                report_id = report.get("id", json_file.stem)
                reports.append((report_id, report))
            except json.JSONDecodeError as e:
                print(f"  ERROR parsing {json_file}: {e}")

    print(f"\nLoaded {len(reports)} MeasureReports")

    # Generate test methods
    test_methods = []
    for report_id, report in reports:
        method = generate_test_method(report_id, report)
        if method:
            test_methods.append(method)

    print(f"Generated {len(test_methods)} test methods")

    # Build the complete test class
    class_content = '''package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Tests for QICore CMS measures (CMS125, CMS529, CMS816) evaluating expected results.
 * Test cases are derived from the measure bundle test resources.
 *
 * Score calculations are based on improvementNotation:
 * - CMS125 (increase): higher scores are better (numerator/denominator)
 * - CMS816 (decrease): lower scores are better (1 - numerator/denominator)
 * - CMS529 (cohort): no score
 */
@SuppressWarnings("squid:S2699")
class MeasureEvaluationQICoreTest {

    private static final Given given = Measure.given().repositoryFor("QICore");

'''

    class_content += "\n\n".join(test_methods)
    class_content += "\n}\n"

    # Write the output file
    with open(OUTPUT_FILE, 'w') as f:
        f.write(class_content)

    print(f"\nWrote {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
