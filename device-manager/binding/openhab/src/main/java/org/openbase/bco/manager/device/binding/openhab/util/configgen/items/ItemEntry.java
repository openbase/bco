package org.openbase.bco.manager.device.binding.openhab.util.configgen.items;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
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

import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ItemEntry {
    
    public String buildStringRep() throws CouldNotPerformException;

    public String getCommandType();

    public String getItemId();

    public String getLabel();

    public String getIcon();

    public List<String> getGroups();

    public String getItemHardwareConfig();

    public String getCommandTypeStringRep();

    public String getItemIdStringRep();

    public String getLabelStringRep();

    public String getIconStringRep();

    public String getGroupsStringRep();

    public String getBindingConfigStringRep();
}
