library RouteOneOrganization version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1' called FHIRHelpers
include "OutpatientPriorAuthorizationPrepopulation" called "OutpatientPriorAuthorizationPrepopulation"

parameter ClaimId String default 'OPA-Claim1'

context Patient

define "RouteOneOrganization.name":
   OutpatientPriorAuthorizationPrepopulation.OrganizationFacility.name

define "RouteOneOrganization.FacilityNPI.value":
   (OutpatientPriorAuthorizationPrepopulation.OrganizationFacility.identifier X
      where X.system = 'http://npi.org'
   ).value.value

define "RouteOneOrganization.FacilityPTAN.value":
   (OutpatientPriorAuthorizationPrepopulation.OrganizationFacility.identifier X
      where X.system = 'http://ptan.org'
   ).value.value
