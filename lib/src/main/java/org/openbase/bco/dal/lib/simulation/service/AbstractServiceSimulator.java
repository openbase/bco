package org.openbase.bco.dal.lib.simulation.service;

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
import java.util.Collection;
import java.util.concurrent.Future;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractServiceSimulator {

    protected final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * Method generates a simulator description.
     *
     * @return the description as string.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Method should submit all simulation tasks to the global executor service and should returns the task future list.
     *
     * @return a future list of the executed tasks.
     */
    public abstract Collection<Future> executeSimulationTasks();
}
