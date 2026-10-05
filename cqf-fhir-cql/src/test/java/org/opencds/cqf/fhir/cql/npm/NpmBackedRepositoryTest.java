package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Nested;

public class NpmBackedRepositoryTest {

    @Nested
    public class R4Tests implements INpmBackedRepositoryTest {
        private final FhirContext fhirContext = FhirContext.forR4Cached();

        @Override
        public FhirContext getFhirContext() {
            return fhirContext;
        }

        @Override
        public IBaseResource createLibraryResource(String name, String canonicalUrl) {
            Library library = new Library();
            library.setStatus(Enumerations.PublicationStatus.ACTIVE);
            library.setUrl(canonicalUrl);
            library.setName(name);
            return library;
        }

        @Override
        public IBaseResource createMeasureResource(String canonicalUrl) {
            Measure measure = new Measure();
            measure.setStatus(PublicationStatus.ACTIVE);
            measure.setUrl(canonicalUrl);
            return measure;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends IBaseResource> Class<T> getResourceClass(String resourceName) {
            return (Class<T>) fhirContext.getResourceDefinition(resourceName).getImplementingClass();
        }
    }

    @Nested
    public class R5Tests implements INpmBackedRepositoryTest {

        private final FhirContext fhirContext = FhirContext.forR5Cached();

        @Override
        public FhirContext getFhirContext() {
            return fhirContext;
        }

        @Override
        public IBaseResource createLibraryResource(String name, String canonicalUrl) {
            var library = new org.hl7.fhir.r5.model.Library();
            library.setStatus(org.hl7.fhir.r5.model.Enumerations.PublicationStatus.ACTIVE);
            library.setUrl(canonicalUrl);
            library.setName(name);
            return library;
        }

        @Override
        public IBaseResource createMeasureResource(String canonicalUrl) {
            var measure = new org.hl7.fhir.r5.model.Measure();
            measure.setStatus(org.hl7.fhir.r5.model.Enumerations.PublicationStatus.ACTIVE);
            measure.setUrl(canonicalUrl);
            return measure;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends IBaseResource> Class<T> getResourceClass(String resourceName) {
            return (Class<T>) fhirContext.getResourceDefinition(resourceName).getImplementingClass();
        }
    }

    @Nested
    public class Dstu3 implements INpmBackedRepositoryTest {

        private final FhirContext fhirContext = FhirContext.forDstu3Cached();

        @Override
        public FhirContext getFhirContext() {
            return fhirContext;
        }

        @Override
        public IBaseResource createLibraryResource(String name, String canonicalUrl) {
            var library = new org.hl7.fhir.dstu3.model.Library();
            library.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
            library.setUrl(canonicalUrl);
            library.setName(name);
            return library;
        }

        @Override
        public IBaseResource createMeasureResource(String canonicalUrl) {
            var measure = new org.hl7.fhir.dstu3.model.Measure();
            measure.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
            measure.setUrl(canonicalUrl);
            return measure;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends IBaseResource> Class<T> getResourceClass(String resourceName) {
            return (Class<T>) fhirContext.getResourceDefinition(resourceName).getImplementingClass();
        }
    }
}
