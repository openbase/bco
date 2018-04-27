package org.openbase.bco.app.openhab.registry.diff;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;

public class IdentifiableEnrichedThingDTO implements AbstractIdentifiableDTO<EnrichedThingDTO> {

    private final EnrichedThingDTO enrichedThingDTO;

    public IdentifiableEnrichedThingDTO(final EnrichedThingDTO enrichedThingDTO) {
        this.enrichedThingDTO = enrichedThingDTO;
    }

    @Override
    public String getId() {
        return enrichedThingDTO.UID;
    }

    @Override
    public EnrichedThingDTO getDTO() {
        return enrichedThingDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof IdentifiableEnrichedThingDTO)) {
            return false;
        }

        IdentifiableEnrichedThingDTO compared = (IdentifiableEnrichedThingDTO) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.getDTO().UID, compared.getDTO().UID);
        equalsBuilder.append(this.getDTO().thingTypeUID, compared.getDTO().thingTypeUID);
        equalsBuilder.append(this.getDTO().label, compared.getDTO().label);
        equalsBuilder.append(this.getDTO().location, compared.getDTO().location);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(this.getDTO().UID);
        hashCodeBuilder.append(this.getDTO().thingTypeUID);
        hashCodeBuilder.append(this.getDTO().label);
        hashCodeBuilder.append(this.getDTO().location);
        return hashCodeBuilder.toHashCode();
    }
}

