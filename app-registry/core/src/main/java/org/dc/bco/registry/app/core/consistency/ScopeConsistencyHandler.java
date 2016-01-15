/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.core.consistency;

import java.util.Map;
import java.util.TreeMap;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AppConfig, AppConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;
    private final Map<String, AppConfig> appMap;

    public ScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
        this.appMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, AppConfig, AppConfig.Builder> entry, ProtoBufMessageMapInterface<String, AppConfig, AppConfig.Builder> entryMap, ProtoBufRegistryInterface<String, AppConfig, AppConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AppConfig appConfig = entry.getMessage();

        if (!appConfig.hasLocationId()) {
            throw new NotAvailableException("appConfig.locationId");
        }
        if (appConfig.getLocationId().isEmpty()) {
            throw new NotAvailableException("Field appConfig.locationId is empty");
        }

        Scope newScope = ScopeGenerator.generateAppScope(appConfig, locationRegistryRemote.getLocationConfigById(appConfig.getLocationId()));
//
        // verify and update scope
        if (!ScopeGenerator.generateStringRep(appConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            if (appMap.containsKey(ScopeGenerator.generateStringRep(newScope))) {
                throw new InvalidStateException("Two apps [" + appConfig + "][" + appMap.get(ScopeGenerator.generateStringRep(newScope)) + "] registered with the same label and location");
            } else {
                appMap.put(ScopeGenerator.generateStringRep(newScope), appConfig);
                entry.setMessage(appConfig.toBuilder().setScope(newScope));
                throw new EntryModification(entry, this);
            }
        }
    }

    @Override
    public void reset() {
        appMap.clear();
    }
}
