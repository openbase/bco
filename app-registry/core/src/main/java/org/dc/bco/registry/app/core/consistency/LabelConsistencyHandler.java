/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.core.consistency;

import java.util.HashMap;
import java.util.Map;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.app.AppConfigType.AppConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AppConfig, AppConfig.Builder> {

    private final Map<String, AppConfig> appMap;

    public LabelConsistencyHandler() {
        this.appMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, AppConfig, AppConfig.Builder> entry, ProtoBufMessageMapInterface<String, AppConfig, AppConfig.Builder> entryMap, ProtoBufRegistryInterface<String, AppConfig, AppConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AppConfig app = entry.getMessage();

        if (!app.hasLabel() || app.getLabel().isEmpty()) {
            throw new NotAvailableException("app.label");
        }

        if (!app.hasLocationId() || app.getLocationId().isEmpty()) {
            throw new NotAvailableException("app.locationId");
        }

        String key = app.getLabel() + app.getLocationId();
        if (!appMap.containsKey(key)) {
            appMap.put(key, app);
        } else {
            throw new InvalidStateException("App [" + app + "] and app [" + appMap.get(key) + "] are registered with the same label at the same location");
        }
    }

    @Override
    public void reset() {
        appMap.clear();
    }
}
