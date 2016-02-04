package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.iface.Identifiable;
import org.dc.jul.iface.provider.ConfigProvider;
import org.dc.jul.iface.provider.LabelProvider;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author mpohling
 */
public interface Unit extends Service, LabelProvider, ScopeProvider, Identifiable<String>, ConfigProvider<UnitConfig> {

    public UnitType getType() throws NotAvailableException;

}
