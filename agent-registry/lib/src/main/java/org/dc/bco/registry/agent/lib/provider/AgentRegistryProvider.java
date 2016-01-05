package org.dc.bco.registry.agent.lib.provider;

import org.dc.bco.registry.agent.lib.AgentRegistry;
import org.dc.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed agent registry instance.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface AgentRegistryProvider {

    /**
     * Returns the globally managed agent registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public AgentRegistry getAgentRegistry() throws NotAvailableException;
}
