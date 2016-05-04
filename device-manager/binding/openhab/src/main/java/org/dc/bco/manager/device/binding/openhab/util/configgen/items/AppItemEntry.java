package org.dc.bco.manager.device.binding.openhab.util.configgen.items;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.manager.device.binding.openhab.util.configgen.GroupEntry;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.app.AppConfigType.AppConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class AppItemEntry extends AbstractItemEntry {

    public static String APP_GROUP_LABEL = "Apps";

    public AppItemEntry(final AppConfig appConfig, final LocationRegistryRemote locationRegistryRemote) throws org.dc.jul.exception.InstantiationException {
        super();
        try {
            this.itemId = generateItemId(appConfig);
            this.icon = "";
            this.commandType = "Switch";
            this.label = appConfig.getLabel();
            this.itemHardwareConfig = "rsb=\"bco.manager.app:" + appConfig.getId() + "\"";
            groups.add(APP_GROUP_LABEL);
            groups.add(GroupEntry.generateGroupID(appConfig.getLocationId(), locationRegistryRemote));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateItemId(AppConfig appConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("App")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(appConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
