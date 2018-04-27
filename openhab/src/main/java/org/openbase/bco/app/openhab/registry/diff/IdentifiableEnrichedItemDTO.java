package org.openbase.bco.app.openhab.registry.diff;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;

public class IdentifiableEnrichedItemDTO implements AbstractIdentifiableDTO<EnrichedItemDTO> {

    private final EnrichedItemDTO enrichedItemDTO;

    public IdentifiableEnrichedItemDTO(final EnrichedItemDTO enrichedThingDTO) {
        this.enrichedItemDTO = enrichedThingDTO;
    }

    @Override
    public String getId() {
        return enrichedItemDTO.name;
    }

    @Override
    public EnrichedItemDTO getDTO() {
        return enrichedItemDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof EnrichedItemDTO)) {
            return false;
        }

        IdentifiableEnrichedItemDTO compared = (IdentifiableEnrichedItemDTO) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.getDTO().name, compared.getDTO().name);
        equalsBuilder.append(this.getDTO().label, compared.getDTO().label);
        equalsBuilder.append(this.getDTO().category, compared.getDTO().category);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(this.getDTO().name);
        hashCodeBuilder.append(this.getDTO().label);
        hashCodeBuilder.append(this.getDTO().category);
        return hashCodeBuilder.toHashCode();
    }
}
