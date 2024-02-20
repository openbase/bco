package org.openbase.bco.registry.unit.core.consistency.locationconfig

import org.openbase.bco.registry.lib.util.LocationUtils
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.exception.printer.LogLevel
import org.openbase.jul.extension.protobuf.IdentifiableMessage
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler
import org.openbase.jul.storage.registry.EntryModification
import org.openbase.jul.storage.registry.ProtoBufRegistry
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType

/**
 *
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class LocationTypeConsistencyHandler :
    AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder>() {
    @Throws(CouldNotPerformException::class, EntryModification::class)
    override fun processData(
        id: String,
        entry: IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>,
        entryMap: ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder>,
        registry: ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder>,
    ) {
        val locationUnit = entry.message.toBuilder()
        val locationConfig = locationUnit.locationConfigBuilder

        val detectedType: LocationType = LocationUtils.detectLocationType(entry.message, registry)

        if (!locationConfig.hasLocationType()) {
            try {
                locationConfig.setLocationType(detectedType)
                throw EntryModification(entry.setMessage(locationUnit, this), this)
            } catch (ex: CouldNotPerformException) {
                throw CouldNotPerformException(
                    "The locationType of location[" + locationUnit.label + "] has to be defined manually",
                    ex
                )
            }
        } else {
            try {
                if (detectedType != locationConfig.locationType) {
                    locationConfig.setLocationType(detectedType)
                    throw EntryModification(entry.setMessage(locationUnit, this), this)
                }
            } catch (ex: CouldNotPerformException) {
                ExceptionPrinter.printHistory(
                    "Could not detect locationType for location[" + locationUnit.label + "] with current type [" + locationConfig.locationType.name + "]",
                    ex,
                    logger,
                    LogLevel.DEBUG
                )
            }
        }
    }
}
