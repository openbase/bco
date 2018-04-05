package org.openbase.bco.registry.lib.launch;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.pattern.launch.AbstractLauncher;

/**
 * @param <L>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractRegistryLauncher<L extends AbstractRegistryController> extends AbstractLauncher<L> {

    public AbstractRegistryLauncher(Class applicationClass, Class<L> launchableClass) throws InstantiationException {
        super(applicationClass, launchableClass);
    }

    @Override
    public void verify() throws VerificationFailedException {
        if (!getLaunchable().isConsistent()) {
            throw new VerificationFailedException(getLaunchable().getClass().getSimpleName() + " is inconsistent");
        }
    }
}
