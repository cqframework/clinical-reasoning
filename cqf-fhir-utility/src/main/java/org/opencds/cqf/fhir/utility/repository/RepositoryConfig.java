package org.opencds.cqf.fhir.utility.repository;

// TODO: Hmm... perhaps this should be a class and a builder rather than an enum?
public enum RepositoryConfig {
    EXTRA_FLAT(ResourceTypeLayoutMode.FLAT, ResourceCategoryLayoutMode.FLAT, ResourceFilenameMode.ID_ONLY),
    FLAT(ResourceTypeLayoutMode.FLAT, ResourceCategoryLayoutMode.FLAT, ResourceFilenameMode.TYPE_AND_ID),
    WITH_CATEGORY_DIRECTORY(
            ResourceTypeLayoutMode.FLAT,
            ResourceCategoryLayoutMode.DIRECTORY_PER_CATEGORY,
            ResourceFilenameMode.TYPE_AND_ID),
    WITH_CATEGORY_AND_TYPE_DIRECTORIES(
            ResourceTypeLayoutMode.DIRECTORY_PER_TYPE,
            ResourceCategoryLayoutMode.DIRECTORY_PER_CATEGORY,
            ResourceFilenameMode.ID_ONLY),
    FULL_REDUNDANT(
            ResourceTypeLayoutMode.DIRECTORY_PER_TYPE,
            ResourceCategoryLayoutMode.DIRECTORY_PER_CATEGORY,
            ResourceFilenameMode.TYPE_AND_ID);

    private RepositoryConfig(
            ResourceTypeLayoutMode typeLayout,
            ResourceCategoryLayoutMode categoryLayout,
            ResourceFilenameMode filenameMode) {
        this.typeLayout = typeLayout;
        this.categoryLayout = categoryLayout;
        this.filenameMode = filenameMode;
    }

    private final ResourceTypeLayoutMode typeLayout;
    private final ResourceCategoryLayoutMode categoryLayout;
    private final ResourceFilenameMode filenameMode;

    public ResourceTypeLayoutMode typeLayout() {
        return typeLayout;
    }

    public ResourceCategoryLayoutMode categoryLayout() {
        return categoryLayout;
    }

    public ResourceFilenameMode filenameMode() {
        return filenameMode;
    }
}
