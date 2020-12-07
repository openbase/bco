package org.openbase.bco.api.graphql.subscriptions;

import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayList;
import java.util.List;

public class UnitDataPublisher implements Publisher<String> {

    private final List<Subscriber<? super String>> l = new ArrayList<>();

    public UnitDataPublisher(String alias) {
        ColorableLightRemote unit = null;
        try {
            unit = Units.getUnit(Registries.getUnitRegistry().getUnitConfigByAlias(alias).getId(), false, Units.COLORABLE_LIGHT);
        } catch (NotAvailableException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        unit.addDataObserver((source, data) -> {
            for (Subscriber<? super String> stringSubscriber : l) {
                stringSubscriber.onNext(data.getPowerState().getValue().name());
            }
        });
    }

    @Override
    public void subscribe(Subscriber<? super String> s) {
        l.add(s);
    }
}
