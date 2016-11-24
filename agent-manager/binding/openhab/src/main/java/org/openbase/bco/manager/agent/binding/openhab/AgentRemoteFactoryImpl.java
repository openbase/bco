package org.openbase.bco.manager.agent.binding.openhab;

/*
 * #%L
 * COMA AgentManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.bco.manager.agent.binding.openhab.execution.OpenHABCommandFactory;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SUBSEGMENT_DELIMITER;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.Factory;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.agent.AgentDataType.AgentData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AgentRemoteFactoryImpl implements Factory<AgentRemote, UnitConfig> {

    private OpenHABRemote openHABRemote;

    public AgentRemoteFactoryImpl() {
    }

    public void init(final OpenHABRemote openHABRemote) {
        this.openHABRemote = openHABRemote;
    }

    @Override
    public AgentRemote newInstance(UnitConfig config) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        AgentRemote agentRemote = new AgentRemote();
        try {
            agentRemote.addDataObserver((final Observable<AgentData> source, AgentData data) -> {
                openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(generateItemId(config)).build());
            });
            agentRemote.init(config);
            agentRemote.activate();

            return agentRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(agentRemote, ex);
        }
    }

    //TODO: method is implemented in the openhab config generator and should be used from there
    private String generateItemId(UnitConfig agentUnitConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("agent")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(agentUnitConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
