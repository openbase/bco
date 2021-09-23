package org.openbase.bco.device.openhab.registry.diff;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openhab.core.io.rest.core.item.EnrichedItemDTO;

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
