library ComponentCriteriaStratifier

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population Boolean":
  exists("All Encounters")

define "All Encounters":
     [Encounter] E

define "Stratifier Encounter Finished Boolean":
    exists("Stratifier Encounter Finished")

define "Stratifier Encounter Finished":
     [Encounter] E
        where E.status = 'finished'

define "Stratifier Encounter In-Progress Boolean":
    exists("Stratifier Encounter In-Progress")

define "Stratifier Encounter In-Progress":
     [Encounter] E
        where E.status = 'in-progress'

define "Initial Population Date":
  { @2024-01-01, @2024-01-02 }

define "Stratifier Feb1 Jan2":
  { @2024-02-01, @2024-01-02 }

define "Stratifier Feb3 Jan2":
  { @2024-02-03, @2024-01-02 }

define "Stratifier Feb1 Mar2":
  { @2024-02-01, @2024-03-02 }

define "Stratifier Feb1 Mar2 Jan2":
  { @2024-02-03, @2024-03-02, @2024-01-02 }
