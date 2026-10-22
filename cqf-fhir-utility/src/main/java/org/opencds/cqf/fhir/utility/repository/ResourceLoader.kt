package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import org.apache.commons.io.IOUtils
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource

interface ResourceLoader {
    val relativeClass: Class<*>?

    fun loadBundle(context: FhirContext, location: String): IBaseBundle {
        return readResource(context, location) as IBaseBundle
    }

    fun readResource(context: FhirContext, location: String): IBaseResource {
        val resourceString = stringFromResource(location)
        if (location.endsWith("json")) {
            return parseResource(context, "json", resourceString)
        } else {
            return parseResource(context, "xml", resourceString)
        }
    }

    fun parseResource(
        context: FhirContext,
        encoding: String,
        resourceString: String,
    ): IBaseResource {
        val parser: IParser
        when (encoding.lowercase()) {
            "json" -> parser = context.newJsonParser()
            "xml" -> parser = context.newXmlParser()
            else ->
                throw IllegalArgumentException(
                    "Expected encoding xml, or json.  $encoding is not a valid encoding"
                )
        }

        return parser.parseResource(resourceString)
    }

    fun loadResource(context: FhirContext, location: String): IBaseResource {
        val resourceString = stringFromResource(location)
        if (location.endsWith("json")) {
            return loadResource(context, "json", resourceString)
        } else {
            return loadResource(context, "xml", resourceString)
        }
    }

    fun loadResource(
        context: FhirContext,
        encoding: String,
        resourceString: String,
    ): IBaseResource {
        return parseResource(context, encoding, resourceString)
    }

    fun stringFromResource(location: String): String {
        var `is`: InputStream? = null
        try {
            val f = File(location)
            if (f.isFile()) {
                `is` = FileInputStream(location)
            } else {
                `is` = this.relativeClass!!.getResourceAsStream(location)
            }
            return IOUtils.toString(`is`, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Error loading resource from $location", e)
        }
    }
}
