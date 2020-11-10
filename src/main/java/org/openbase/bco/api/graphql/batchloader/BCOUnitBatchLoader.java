package org.openbase.bco.api.graphql.batchloader;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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

import org.dataloader.BatchLoader;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class BCOUnitBatchLoader implements BatchLoader<String, UnitConfig> {

    private final UnitRegistry unitRegistry;

    public BCOUnitBatchLoader(UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
    }

    @Override
    public CompletionStage<List<UnitConfig>> load(List<String> ids) {

        final List<UnitConfig> unitConfigList = new ArrayList<>();

        for (String id : ids) {
            try {
                unitConfigList.add(unitRegistry.getUnitConfigById(id));
            } catch (NotAvailableException e) {
                e.printStackTrace();
            }
        }

        return CompletableFuture.completedFuture(unitConfigList);
    }
}
