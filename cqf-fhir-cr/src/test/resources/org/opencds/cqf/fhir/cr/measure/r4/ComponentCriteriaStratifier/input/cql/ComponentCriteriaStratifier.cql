library ComponentCriteriaStratifier

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population Boolean":
  exists("Initial Population Resource")

define "Initial Population Resource":
  "Encounters Arrived Planned"

define "Initial Population Date":
  { @2024-01-01, @2024-01-02 }

define "Encounters Arrived Planned":
     [Encounter] E
        where E.status.value in { 'arrived', 'planned' }

//
// Boolean/Resource
//

//// Scenario 1:  Overlap on Arrived

define "Stratifier Encounters Arrived Triaged Boolean":
    exists("Stratifier Encounters Arrived Triaged Resource")

define "Stratifier Encounters Arrived Triaged Resource":
     [Encounter] E
        where E.status.value in { 'arrived', 'triaged' }

define "Stratifier Encounters Arrived In-Progress Boolean":
    exists("Stratifier Encounters Arrived In-Progress Resource")

define "Stratifier Encounters Arrived In-Progress Resource":
     [Encounter] E
        where E.status.value in { 'arrived', 'in-progress' }

// Scenario 2:  No overlap due to intersection only between initial-pop and strat2

define "Stratifier Encounters Planned Triaged Boolean":
    exists("Stratifier Encounters Planned Triaged Resource")

define "Stratifier Encounters Planned Triaged Resource":
     [Encounter] E
        where E.status.value in { 'planned', 'triaged' }

define "Stratifier Encounters Arrived Cancelled Boolean":
    exists("Stratifier Encounters Arrived Cancelled Resource")

define "Stratifier Encounters Arrived Cancelled Resource":
     [Encounter] E
        where E.status.value in { 'arrived', 'cancelled' }

// Scenario 3:  No overlap despite total intersection between initial-pop and strat2

define "Stratifier Encounters Cancelled Finished Boolean":
    exists("Stratifier Encounters Cancelled Finished Resource")

define "Stratifier Encounters Cancelled Finished Resource":
     [Encounter] E
        where E.status.value in { 'cancelled', 'finished' }

define "Stratifier Encounters Arrived Planned Boolean":
    exists("Stratifier Encounters Arrived Planned Resource")

define "Stratifier Encounters Arrived Planned Resource":
    "Encounters Arrived Planned"

// Scenario 4:  No overlap despite total intersection between strat1 and strat2

define "Stratifier Encounters Cancelled Triaged 1 Boolean":
    exists("Stratifier Encounters Cancelled Triaged 1 Resource")

define "Stratifier Encounters Cancelled Triaged 2 Boolean":
    exists("Stratifier Encounters Cancelled Triaged 2 Resource")

define "Stratifier Encounters Cancelled Triaged 1 Resource":
     [Encounter] E
        where E.status.value in { 'cancelled', 'triaged' }

define "Stratifier Encounters Cancelled Triaged 2 Resource":
     [Encounter] E
        where E.status.value in { 'cancelled', 'triaged' }

// Scenario 5:  No overlap because init-pop, strat1, and strat2 have zero overlap with all

define "Stratifier Encounters Cancelled In-Progress Boolean":
    exists("Stratifier Encounters Cancelled In-Progress Resource")

define "Stratifier Encounters Finished Triaged Boolean":
    exists("Stratifier Encounters Finished Triaged Resource")

define "Stratifier Encounters Cancelled In-Progress Resource":
     [Encounter] E
        where E.status.value in { 'cancelled', 'in-progress' }

define "Stratifier Encounters Finished Triaged Resource":
     [Encounter] E
        where E.status.value in { 'finished', 'triaged' }

//
// Date
//

// Scenario 1:  Overlap on 2024-01-02

define "Stratifier Feb1 Jan2":
  { @2024-02-01, @2024-01-02 }

define "Stratifier Feb3 Jan2":
  { @2024-02-03, @2024-01-02 }

// Scenario 2:  No overlap due to intersection only between initial-pop and strat2

define "Stratifier Feb1 Mar2":
  { @2024-02-01, @2024-03-02 }

define "Stratifier Feb1 Mar2 Jan2":
  { @2024-02-03, @2024-03-02, @2024-01-02 }

// Scenario 3:  No overlap despite total intersection between initial-pop and strat2

define "Stratifier Mar1 Apr1":
  { @2024-03-01, @2024-04-01 }

define "Stratifier Jan1 Jan2":
  { @2024-01-01, @2024-02-01 }

// Scenario 4:  No overlap despite total intersection between strat1 and strat2

define "Stratifier May1 Jun1 1":
  { @2024-05-01, @2024-06-01 }

define "Stratifier May1 Jun1 2":
  { @2024-05-01, @2024-06-01 }

// Scenario 5:  No overlap because init-pop, strat1, and strat2 have zero overlap with all

define "Stratifier Jul1 Aug1":
  { @2024-07-01, @2024-08-01 }

define "Stratifier Sep1 Oct1":
  { @2024-09-01, @2024-10-01 }
