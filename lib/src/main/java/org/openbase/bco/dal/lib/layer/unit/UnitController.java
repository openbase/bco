package org.openbase.bco.dal.lib.layer.unit;

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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.Method;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.MessageController;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * 
 * @param <D> the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 */
public interface UnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends Unit<D>, MessageController<D, DB> {

    /**
     * Method initialize this controller with the given unit configuration.
     * @param config the unit configuration
     * @throws InitializationException is throw if any error occurs during the initialization phase.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    public void init(final UnitConfig config) throws InitializationException, InterruptedException;
    
    /**
     * Returns the service state update method for the given service type.
     *
     * @param serviceType the type of service to update
     * @param serviceArgumentClass the class of the service state.
     * @return the update method.
     * @throws CouldNotPerformException is thrown in case the update method could not be detected.
     */
    public Method getUpdateMethod(final ServiceType serviceType, final Class serviceArgumentClass) throws CouldNotPerformException;

    /**
     * Applies the given service state update for this unit.
     *
     * @param serviceType
     * @param serviceArgument
     * @throws CouldNotPerformException
     */
    public void applyDataUpdate(final ServiceType serviceType, final Object serviceArgument) throws CouldNotPerformException;
}
