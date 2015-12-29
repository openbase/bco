/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.registry.dbconvert.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.processing.StringProcessor;
import org.dc.jul.storage.registry.AbstractVersionConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.FileSynchronizedRegistryInterface;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import org.dc.jul.storage.registry.version.DBVersionControl;
import de.citec.lm.remote.LocationRegistryRemote;
import java.util.HashMap;
import java.util.Map;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceConfig_1_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;
    private final Map<String, String> locationLabelIdMap;

    public DeviceConfig_1_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistryInterface<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder>> registry) throws InstantiationException, InterruptedException {
        super(versionControl, registry);
        System.out.println("Constructor call");
        try {
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationLabelIdMap = new HashMap<>();

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        System.out.println("processData:" + entry.getId());
        if (!locationRegistryRemote.isActive()) {
            System.out.println("activate location registry");
            try {
                locationRegistryRemote.activate();
                System.out.println("read location configs.");
                String oldID;
                for (LocationConfig locationConfig : locationRegistryRemote.getLocationConfigs()) {
                    oldID = oldGenerateId(locationConfig);
                    if (!locationLabelIdMap.containsKey(oldID)) {
                        locationLabelIdMap.put(oldID, locationConfig.getId());
                        System.out.println("register mapping old[" + oldID + "] = new[" + locationConfig.getId() + "]");
                    }
                }
            } catch (InterruptedException ex) {
                System.out.println("read location configs interrupted.");
                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
//                throw new RuntimeException();
            }
        }

        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        if (!deviceConfig.hasPlacementConfig()) {
            System.out.println("no placement avail");
            throw new NotAvailableException("deviceConfig.placementconfig");
        }

        if (!deviceConfig.getPlacementConfig().hasLocationId()) {
            System.out.println("no location id avail");
            throw new NotAvailableException("deviceConfig.placementconfig.locationid");
        }

        boolean modification = false;
        if (locationLabelIdMap.containsKey(deviceConfig.getPlacementConfig().getLocationId())) {
            System.out.println("Update Device[" + deviceConfig.getId() + "] Location id from [" + deviceConfig.getPlacementConfig().getLocationId() + "] to [" + locationLabelIdMap.get(deviceConfig.getPlacementConfig().getLocationId()) + "]");
            deviceConfig.setPlacementConfig(PlacementConfig.newBuilder(deviceConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(deviceConfig.getPlacementConfig().getLocationId())));
            modification = true;
        } else {
            System.out.println("Could not resolve device location id[" + deviceConfig.getPlacementConfig().getLocationId() + "]");
        }

        deviceConfig.clearUnitConfig();
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {

            // Check if placement is available
            if (!unitConfig.hasPlacementConfig()) {
                throw new NotAvailableException("unit.placementconfig");
            }

            if (!unitConfig.getPlacementConfig().hasLocationId()) {
                throw new NotAvailableException("unit.placementconfig.locationid");
            }

            if (locationLabelIdMap.containsKey(unitConfig.getPlacementConfig().getLocationId())) {
                System.out.println("Update Unit[" + unitConfig.getId() + "] Location id from [" + unitConfig.getPlacementConfig().getLocationId() + "] to [" + locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId()) + "]");
                unitConfig.setPlacementConfig(PlacementConfig.newBuilder(unitConfig.getPlacementConfig()).setLocationId(locationLabelIdMap.get(unitConfig.getPlacementConfig().getLocationId())));
                modification = true;
            } else {
                System.out.println("Could not resolve unit location id[" + unitConfig.getPlacementConfig().getLocationId() + "]");
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    /**
     * This is the old location id generator used for id recovery.
     *
     * @param message
     * @return
     * @throws CouldNotPerformException
     */
    public String oldGenerateId(LocationConfig message) throws CouldNotPerformException {
        try {
            if (!message.hasLabel()) {
                throw new InvalidStateException("Field [locationConfig.label] is missing!");
            }

            if (message.getLabel().isEmpty()) {
                throw new InvalidStateException("Field [Label] is empty!");
            }

            return StringProcessor.transformToIdString(message.getLabel());

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }

}
