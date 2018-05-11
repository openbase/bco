package org.openbase.bco.registry.template.lib.provider;

import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed template registry instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface TemplateRegistryProvider {

    /**
     * Returns the globally managed template registry instance.
     * @return
     * @throws NotAvailableException
     */
    TemplateRegistry getTemplateRegistry() throws NotAvailableException;
}
