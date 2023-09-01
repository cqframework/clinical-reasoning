package org.opencds.cqf.fhir.cr.questionnaire.dstu3.helpers;

import java.util.List;
import javax.annotation.Nonnull;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.UriType;

public class TestingHelper {
    static final String PROFILE_URL = "http://www.sample.com/profile/profileId";
    static final String LINK_ID = "profileId";
    static final QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.GROUP;
    static final String PROFILE_TITLE = "Profile Title";

    @Nonnull
    public static ElementDefinition withElementDefinition(String typeCode, String pathValue) {
        final ElementDefinition elementDefinition = new ElementDefinition().setPath(pathValue);
        final TypeRefComponent type = new TypeRefComponent();
        type.setCode(typeCode);
        elementDefinition.setType(List.of(type));
        return elementDefinition;
    }

    @Nonnull
    public static DataRequirement withActionInput() {
        final DataRequirement actionInput = new DataRequirement();
        final UriType canonicalType = new UriType();
        canonicalType.setValue(PROFILE_URL);
        actionInput.setProfile(List.of(canonicalType));
        return actionInput;
    }

    @Nonnull
    public static QuestionnaireItemComponent withQuestionnaireItemComponent() {
        return new QuestionnaireItemComponent()
                .setType(QUESTIONNAIRE_ITEM_TYPE)
                .setLinkId(LINK_ID)
                .setText(PROFILE_TITLE)
                .setRequired(false);
    }
}
