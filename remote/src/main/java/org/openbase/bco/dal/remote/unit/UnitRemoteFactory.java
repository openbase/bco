package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.TimeUnit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.AbstractIdentifiableRemote;
import org.openbase.jul.pattern.Factory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitRemoteFactory extends Factory<UnitRemote, UnitConfig> {

    /**
     * Creates and initializes an unit remote out of the given unit configuration.
     *
     * @param config the unit configuration which defines the remote type and is used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @deprecated use newInitializedInstance instead!
     */
    @Deprecated
    public AbstractIdentifiableRemote createAndInitUnitRemote(final UnitConfig config) throws CouldNotPerformException;

    /**
     * Creates an unit remote out of the given unit configuration.
     *
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @deprecated use newInstance instead!
     */
    @Deprecated
    public AbstractIdentifiableRemote createUnitRemote(final UnitConfig config) throws CouldNotPerformException;

    /**
     * Creates and initializes an unit remote out of the given unit configuration.
     *
     * @param config the unit configuration which defines the remote type and is used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @throws InterruptedException
     */
    public UnitRemote newInitializedInstance(final UnitConfig config) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates an unit remote out of the given unit configuration.
     *
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws InstantiationException
     */
    @Override
    public UnitRemote newInstance(final UnitConfig config) throws InstantiationException;

    /**
     * Creates an unit remote out of the given unit class.
     * @param <R> the unit remote class type.
     * @param unitRemoteClass the unit class which defines the remote type.
     * @return the new created unit remote.
     * @throws InstantiationException
     */
    public <R extends AbstractUnitRemote> R newInstance(final Class<R> unitRemoteClass) throws InstantiationException;

    /**
     * Creates an unit remote out of the given unit id.
     *
     * @param unitId the unit id which defines the remote type.
     * @param timeout the timeout for the unit registry lookup. 
     * @param timeUnit the time unit of the timeout.
     * @return the new created unit remote.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public UnitRemote newInstance(String unitId, long timeout, final TimeUnit timeUnit) throws InstantiationException, CouldNotPerformException, InterruptedException;

    /**
     * Creates an unit remote out of the given unit scope.
     *
     * @param scope the unit scope which defines the remote type..
     * @param timeout the timeout for the unit registry lookup. 
     * @param timeUnit the time unit of the timeout.
     * @return the new created unit remote.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public UnitRemote newInstance(ScopeType.Scope scope, long timeout, final TimeUnit timeUnit) throws InstantiationException, CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes an unit remote out of the given unit scope and type declaration.
     *
     * @param scope the unit scope which is used for the remote initialization.
     * @param type the unit to instantiate.
     * @return the new created unit remote.
     * @throws InitializationException is thrown if the unit could not be initialized with the given information.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public UnitRemote newInitializedInstance(final Scope scope, final UnitType type) throws InitializationException, InstantiationException, CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes an unit remote out of the given unit scope and class type.
     *
     * @param <R> the unit remote class type.
     * @param scope the unit scope which is used for the remote initialization.
     * @param unitRemoteClass to identify the unit type.
     * @return the new created and initialized unit remote.
     * @throws InitializationException is thrown if the unit could not be initialized with the given information.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @throws InterruptedException is thrown if the thread was externally interrupted. is thrown if the thread was externally interrupted.
     */
    public <R extends AbstractUnitRemote> R newInitializedInstance(final Scope scope, final Class<R> unitRemoteClass) throws InitializationException, InstantiationException, CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes an unit remote out of the given unit scope.
     *
     * @param scope the unit scope which is used for the remote initialization.
      * @param timeout the timeout for the unit registry lookup. 
     * @param timeUnit the time unit of the timeout.
     * @return the new created and initialized unit remote.
     * @throws InitializationException is thrown if the unit could not be initialized with the given information.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @throws InterruptedException is thrown if the thread was externally interrupted. is thrown if the thread was externally interrupted.
     */
    public UnitRemote newInitializedInstance(final Scope scope, long timeout, final TimeUnit timeUnit) throws InitializationException, InstantiationException, CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes an unit remote out of the given unit id.
     *
     * @param unitId the unit id which is used for the remote initialization.
     * @param timeout the timeout for the unit registry lookup. 
     * @param timeUnit the time unit of the timeout.
     * @return the new created and initialized unit remote.
     * @throws InitializationException is thrown if the unit could not be initialized with the given information.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     * @throws CouldNotPerformException is thrown if any other error occurs during buildup.
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     */
    public UnitRemote newInitializedInstance(final String unitId, long timeout, final TimeUnit timeUnit) throws InitializationException, InstantiationException, CouldNotPerformException, InterruptedException;

    /**
     * Creates an unit remote out of the given unit configuration.
     *
     * @param type the unit type which is used for the remote initialization.
     * @return the new created and initialized unit remote.
     * @throws InstantiationException is thrown if the unit could not be instantiated with the given information.
     */
    public UnitRemote newInstance(final UnitType type) throws InstantiationException;

}
