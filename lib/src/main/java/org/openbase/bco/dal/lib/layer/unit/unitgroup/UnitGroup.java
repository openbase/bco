package org.openbase.bco.dal.lib.layer.unit.unitgroup;

/*
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
import java.util.HashSet;
import java.util.Set;
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;
import org.openbase.bco.dal.lib.layer.unit.MultiUnitServiceFusion;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitGroup extends BaseUnit<UnitGroupData>, MultiUnitServiceFusion {

    @Override
    default public Set<ServiceTemplateType.ServiceTemplate.ServiceType> getSupportedServiceTypes() throws NotAvailableException, InterruptedException {
        final Set<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceTemplate serviceTemplate : getConfig().getUnitGroupConfig().getServiceTemplateList()) {
                serviceTypeSet.add(serviceTemplate.getType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }
}
