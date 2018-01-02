package org.openbase.bco.manager.app.binding.openhab;

/*
 * #%L
 * BCO Manager App Binding OpenHAB
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
import org.openbase.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class AppBindingOpenHABLauncher extends AbstractLauncher<AppBindingOpenHABImpl> {

    public AppBindingOpenHABLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(AppBindingOpenHABLauncher.class, AppBindingOpenHABImpl.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPAppRegistryScope.class);
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(args, AppBindingOpenHABLauncher.class, AppBindingOpenHABLauncher.class);
    }
}
