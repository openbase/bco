package org.openbase.bco.dal.lib.layer.service.provider;

/*
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
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotSupportedException;
import rst.domotic.action.ActionConfigType;
import rst.domotic.service.ServiceTemplateType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ProviderService extends Service {

    /**
     * The prefix of each update method.
     */
    public static final String UPDATE_METHOD_PREFIX = "update";

    /**
     * Method returns the update method name of the given service provider.
     *
     * This method should provide each unit controller which implements this provider service.
     *
     * @param serviceType the related service type for the update method.
     * @return the name of the update method.
     */
    public static String getUpdateMethodName(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        return UPDATE_METHOD_PREFIX + Service.getServiceBaseName(serviceType) + "Provider";
    }

    @Override
    public default Future<Void> applyAction(ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        throw new NotSupportedException("actions", ProviderService.class);
    }
}
