package org.openbase.bco.device.openhab.jp

import org.openbase.jps.core.JPService
import org.openbase.jps.exception.JPNotAvailableException
import org.openbase.jps.preset.AbstractJPDirectory
import org.openbase.jps.tools.FileHandler
import java.io.File

class JPOpenHABSitemap :
    AbstractJPDirectory(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off) {
    init {
        registerDependingProperty(JPOpenHABConfiguration::class.java)
    }

    @Throws(JPNotAvailableException::class)
    override fun getPropertyDefaultValue(): File {
        return File(JPService.getProperty(JPOpenHABConfiguration::class.java).value, "sitemaps")
    }

    override fun getDescription(): String {
        return "Defines the path to the openhab sitemap directory."
    }

    companion object {
        private val COMMAND_IDENTIFIERS = arrayOf("--sitemap")
    }
}
