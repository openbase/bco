
package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.Observer;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.ActionFutureType;
import rst.domotic.service.ServiceTemplateType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ServiceProvider {
    
    @RPCMethod
    public Future<ActionFutureType.ActionFuture> applyAction(final ActionDescriptionType.ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException;
    
    public void addServiceStateObserver(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType, final Observer observer);

    public void removeServiceStateObserver(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType, final Observer observer);
}
