library SDHCommon version '1.0.000'

using QICore version '4.1.0'

include FHIRHelpers version '4.1.000'
include FHIRCommon version '1.1.000' called FC

context Patient

define function Official(identifiers List<Identifier>):
  singleton from ( identifiers I
      where I.use = 'official'
  )

define function Official(names List<HumanName>):
  singleton from ( names N
      where N.use = 'official'
  )

define function Only(observations List<QICore.Observation>):
  singleton from observations

define function Earliest(observations List<QICore.Observation>):
  First(observations O
      sort by issued
  )

define function Latest(observations List<QICore.Observation>):
  Last(observations O
      sort by issued
  )

define function MostRecent(observations List<QICore.Observation>):
  Last(observations O
      sort by issued
  )

define function MostRecent(conditions List<QICore.Condition>):
  Last(conditions O
      sort by recordedDate
  )

define function Lowest(observations List<QICore.Observation>):
  First(observations O
      sort by(value as System.Quantity)
  )

define function Highest(observations List<QICore.Observation>):
  Last(observations O
      sort by(value as System.Quantity)
  )

define function Has(value List<System.Any>):
  if exists value then true 
    else null