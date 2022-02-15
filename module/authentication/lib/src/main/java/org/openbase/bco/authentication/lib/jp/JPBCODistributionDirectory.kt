package org.openbase.bco.authentication.lib.jp

import org.openbase.jps.core.JPService
import org.openbase.jps.exception.JPNotAvailableException
import org.openbase.jps.preset.AbstractJPDirectory
import org.openbase.jps.preset.JPSystemDirectory
import org.openbase.jps.preset.JPTmpDirectory
import org.openbase.jps.tools.FileHandler.AutoMode.Off
import org.openbase.jps.tools.FileHandler.ExistenceHandling.Must
import java.io.File

class JPBCODistributionDirectory: AbstractJPDirectory(
    arrayOf("--dist", "--distribution"),
    Must,
    Off
) {
    init {
        registerDependingProperty(JPSystemDirectory::class.java)
    }

    @Throws(JPNotAvailableException::class)
    override fun getPropertyDefaultValue(): File? {
        if (JPService.testMode()) {
            return JPService.getProperty(JPTmpDirectory::class.java).value
        }

        return System.getenv("BCO_DIST")
            // load prefix via system variable
            ?.let { File(it) }
            // use global usr folder as prefix
            ?: JPService.getProperty(JPSystemDirectory::class.java).value
    }

    override fun getDescription() =
        "Set the application prefix, which is used for accessing binaries, shared data and templates."
}
