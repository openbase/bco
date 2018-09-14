package org.openbase.bco.dal.lib.layer.unit.unitgroup;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.bco.dal.lib.layer.unit.MultiUnitServiceFusion;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitGroup extends BaseUnit<UnitGroupData>, MultiUnitServiceFusion {

    @Override
    default Set<ServiceType> getSupportedServiceTypes() throws NotAvailableException {
        final Set<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceDescription serviceDescription : getConfig().getUnitGroupConfig().getServiceDescriptionList()) {
                serviceTypeSet.add(serviceDescription.getServiceType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }
}
