package org.openbase.bco.manager.agent.binding.openhab;

/*
 * #%L
 * BCO Manager Agent Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class AgentBindingOpenHABLauncher extends AbstractLauncher<AgentBindingOpenHABImpl> {

    public AgentBindingOpenHABLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(AgentBindingOpenHABLauncher.class, AgentBindingOpenHABImpl.class);
    }
    
    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPAgentRegistryScope.class);
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(args, AgentBindingOpenHABLauncher.class, AgentBindingOpenHABLauncher.class);
    }
}
