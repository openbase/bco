package org.openbase.bco.registry.unit.core.consistency.app;

import org.openbase.bco.registry.lib.util.LocationUtils;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppLocationConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;

    public AppLocationConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig app = entry.getMessage();

        if (app.hasPlacementConfig() || !app.getPlacementConfig().hasLocationId() || app.getPlacementConfig().getLocationId().isEmpty()) {
            String rootLocationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
            PlacementConfig rootPlacement = PlacementConfig.newBuilder().setLocationId(rootLocationId).build();
            throw new EntryModification(entry.setMessage(app.toBuilder().setPlacementConfig(rootPlacement)), this);
        }

        // verify if configured location exists.
        if (!locationRegistry.contains(app.getPlacementConfig().getLocationId())) {
            try {
                if (!JPService.getProperty(JPRecoverDB.class).getValue()) {
                    throw new InvalidStateException("The configured Location[" + app.getPlacementConfig().getLocationId() + "] of App[" + app.getId() + "] is unknown!");
                }
            } catch (JPServiceException ex) {
                throw new InvalidStateException("The configured Location[" + app.getPlacementConfig().getLocationId() + "] of App[" + app.getId() + "] is unknown and can not be recovered!", ex);
            }
            // recover agent location with root location.
            String rootLocationId = LocationUtils.getRootLocation(locationRegistry.getMessages()).getId();
            PlacementConfig rootPlacement = PlacementConfig.newBuilder().setLocationId(rootLocationId).build();
            throw new EntryModification(entry.setMessage(app.toBuilder().setPlacementConfig(rootPlacement)), this);
        }
    }
}
