library ComponentCriteriaStratifier

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers

parameter "Measurement Period" Interval<DateTime> default Interval[@2024-01-01T00:00:00, @2024-12-31T23:59:59]

context Patient

define "Initial Population Boolean":
  exists("All Encounters")

define "Initial Population Resource":
  "Encounters Arrived Planned"

define "Encounters Arrived Planned":
     [Encounter] E
        where E.status.value in { 'arrived', 'planned' }

//
// Boolean/Resource
//

// Scenario 1:  Overlap on Arrived

define "Stratifier Encounters Arrived Triaged":
     [Encounter] E
        where E.status.value in { 'arrived', 'triaged' }

define "Stratifier Encounters Arrived In-Progress":
     [Encounter] E
        where E.status.value in { 'arrived', 'in-progress' }

// Scenario 2:  No overlap due to intersection only between initial-pop and strat2

define "Stratifier Encounters Planned Triaged":
     [Encounter] E
        where E.status.value in { 'planned', 'triaged' }

define "Stratifier Encounters Arrived Cancelled":
     [Encounter] E
        where E.status.value in { 'arrived', 'cancelled' }

// Scenario 3:  No overlap despite total intersection between initial-pop and strat2

define "Stratifier Encounters Cancelled Finished":
     [Encounter] E
        where E.status.value in { 'cancelled', 'finished' }

define "Stratifier Encounters Arrived Planned":
    "Encounters Arrived Planned":

// Scenario 4:  No overlap despite total intersection between strat1 and strat2

define "Stratifier Encounters Cancelled Triaged":
     [Encounter] E
        where E.status.value in { 'cancelled', 'triaged' }

// Scenario 5:  No overlap because init-pop, strat1, and strat2 have zero overlap with all

define "Stratifier Encounters Cancelled In-Progress":
     [Encounter] E
        where E.status.value in { 'cancelled', 'in-progress' }

define "Stratifier Encounters Finished Triaged":
     [Encounter] E
        where E.status.value in { 'finished', 'triaged' }

//
// Date
//

// Scenario 1:  Overlap on 2024-01-02

define "Initial Population Date":
  { @2024-01-01, @2024-01-02 }

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

define "Stratifier Mar1 Apr1":
  { @2024-03-01, @2024-04-01 }

// Scenario 5:  No overlap because init-pop, strat1, and strat2 have zero overlap with all

define "Stratifier May1 Jun1":
  { @2024-05-01, @2024-06-01 }

define "Stratifier Jul1 Aug1":
  { @2024-07-01, @2024-08-01 }