package org.openbase.bco.app.cloud.connector.google;

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
