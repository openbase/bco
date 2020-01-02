package org.openbase.bco.device.openhab;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.device.openhab.registry.synchronizer.SynchronizationProcessor;
import org.openbase.bco.registry.clazz.core.consistency.KNXDeviceClassConsistencyHandler;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.configuration.EntryType.Entry.Builder;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            SessionManager.getInstance().loginUser(Registries.getUnitRegistry().getUserUnitIdByUserName("admin"), "admin", true);

            final Map<String, List<UnitConfig>> companyDeviceClassMap = new HashMap<>();
            for (final DeviceClass deviceClass : Registries.getClassRegistry().getDeviceClasses()) {
                // ignore knx device classes
                if (KNXDeviceClassConsistencyHandler.isKNXDeviceClass(deviceClass)) {
                    continue;
                }

                if (!companyDeviceClassMap.containsKey(deviceClass.getCompany())) {
                    companyDeviceClassMap.put(deviceClass.getCompany(), new ArrayList<>());
                }

                for (final UnitConfig deviceUnitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.DEVICE)) {
                    if (deviceUnitConfig.getDeviceConfig().getDeviceClassId().equals(deviceClass.getId())) {
                        companyDeviceClassMap.get(deviceClass.getCompany()).add(deviceUnitConfig);
                    }
                }
            }

            for (final String company : companyDeviceClassMap.keySet()) {
                LOGGER.info("Check company {}", company);
                for (final UnitConfig unitConfig : companyDeviceClassMap.get(company)) {
                    LOGGER.info("Check device {}", UnitConfigProcessor.getDefaultAlias(unitConfig, "?"));
                    final MetaConfigPool metaConfigPool = new MetaConfigPool();
                    metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "MetaConfig", unitConfig.getMetaConfig()));

                    String thingUID;
                    UnitConfig openhab2Device;
                    UnitConfig.Builder bcoDevice;
                    try {
                        thingUID = metaConfigPool.getValue(SynchronizationProcessor.OPENHAB_THING_UID_KEY);
                        openhab2Device = unitConfig;
                    } catch (NotAvailableException ex) {
                        // continue because not an openhab2 device
                        continue;
                    }

                    try {
                        if (getNodeId(thingUID).equals(getDeviceId(openhab2Device))) {
                            // skip already updated devices
                            continue;
                        }
                    } catch (NotAvailableException ex) {
                        // go on because openhab device does not have a node entry in the meta config
                    }

                    try {
                        bcoDevice = getOldDevice(thingUID, company, companyDeviceClassMap.get(company)).toBuilder();
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Could not find device corresponding to openhab2 device[" + openhab2Device.getAlias(0) + "]");
                        continue;
                    }

                    LOGGER.info("Found BCODevice[" + bcoDevice.getAlias(0) + "] matching openHAB device[" + openhab2Device.getAlias(0) + "]");

                    // add meta config entry to bco device
                    for (Entry entry : openhab2Device.getMetaConfig().getEntryList()) {
                        if (entry.getKey().equals(SynchronizationProcessor.OPENHAB_THING_UID_KEY)) {
                            Builder builder = bcoDevice.getMetaConfigBuilder().addEntryBuilder();
                            builder.setKey(entry.getKey());
                            builder.setValue(entry.getValue());
                        }
                    }
                    LOGGER.info("Update BCO device[" + bcoDevice.getAlias(0) + "]");
                    Registries.getUnitRegistry().updateUnitConfig(bcoDevice.build()).get();
                    LOGGER.info("Remove openHAB device");
                    Registries.getUnitRegistry().removeUnitConfig(openhab2Device).get();
                }
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not sync config", ex, LOGGER);
        }


        System.exit(0);
    }

    private static UnitConfig getOldDevice(final String thingUID, final String company, final List<UnitConfig> devices) throws NotAvailableException {
        if (company.equalsIgnoreCase("fibaro") || company.equalsIgnoreCase("philips") || company.equalsIgnoreCase("homematic")) {
            final String nodeId = getNodeId(thingUID);
            for (final UnitConfig deviceUnitConfig : devices) {
                String deviceId;
                try {
                    deviceId = getDeviceId(deviceUnitConfig);
                } catch (NotAvailableException ex) {
                    // ignore because not the correct device
                    continue;
                }

                if (deviceId.equals(nodeId)) {
                    return deviceUnitConfig;
                }
            }
        }

        throw new NotAvailableException("Could not resolve device for [" + thingUID + "]");
    }

    private static String getDeviceId(final UnitConfig unitConfig) throws NotAvailableException {
        final MetaConfigPool metaConfigPool = new MetaConfigPool();
        metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "MetaConfig", unitConfig.getMetaConfig()));
        return metaConfigPool.getValue("OPENHAB_BINDING_DEVICE_ID");
    }

    private static String getNodeId(String thingUID) throws NotAvailableException {
        final String[] split = thingUID.split(":");
        if (split.length < 4) {
            throw new NotAvailableException("node id of thin " + thingUID);
        }
        return split[3].replace("node", "");
    }
}
