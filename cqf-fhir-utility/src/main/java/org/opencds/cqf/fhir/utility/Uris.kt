package org.opencds.cqf.fhir.utility

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException

object Uris {
    fun isUri(uri: String?): Boolean {
        if (uri == null) {
            return false
        }

        return uri.startsWith("file:/") || uri.matches("\\w+?://.*".toRegex())
    }

    @JvmStatic
    fun isFileUri(uri: String?): Boolean {
        if (uri == null) {
            return false
        }

        return uri.startsWith("file") || !uri.matches("\\w+?://.*".toRegex())
    }

    @JvmStatic
    @Throws(MalformedURLException::class, URISyntaxException::class)
    fun ensureHttps(urlString: String): String {
        val url = URI.create(urlString).toURL()

        // Check if the protocol is already HTTPS
        if ("https".equals(url.protocol, ignoreCase = true)) {
            return urlString
        }

        // Construct a new URL with the HTTPS protocol
        val httpsUrl =
            URI(
                "https", // scheme
                null, // userinfo
                url.host, // host
                url.port, // port
                url.file, // path
                null, // query
                null, // fragment
            )
        return httpsUrl.toString()
    }
}
