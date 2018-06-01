package org.openbase.bco.app.cloud.connector.google;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum ServiceTypeTraitMapping {

    BRIGHTNESS_STATE_SERVICE_TRAIT_MAPPING(ServiceType.BRIGHTNESS_STATE_SERVICE, Trait.BRIGHTNESS),
    POWER_STATE_SERVICE_TRAIT_MAPPING(ServiceType.POWER_STATE_SERVICE, Trait.ON_OFF),
    COLOR_STATE_SERVICE_TRAIT_MAPPING(ServiceType.COLOR_STATE_SERVICE, Trait.COLOR_SPECTRUM, Trait.COLOR_TEMPERATURE);

    public static final String POSTFIX = "_TRAIT_MAPPING";

    private final Set<Trait> traitSet;
    private final ServiceType serviceType;

    ServiceTypeTraitMapping(final ServiceType serviceType, final Trait... traits) {
        this.serviceType = serviceType;
        this.traitSet = new HashSet<>();
        Collections.addAll(this.traitSet, traits);
    }

    public ServiceType getServiceType() {
        return this.serviceType;
    }

    public Set<Trait> getTraitSet() {
        return Collections.unmodifiableSet(traitSet);
    }


    public static ServiceTypeTraitMapping getByServiceType(final ServiceType serviceType) throws NotAvailableException {
        try {
            return ServiceTypeTraitMapping.valueOf(serviceType.name() + POSTFIX);
        } catch (IllegalArgumentException ex) {
            throw new NotAvailableException("ServiceTypeTraitMapping for serviceType[" + serviceType.name() + "]");
        }
    }

}
