library ImagingStudyFeasibilityTest version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem DICOM: 'http://dicom.nema.org/resources/ontology/DCM'

code CT: 'CT' from DICOM display 'Computed Tomography'
code MR: 'MR' from DICOM display 'Magnetic Resonance'
code US: 'US' from DICOM display 'Ultrasound'
code XR: 'XR' from DICOM display 'X-Ray Radiography'
code MG: 'MG' from DICOM display 'Mammography'
code NM: 'NM' from DICOM display 'Nuclear Medicine'
code PT: 'PT' from DICOM display 'Positron Emission Tomography'
code DX: 'DX' from DICOM display 'Digital Radiography'

valueset CTImagingModality: 'https://research.balgrist.ch/ValueSet/CTImagingModality'

parameter ModalityCode String
parameter Manufacturer String
parameter SeriesDescriptionText String
parameter MinNumberOfSeries Integer

context Patient

define "AllImagingStudies":
  [ImagingStudy]

// =============================================================================
// TEST 1: Default Search Param (modality - study level)
// =============================================================================
define "StudiesWithModality":
  [ImagingStudy: modality ~ ModalityCode]
//  "AllImagingStudies" S
//    where exists (
//      S.modality M
//        where M.code.value = ModalityCode
//    )

define "HasStudyWithModality":
  exists "StudiesWithModality"

/*
@description: Returns ImagingStudy resources with a modality in the CTImagingModality value set
@comment: This item is marked selective to indicate it can be used as a narrowing filter in
determining the set of subjects on which to run. For the purposes of this demonstration this
tag is added manually to the data requirements for the measure post processing. In the future
this step will be performed by tooling.
@isSelective
*/
define "CTImagingStudies":
  [ImagingStudy: modality in CTImagingModality]

define "HasCTImagingStudies":
  exists "CTImagingStudies"

// =============================================================================
// TEST 2: Custom Search Param (manufacturer - series level extension)
// =============================================================================
define "StudiesWithManufacturer":
//  [ImagingStudy: manufacturer ~ Manufacturer]
  "AllImagingStudies" S
    where exists (
      S.series Serie
        where exists (
          Serie.extension E
            where E.url = 'http://example.org/fhir/StructureDefinition/dicom-equipment-manufacturer'
              and (E.value as FHIR.string).value = Manufacturer
        )
    )

define "HasStudyWithManufacturer":
  exists "StudiesWithManufacturer"

// =============================================================================
// TEST 3a: Non-Search-Param - Text (series.description)
// =============================================================================
define "StudiesWithSeriesDescription":
  "AllImagingStudies" S
    where exists (
      S.series Serie
        where PositionOf(Lower(SeriesDescriptionText), Lower(Serie.description.value)) >= 0
    )

define "HasStudyWithSeriesDescription":
  exists "StudiesWithSeriesDescription"

// =============================================================================
// TEST 3b: Non-Search-Param - Numeric (numberOfSeries)
// =============================================================================
define "StudiesWithMinSeries":
  "AllImagingStudies" S
    where S.numberOfSeries.value >= MinNumberOfSeries

define "HasStudyWithMinSeries":
  exists "StudiesWithMinSeries"

// =============================================================================
// Combined: ANY match (OR logic)
// =============================================================================
define "HasAnyMatch":
  "HasStudyWithModality"
    or "HasStudyWithManufacturer"
    or "HasStudyWithSeriesDescription"
    or "HasStudyWithMinSeries"

// =============================================================================
// Combined: ALL match (AND logic) Default Search Parameter First
// =============================================================================
define "HasAllMatchSearchDefault":
  "HasStudyWithModality"
    and "HasStudyWithManufacturer"
    and "HasStudyWithSeriesDescription"

// =============================================================================
// Combined: ALL match (AND logic) Custom Search Parameter First
// =============================================================================
define "HasAllMatchSearchCustom":
  "HasStudyWithManufacturer"
  and "HasStudyWithModality"
    and "HasStudyWithSeriesDescription"

// =============================================================================
// Combined: ALL match (AND logic) No Search Parameter First
// =============================================================================
define "HasAllMatchNoSearch":
  "HasStudyWithSeriesDescription"
  and "HasStudyWithModality"
    and "HasStudyWithManufacturer"

// =============================================================================
// Initial Population (default to any match)
// =============================================================================
define "InitialPopulation":
  "HasAnyMatch"