package org.openbase.bco.api.graphql.subscriptions;

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
