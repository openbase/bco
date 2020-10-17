package org.openbase.bco.api.graphql.batchloader;

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