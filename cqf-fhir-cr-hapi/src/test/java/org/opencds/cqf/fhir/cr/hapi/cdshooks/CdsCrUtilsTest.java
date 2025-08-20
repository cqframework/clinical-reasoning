package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class CdsCrUtilsTest {

    @Test
    void readPlanDefinition_forDstu3_shouldCallCorrectReadMethod() {
        // Arrange
        IRepository iRepository = new InMemoryFhirRepository(FhirContext.forDstu3Cached());
        PlanDefinition expectedResource = new PlanDefinition();
        IIdType id = iRepository.create(expectedResource).getId();

        // Act
        IBaseResource result = CdsCrUtils.readPlanDefinitionFromRepository(iRepository, id);

        // Assert
        assertInstanceOf(PlanDefinition.class, result);
    }

    @Test
    void readPlanDefinition_forR4_shouldCallCorrectReadMethod() {
        // Arrange
        IRepository iRepository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        org.hl7.fhir.r4.model.PlanDefinition expectedResource = new org.hl7.fhir.r4.model.PlanDefinition();
        IIdType id = iRepository.create(expectedResource).getId();

        // Act
        IBaseResource result = CdsCrUtils.readPlanDefinitionFromRepository(iRepository, id);

        // Assert
        assertInstanceOf(org.hl7.fhir.r4.model.PlanDefinition.class, result);
    }

    @Test
    void readPlanDefinition_forR5_shouldCallCorrectReadMethod() {
        // Arrange
        IRepository iRepository = new InMemoryFhirRepository(FhirContext.forR5Cached());
        org.hl7.fhir.r5.model.PlanDefinition expectedResource = new org.hl7.fhir.r5.model.PlanDefinition();
        IIdType id = iRepository.create(expectedResource).getId();

        // Act
        IBaseResource result = CdsCrUtils.readPlanDefinitionFromRepository(iRepository, id);

        // Assert
        assertInstanceOf(org.hl7.fhir.r5.model.PlanDefinition.class, result);
    }

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Should return null for unsupported FHIR versions")
    void readPlanDefinition_forUnsupportedVersions_shouldReturnNull(FhirVersionEnum theUnsupportedVersion) {
        IIdType dontCare = null;
        // Arrange
        FhirContext fhirContext = FhirContext.forVersion(theUnsupportedVersion);
        IRepository repository = Mockito.mock(IRepository.class);
        Mockito.when(repository.fhirContext()).thenReturn(fhirContext);

        // Act
        IBaseResource result = CdsCrUtils.readPlanDefinitionFromRepository(repository, dontCare);

        // Assert
        assertNull(result, "The result should be null for unsupported version: " + theUnsupportedVersion);
    }
}
