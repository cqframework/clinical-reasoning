library HelloWorld version '2.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'


context Patient

define "Info":
  'info'

define "Warning":
  'warning'

define "Critical":
  'critical'

define "Main Action Condition Expression Is True":
  true

define "Get Title":
  'Hello World! I am a new version!'

define "Get Description":
  'The CDS Service is alive and communicating successfully!'

define "Get Indicator":
  'info'