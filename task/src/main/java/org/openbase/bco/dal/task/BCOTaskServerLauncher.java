
package org.openbase.bco.dal.task;

import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import org.openbase.jul.exception.CouldNotPerformException;

/*-
 * #%L
 * BCO DAL Task
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

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTaskServerLauncher extends AbstractLauncher<BCOTaskServerController> {

    public BCOTaskServerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(BCOTaskServer.class, BCOTaskServerController.class);
    }

    @Override
    protected void loadProperties() {

    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(args, BCOTaskServer.class, BCOTaskServerLauncher.class);
    }
}
