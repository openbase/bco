package org.openbase.bco.dal.visual.action;

/*-
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.jps.core.JPService;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.visual.javafx.launch.AbstractFXMLApplication;

public class BCOActionInspector extends AbstractFXMLApplication {

    public BCOActionInspector() {
        super(UnitAllocationPaneController.class);
    }

    @Override
    protected void registerProperties() {
        JPService.registerProperty(JPComPort.class);
        JPService.registerProperty(JPComHost.class);
        JPService.registerProperty(JPProviderControlMode.class);
    }

    public static void main(String[] args) {
        BCOActionInspector.launch(args);
    }
}
