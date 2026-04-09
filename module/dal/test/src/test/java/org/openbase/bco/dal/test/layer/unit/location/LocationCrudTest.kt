package org.openbase.bco.dal.test.layer.unit.location

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.registry.remote.Registries
import org.openbase.jps.core.JPService
import org.openbase.jps.preset.JPDebugMode
import org.openbase.jps.preset.JPVerbose
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate
import org.openbase.type.domotic.unit.location.LocationConfigType
import java.util.concurrent.TimeUnit

/**
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class LocationCrudTest : AbstractBCOLocationManagerTest() {

    @Test
//  @Timeout(10)
    @Throws(Exception::class)
    fun createTileTest() {
        println("createTileTest")

        val wonderTile = UnitConfig.newBuilder()
            .apply { locationConfigBuilder.setLocationType(LocationConfigType.LocationConfig.LocationType.TILE) }
            .setUnitType(UnitTemplate.UnitType.LOCATION)
            .setLabel(LabelProcessor.buildLabel("WonderTile"))
            .build()


        val savedWonderTile = Registries.getUnitRegistry(true).registerUnitConfig(wonderTile).get(5, TimeUnit.SECONDS)

        savedWonderTile.hasId().shouldBeTrue()
        savedWonderTile.locationConfig.locationType.shouldBeEqual(wonderTile.locationConfig.locationType)
    }

    @Test
//  @Timeout(10)
    @Throws(Exception::class)
    fun createZoneTest() {
        println("createZoneTest")

        JPService.overwriteDefaultValue(JPDebugMode::class.java, true)
        JPService.overwriteDefaultValue(JPVerbose::class.java, true)

        val wonderZone = UnitConfig.newBuilder()
            .apply { locationConfigBuilder.setLocationType(LocationConfigType.LocationConfig.LocationType.ZONE) }
            .setUnitType(UnitTemplate.UnitType.LOCATION)
            .setLabel(LabelProcessor.buildLabel("WonderZone"))
            .build()

        val savedWonderZone = Registries.getUnitRegistry(true).registerUnitConfig(wonderZone).get(5, TimeUnit.SECONDS)

        savedWonderZone.hasId().shouldBeTrue()
        savedWonderZone.locationConfig.locationType.shouldBeEqual(wonderZone.locationConfig.locationType)
    }
}
