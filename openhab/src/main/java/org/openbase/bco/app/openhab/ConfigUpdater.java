package org.openbase.bco.app.openhab;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.app.openhab.registry.synchronizer.ThingDeviceUnitSynchronizer;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConfigUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUpdater.class);

    public static void main(String[] args) {
        try {
            Registries.waitForData();
            SessionManager.getInstance().login(Registries.getUnitRegistry().getUserUnitIdByUserName("admin"), "admin");

            final Map<DeviceClass, List<UnitConfig>> deviceByClassMap = new HashMap<>();
            for (final UnitConfig deviceUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE)) {
                final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
                if (!deviceByClassMap.containsKey(deviceClass)) {
                    deviceByClassMap.put(deviceClass, new ArrayList<>());
                }
                deviceByClassMap.get(deviceClass).add(deviceUnitConfig);
            }

            for (final DeviceClass deviceClass : deviceByClassMap.keySet()) {
                for (final UnitConfig unitConfig : deviceByClassMap.get(deviceClass)) {
                    final MetaConfigPool metaConfigPool = new MetaConfigPool();
                    metaConfigPool.register(new MetaConfigVariableProvider(unitConfig.getAlias(0) + "MetaConfig", unitConfig.getMetaConfig()));

                    String thingUID;
                    UnitConfig openhab2Device;
                    UnitConfig.Builder bcoDevice;
                    try {
                        thingUID = metaConfigPool.getValue(ThingDeviceUnitSynchronizer.OPENHAB_THING_UID_KEY);
                        openhab2Device = unitConfig;
                        LOGGER.info("Found openhab device[" + openhab2Device.getAlias(0) + "]");
                    } catch (NotAvailableException ex) {
                        // continue because not an openhab2 device
                        continue;
                    }

                    try {
                        bcoDevice = getOldDevice(thingUID, deviceClass, deviceByClassMap.get(deviceClass)).toBuilder();
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Could not find device corresponding to openhab2 device[" + openhab2Device.getAlias(0) + "]");
                        continue;
                    }

                    Entry.Builder builder = bcoDevice.getMetaConfigBuilder().addEntryBuilder();
                    builder.setKey(ThingDeviceUnitSynchronizer.OPENHAB_THING_UID_KEY);
                    builder.setValue(thingUID);

                    List<UnitConfig.Builder> dalUnitList = new ArrayList<>();
                    for (final String dalUnitId1 : bcoDevice.getDeviceConfig().getUnitIdList()) {
                        final UnitConfig.Builder dalUnit1 = Registries.getUnitRegistry().getUnitConfigById(dalUnitId1).toBuilder();

                        for (final String dalUnitId2 : openhab2Device.getDeviceConfig().getUnitIdList()) {
                            final UnitConfig dalUnit2 = Registries.getUnitRegistry().getUnitConfigById(dalUnitId2);

                            if (dalUnit1.getUnitTemplateConfigId().equals(dalUnit2.getUnitTemplateConfigId())) {
                                for (String alias : dalUnit2.getAliasList()) {
                                    dalUnit1.addAlias(alias);
                                }
                                dalUnitList.add(dalUnit1);
                                break;
                            }
                        }
                    }

                    // TODO: this leads to potentially many units config is that right
                    LOGGER.info("Remove openHAB device");
                    Registries.getUnitRegistry().removeUnitConfig(openhab2Device).get();
                    LOGGER.info("Update BCO device");
                    Registries.getUnitRegistry().updateUnitConfig(bcoDevice.build()).get();
                    LOGGER.info("Update its dal units");
                    for (final UnitConfig.Builder dalUnit : dalUnitList) {
                        Registries.getUnitRegistry().updateUnitConfig(dalUnit.build()).get();
                    }
                }
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not sync config", ex, LOGGER);
        }

        System.exit(0);
    }

    private static UnitConfig getOldDevice(final String thingUID, final DeviceClass deviceClass, final List<UnitConfig> devices) throws NotAvailableException {
        if (deviceClass.getCompany().equalsIgnoreCase("Fibaro") || deviceClass.getCompany().equalsIgnoreCase("Philips")) {
            final String nodeId = thingUID.split(":")[3].replace("node", "");
            LOGGER.info("Found nodeId[" + nodeId + "]");
            for (final UnitConfig deviceUnitConfig : devices) {

                final MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(deviceUnitConfig.getAlias(0) + "MetaConfig", deviceUnitConfig.getMetaConfig()));

                String deviceId;
                try {
                    deviceId = metaConfigPool.getValue("OPENHAB_BINDING_DEVICE_ID");
                    LOGGER.info("Parsed device id[" + deviceId + "] for device[" + deviceUnitConfig.getAlias(0) + "]");
                } catch (NotAvailableException ex) {
                    // ignore because not the correct device
                    continue;
                }

                if (deviceId.equals(nodeId)) {
                    return deviceUnitConfig;
                }
            }
        }

        throw new NotAvailableException("Could not resolve device for [" + thingUID + ", " + LabelProcessor.getBestMatch(deviceClass.getLabel()) + "]");
    }
}
