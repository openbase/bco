package org.openbase.bco.device.openhab.jp

import org.openbase.jps.exception.JPNotAvailableException
import org.openbase.jps.preset.AbstractJPDirectory
import org.openbase.jps.tools.FileHandler
import java.io.File

/**
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 */
class JPOpenHABConfiguration :
    AbstractJPDirectory(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off) {
    @Throws(JPNotAvailableException::class)
    override fun getPropertyDefaultValue(): File {
        // use system variable if defined

        val systemDefinedPath = System.getenv(SYSTEM_VARIABLE_OPENHAB_CONF)
        if (systemDefinedPath != null) {
            return File(systemDefinedPath)
        }

        return File(DEFAULT_PATH)
    }

    override fun getDescription(): String {
        return "Defines the openhab configuration directory. This property is based on the system variable " + SYSTEM_VARIABLE_OPENHAB_CONF
    }

    companion object {
        val COMMAND_IDENTIFIERS: Array<String> = arrayOf("--openhab-config")
        const val SYSTEM_VARIABLE_OPENHAB_CONF: String = "OPENHAB_CONF"
        const val DEFAULT_PATH: String = "/etc/openhab2"
    }
}
