package org.opencds.cqf.cql.evaluator.fhir.npm;

import org.cqframework.fhir.npm.NpmPackageManager;
import org.cqframework.fhir.utilities.IGContext;
import org.cqframework.fhir.utilities.exception.IGInitializationException;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.utilities.npm.NpmPackage;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

//@Named
public class NpmProcessor {
    /**
     * Provides access to the Npm package manager. Note that this will be throw an exception in the case that
     * there is no ig context.
     */
    private NpmPackageManager packageManager;
    public NpmPackageManager getPackageManager() {
        if (this.packageManager == null) {
            throw new IllegalStateException("Package manager is not available outside of an ig context");
        }
        return this.packageManager;
    }

    /**
     * The igContext for the npmProcessor (i.e. the root IG that defines dependencies accessible in the context)
     * Note that this may be null in the case that there is no IG context
     */
    private IGContext igContext;
    public IGContext getIgContext() {
        return this.igContext;
    }

    //@Inject
    public NpmProcessor(IGContext igContext) {
        this.igContext = igContext;
        if (igContext != null) {
            try {
                packageManager = new NpmPackageManager(igContext.getSourceIg(), igContext.getFhirVersion());
            } catch (IOException e) {
                String message = String.format("Exceptions occurred loading npm package manager from source Ig: %s",
                        igContext.getSourceIg().getName());
                igContext.logMessage(message);
                throw new IGInitializationException(message, e);
            }
        }
    }

    public List<NamespaceInfo> getNamespaces() {
        List<NamespaceInfo> namespaceInfos = new ArrayList<NamespaceInfo>();
        if (packageManager != null) {
            List<NpmPackage> packages = packageManager.getNpmList();
            for (NpmPackage p : packages) {
                if (p.name() != null && !p.name().isEmpty() && p.canonical() != null && !p.canonical().isEmpty()) {
                    NamespaceInfo ni = new NamespaceInfo(p.name(), p.canonical());
                    namespaceInfos.add(ni);
                }
            }
        }
        return namespaceInfos;
    }
}
