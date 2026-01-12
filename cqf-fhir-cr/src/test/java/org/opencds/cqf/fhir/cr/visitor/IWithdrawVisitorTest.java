package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.CanonicalBundleEntry;
import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public interface IWithdrawVisitorTest {

    FhirContext fhirContext();

    IRepository getRepo();

    <T extends IBaseResource> T createFromResourceLocation(String resourceLocation);

    Class<? extends IBaseResource> libraryClass();

    IAdapterFactory getAdapterFactory();

    IBaseParameters createParametersForWithdrawVisitor(String version);

    // this test largely tests BaseKnowledgeArtifactVisitor...
    @Test
    default void library_withdraw_test() {
        // setup
        IBaseBundle bundle = createFromResourceLocation("Bundle-small-draft.json");

        IBaseBundle tsBundle = getRepo().transaction(bundle);
        IIdType id;
        {
            List<CanonicalBundleEntry> entries = BundleUtil.toListOfCanonicalBundleEntries(fhirContext(), tsBundle);
            id = fhirContext().getVersion().newIdType().setValue(entries.get(0).getResponseLocation());
        }

        String version = "1.2.3-draft";
        var library = getRepo().read(libraryClass(), id);

        ILibraryAdapter libraryAdapter = getAdapterFactory().createLibrary(library);

        IKnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor(getRepo());
        var params = createParametersForWithdrawVisitor(version);

        // test
        IBaseBundle returnedBundle = (IBaseBundle) libraryAdapter.accept(withdrawVisitor, params);

        // verify
        List<BundleEntryParts> entries = BundleUtil.toListOfEntries(fhirContext(), returnedBundle);

        assertEquals(4, entries.size());
    }

    @Test
    default void library_withdraw_with_approval_test() {
        IBaseBundle bundle = createFromResourceLocation("Bundle-small-approved-draft.json");
        IBaseResource sp = createFromResourceLocation("SearchParameter-artifactAssessment.json");

        IBaseBundle tsBundle = getRepo().transaction(bundle);
        getRepo().update(sp);

        IIdType id;
        {
            List<CanonicalBundleEntry> entries = BundleUtil.toListOfCanonicalBundleEntries(fhirContext(), tsBundle);
            id = fhirContext().getVersion().newIdType().setValue(entries.get(0).getResponseLocation());
        }
        String version = "1.2.3-draft";
        IBaseResource library = getRepo().read(libraryClass(), id);
        ILibraryAdapter libraryAdapter = getAdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor(getRepo());
        var params = createParametersForWithdrawVisitor(version);

        // test
        IBaseBundle returnedBundle = (IBaseBundle) libraryAdapter.accept(withdrawVisitor, params);

        // verify
        List<BundleEntryParts> entries = BundleUtil.toListOfEntries(fhirContext(), returnedBundle);
        assertEquals(5, entries.size());
    }

    @Test
    default void library_withdraw_No_draft_test() {
        IBaseBundle bundle = createFromResourceLocation("Bundle-ersd-small-active.json");
        IBaseBundle bundlets = getRepo().transaction(bundle);
        String version = "1.2.3";
        IIdType id = fhirContext().getVersion().newIdType().setValue("Library/SpecificationLibrary");
        IBaseResource library = getRepo().read(libraryClass(), id);

        ILibraryAdapter libraryAdapter = getAdapterFactory().createLibrary(library);
        IKnowledgeArtifactVisitor withdrawVisitor = new WithdrawVisitor(getRepo());
        var params = createParametersForWithdrawVisitor(version);

        var exception =
                assertThrows(PreconditionFailedException.class, () -> libraryAdapter.accept(withdrawVisitor, params));
        assertTrue(exception.getMessage().contains("Cannot withdraw an artifact that is not in draft status"));
    }
}
