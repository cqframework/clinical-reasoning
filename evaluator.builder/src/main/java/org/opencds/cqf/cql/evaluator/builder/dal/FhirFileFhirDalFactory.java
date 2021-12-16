package org.opencds.cqf.cql.evaluator.builder.dal;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.context.FhirContext;

@Named
public class FhirFileFhirDalFactory implements TypedFhirDalFactory {

    private FhirContext fhirContext;
    private DirectoryBundler directoryBundler;

    @Inject
    public FhirFileFhirDalFactory(FhirContext fhirContent, DirectoryBundler directoryBundler) {
        this.fhirContext = requireNonNull(fhirContent, "fhirContext can not be null");
        this.directoryBundler = requireNonNull(directoryBundler, "directoryBundler can not be null");
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_FILES;
    }

    @Override
    public FhirDal create(String url, List<String> headers) {
        IBaseBundle bundle = this.directoryBundler.bundle(url);
        return new BundleFhirDal(this.fhirContext, bundle);
    }
    
}
