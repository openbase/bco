package org.openbase.bco.registry.lib.launch;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

import java.util.List;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.storage.registry.RemoteRegistry;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <L>
 */
 public abstract class AbstractRegistryLauncher<L extends AbstractRegistryController> extends AbstractLauncher<L> {

    public AbstractRegistryLauncher(Class applicationClass, Class<L> launchableClass) throws InstantiationException {
        super(applicationClass, launchableClass);
    }

    @Override
    public void verify() throws VerificationFailedException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        List<RemoteRegistry> remoteRegistries = getLaunchable().getRemoteRegistries();
        for (RemoteRegistry registry : remoteRegistries) {

            if (!registry.isConsistent()) {
                exceptionStack = MultiException.push(getLaunchable(), new VerificationFailedException(registry.getName() + " started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
            }
        }
        try {
            MultiException.checkAndThrow(JPService.getApplicationName() + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException(ex);
        }
    }
}
