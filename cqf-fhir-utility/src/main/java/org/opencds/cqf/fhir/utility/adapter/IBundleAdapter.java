package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;

public interface IBundleAdapter extends IResourceAdapter {

    List<? extends IBundleEntryComponentAdapter> getEntry();
}
