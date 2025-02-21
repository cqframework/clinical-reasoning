library PHQ9Logic

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "LOINC": 'http://loinc.org'

code "Not at all": 'LA6568-5' from "LOINC"
code "Several days": 'LA6569-3' from "LOINC"
code "More than half the days": 'LA6570-1' from "LOINC"
code "Nearly every day": 'LA6571-9' from "LOINC"

parameter response QuestionnaireResponse

context Patient

define QuestionnaireResponses: [QuestionnaireResponse]

// Use the QuestionnaireResponse provided as a parameter if available
// Otherwise look it up from the record
define "QuestionnaireResponse":
  if response is not null then
    response
  else
    Last(
      [QuestionnaireResponse] QR
        where QR.questionnaire = 'http://somewhere.org/fhir/uv/mycontentig/Questionnaire/phq-9-questionnaire'
        sort by (authored as FHIR.dateTime) desc
    )

// NOTE: This ties this logic directly to this particular rendering of the Questionnaire.
// Ideally the items would be filtered based on Code of the Item, rather than the LinkId,
// but that would require pulling in the Questionnaire because the QuestionnaireResponse
// only has the LinkId.
define "Responses":
  "QuestionnaireResponse" QR
    return QR.item I where I.linkId in {
      'LittleInterest',
      'FeelingDown',
      'TroubleSleeping',
      'FeelingTired',
      'BadApetite',
      'FeelingBadAboutSelf',
      'TroubleConcetrating',
      'MovingSpeaking'
    }

/*
@source: https://www.med.umich.edu/1info/FHP/practiceguides/depress/score.pdf
*/
define function "AnswerCardinality"(answer Coding):
  case
    when answer ~ "Not at all" then 0
    when answer ~ "Several days" then 1
    when answer ~ "More than half the days" then 2
    when answer ~ "Nearly every day" then 3
    else null
  end

define "CalculateTotalScore":
  Sum("Responses" R return all AnswerCardinality(First(R.answer.value)))
