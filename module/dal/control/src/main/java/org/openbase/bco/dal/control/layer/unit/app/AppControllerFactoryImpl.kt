package org.openbase.bco.dal.control.layer.unit.app

import org.openbase.bco.dal.lib.layer.unit.app.App
import org.openbase.bco.dal.lib.layer.unit.app.AppController
import org.openbase.bco.dal.lib.layer.unit.app.AppControllerFactory
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.extension.type.processing.LabelProcessor.getBestMatch
import org.openbase.jul.extension.type.processing.LabelProcessor.getLabelByLanguage
import org.openbase.jul.processing.StringProcessor
import org.openbase.type.domotic.unit.UnitConfigType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class AppControllerFactoryImpl private constructor() : AppControllerFactory {
    protected val logger: Logger = LoggerFactory.getLogger(AppControllerFactoryImpl::class.java)

    @Throws(org.openbase.jul.exception.InstantiationException::class)
    override fun newInstance(appUnitConfig: UnitConfigType.UnitConfig): AppController {
        var app: AppController
        try {

            Registries.waitForData()
            val appClass = Registries.getClassRegistry().getAppClassById(appUnitConfig.appConfig.appClassId)

            try {
                // try to load preset app
                val className = (PRESET_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(
                    getLabelByLanguage(
                        Locale.ENGLISH,
                        appClass.label
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: CouldNotPerformException) {
                // try to load custom app
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: ClassNotFoundException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: SecurityException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: InstantiationException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: IllegalAccessException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: IllegalArgumentException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: NoSuchMethodException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            } catch (ex: InvocationTargetException) {
                val className = (CUSTOM_APP_PACKAGE_PREFIX
                        + "." + StringProcessor.removeWhiteSpaces(getLabelByLanguage(Locale.ENGLISH, appClass.label))
                    .lowercase(
                        Locale.getDefault()
                    )
                        + "." + StringProcessor.transformToPascalCase(
                    StringProcessor.removeWhiteSpaces(
                        getLabelByLanguage(
                            Locale.ENGLISH, appClass.label
                        )
                    )
                ) + "App")
                app = Thread.currentThread().contextClassLoader.loadClass(className).getConstructor()
                    .newInstance() as AppController
            }
            logger.debug("Creating app of type [" + getBestMatch(appClass.label) + "]")
            app.init(appUnitConfig)
        } catch (ex: CouldNotPerformException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: ClassNotFoundException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: SecurityException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: InstantiationException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: IllegalAccessException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: IllegalArgumentException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: InterruptedException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: NoSuchMethodException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        } catch (ex: InvocationTargetException) {
            throw org.openbase.jul.exception.InstantiationException(App::class.java, appUnitConfig.id, ex)
        }
        return app
    }

    companion object {
        @get:Synchronized
        var instance: AppControllerFactoryImpl = AppControllerFactoryImpl()
            private set

        private const val PRESET_APP_PACKAGE_PREFIX = "org.openbase.bco.app.preset"
        private const val CUSTOM_APP_PACKAGE_PREFIX = "org.openbase.bco.app"
    }
}
