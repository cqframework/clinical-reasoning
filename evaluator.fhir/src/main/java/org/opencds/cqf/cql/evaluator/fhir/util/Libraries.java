package org.opencds.cqf.cql.evaluator.fhir.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a utility class for content Libraries
 */

public class Libraries {

	private static final Map<FhirVersionEnum, LibraryFunctions> cachedFunctions = new ConcurrentHashMap<>();
	private static final String LIBRARY_RESOURCE_TYPE = "Library";

	private Libraries() {
	}

	/**
	 * Creates the appropriate content for a given library, library function and
	 * content type
	 *
	 * @param library          an IBase type
	 * @param libraryFunctions LibraryFunction like getContent/getVersion etc
	 * @param contentType      the library content type used like XML/JSON
	 * @return the content
	 */
	static byte[] getContent(IBaseResource library, LibraryFunctions libraryFunctions, String contentType) {
		for (IBase attachment : libraryFunctions.getAttachments().apply(library)) {
			String libraryContentType = libraryFunctions.getContentType().apply(attachment);
			if (libraryContentType != null && libraryContentType.equals(contentType)) {
				byte[] content = libraryFunctions.getContent().apply(attachment);
				if (content != null) {
					return content;
				}
			}
		}

		return null;
	}

	/**
	 * Creates the appropriate content for a given library function and content type
	 *
	 * @param library     an IBase type
	 * @param contentType the library content type used like XML/JSON
	 * @return the content
	 */
	public static byte[] getContent(IBaseResource library, String contentType) {
		checkNotNull(library);
		checkArgument(library.fhirType().equals(LIBRARY_RESOURCE_TYPE));
		checkNotNull(contentType);

		LibraryFunctions libraryFunctions = getFunctions(library);
		return getContent(library, libraryFunctions, contentType);
	}

	static LibraryFunctions getFunctions(IBaseResource library) {
		FhirVersionEnum fhirVersion = library.getStructureFhirVersionEnum();
		return cachedFunctions.computeIfAbsent(fhirVersion, Libraries::getFunctions);
	}

	static LibraryFunctions getFunctions(FhirVersionEnum fhirVersionEnum) {
		FhirContext fhirContext = FhirContext.forCached(fhirVersionEnum);

		Class<? extends IBaseResource> libraryClass = fhirContext.getResourceDefinition(LIBRARY_RESOURCE_TYPE)
				.getImplementingClass();
		Function<IBase, List<IBase>> attachments = Reflections
				.getFunction(libraryClass, "content");
		Function<IBase, String> contentType = Reflections.getPrimitiveFunction(
				fhirContext.getElementDefinition("Attachment").getImplementingClass(), "contentType");
		Function<IBase, byte[]> content = Reflections
				.getPrimitiveFunction(fhirContext.getElementDefinition("Attachment").getImplementingClass(), "data");
		Function<IBase, String> version = Reflections.getVersionFunction(libraryClass);
		return new LibraryFunctions(attachments, contentType, content, version);
	}

	/**
	 * Returns appropriate version for a given library IBase Resource type
	 *
	 * @param library an IBase type
	 * @return the Library version
	 */

	public static String getVersion(IBaseResource library) {
		checkNotNull(library);
		checkArgument(library.fhirType().equals(LIBRARY_RESOURCE_TYPE));

		LibraryFunctions libraryFunctions = getFunctions(library);
		return libraryFunctions.getVersion().apply(library);
	}

	public static final class LibraryFunctions {

		private final Function<IBase, List<IBase>> getAttachments;
		private final Function<IBase, String> getContentType;
		private final Function<IBase, byte[]> getContent;
		private final Function<IBase, String> getVersion;

		LibraryFunctions(Function<IBase, List<IBase>> getAttachments, Function<IBase, String> getContentType,
				Function<IBase, byte[]> getContent, Function<IBase, String> getVersion) {
			this.getAttachments = getAttachments;
			this.getContentType = getContentType;
			this.getContent = getContent;
			this.getVersion = getVersion;
		}

		public Function<IBase, List<IBase>> getAttachments() {
			return this.getAttachments;
		}

		public Function<IBase, String> getContentType() {
			return this.getContentType;
		}

		public Function<IBase, byte[]> getContent() {
			return this.getContent;
		}

		public Function<IBase, String> getVersion() {
			return this.getVersion;
		}
	}
}
