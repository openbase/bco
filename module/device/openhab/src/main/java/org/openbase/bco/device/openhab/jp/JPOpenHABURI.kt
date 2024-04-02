package org.openbase.bco.device.openhab.jp

import org.openbase.jps.core.AbstractJavaProperty
import java.net.URI

/**
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class JPOpenHABURI : AbstractJavaProperty<URI>(COMMAND_IDENTIFIERS) {
    override fun generateArgumentIdentifiers(): Array<String> {
        return ARGUMENT_IDENTIFIERS
    }

    override fun getPropertyDefaultValue(): URI {
        // URI.create does not throw an exception which is fine for the default value

        // use system variable if defined

        val systemDefinedPort = System.getenv(SYSTEM_VARIABLE_OPENHAB_PORT)
        if (systemDefinedPort != null) {
            return URI.create("http://localhost:$systemDefinedPort")
        }

        return URI.create(DEFAULT_URI)
    }

    @Throws(Exception::class)
    override fun parse(list: List<String>): URI {
        var uri = oneArgumentResult
        // make sure that the uri always starts with http ot https
        if (!uri.startsWith("http")) {
            uri = "http://$uri"
        }
        // create a new uri, this will throw an exception if the uri is not valid
        return URI(uri)
    }

    override fun getDescription(): String {
        return "Define the URI of the OpenHAB 2 instance this app should connect to."
    }

    companion object {
        private val ARGUMENT_IDENTIFIERS = arrayOf("URI")
        private val COMMAND_IDENTIFIERS = arrayOf("--openhab-url", "--openhab-uri", "--uri")

        private const val DEFAULT_URI = "http://openhab:8080"

        const val SYSTEM_VARIABLE_OPENHAB_PORT: String = "OPENHAB_HTTP_PORT"
    }
}
