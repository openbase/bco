package org.openbase.bco.dal.test.layer.unit;

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.openbase.bco.dal.lib.layer.unit.UnitDataFilteredObservable;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.dal.DimmerDataType.DimmerData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class UnitDataFilteredObservableTest {

    @Test
    public void testRequestedTempusObservation() throws Exception {
        final UnitDataFilteredObservable<DimmerData> observable = createObservable(ServiceTempus.REQUESTED);

        final EventLogObserver observer = new EventLogObserver();
        observable.addObserver(observer);

        DimmerData.Builder builder = DimmerData.newBuilder();
        builder.getPowerStateRequestedBuilder().setValue(PowerStateType.PowerState.State.ON);
        observable.notifyObservers(builder.build());

        assertEquals(1, observer.getEvents().size());

        builder.getBrightnessStateBuilder().setBrightness(100);
        observable.notifyObservers(builder.build());

        assertEquals(1, observer.getEvents().size(), "Changes to the current brightness state triggered requested observable");

        builder.addActionBuilder().getDescriptionBuilder().addEntryBuilder().setKey("en").setValue("ActionDescription");
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the action list triggered requested state observable");

        builder.getPowerStateBuilder().setValue(PowerStateType.PowerState.State.OFF);
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the current power state triggered requested state observable");

        builder.addAlias("test");
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the aliases triggered requested state observable");

        builder.getPowerStateLastBuilder().setValue(PowerStateType.PowerState.State.ON);
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the last power state triggered requested state observable");

        builder.getBrightnessStateLastBuilder().setBrightness(50);
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the last brightness state triggered requested state observable");

        builder.setTransactionId(10);
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the transaction id  triggered requested state observable");

        builder.setId("testID");
        observable.notifyObservers(builder.build());
        assertEquals(1, observer.getEvents().size(), "Changes to the id triggered requested state observable");

        builder.getBrightnessStateRequestedBuilder().setBrightness(30);
        observable.notifyObservers(builder.build());
        assertEquals(2, observer.getEvents().size(), "Changes to the requested brightness state did not trigger the requested state observable");

        builder.getBrightnessStateRequestedBuilder().setBrightness(30);
        observable.notifyObservers(builder.build());
        assertEquals(2, observer.getEvents().size(), "The same requested brightness state is notified twice");

        PowerStateType.PowerState.Builder powerStateRequestedBuilder = builder.getPowerStateRequestedBuilder();
        powerStateRequestedBuilder.addLastValueOccurrenceBuilder().setKey(PowerStateType.PowerState.State.OFF).setValue(TimestampProcessor.getCurrentTimestamp());
        observable.notifyObservers(builder.build());
        assertEquals(3, observer.getEvents().size(), "Changes to the latest value occurrence did not trigger the requested state observable");

        powerStateRequestedBuilder.addAggregatedValueCoverageBuilder().setKey(PowerStateType.PowerState.State.ON).setCoverage(0.5);
        observable.notifyObservers(builder.build());
        assertEquals(3, observer.getEvents().size(), "Changes to the aggregated value coverage triggered the requested state observable");

        powerStateRequestedBuilder.setTimestamp(TimestampProcessor.getCurrentTimestamp());
        observable.notifyObservers(builder.build());
        assertEquals(4, observer.getEvents().size(), "Changes to the timestamp did not trigger the requested state observable");

        powerStateRequestedBuilder.getResponsibleActionBuilder().setActionId("actionId");
        observable.notifyObservers(builder.build());
        assertEquals(5, observer.getEvents().size(), "Changes to the responsible action did not trigger the requested state observable");
    }

    private UnitDataFilteredObservable<DimmerData> createObservable(final ServiceTempus serviceTempus) throws CouldNotPerformException {
        final UnitTemplate unitTemplate = MockRegistry.MockUnitTemplate.getUnitTemplate(UnitTemplate.UnitType.DIMMER);
        return new UnitDataFilteredObservable<>(new MockDimmerDataProvider(), serviceTempus, unitTemplate);
    }

    private static class EventLogObserver implements Observer<DataProvider<DimmerData>, DimmerData> {

        private final List<DimmerData> events = new ArrayList<>();

        @Override
        public void update(DataProvider<DimmerData> source, DimmerData data) throws Exception {
            events.add(data);
        }

        public List<DimmerData> getEvents() {
            return events;
        }
    }

    private static class MockDimmerDataProvider implements DataProvider<DimmerData> {

        @Override
        public boolean isDataAvailable() {
            return true;
        }

        @Override
        public void validateData() throws InvalidStateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<DimmerData> getDataClass() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DimmerData getData() throws NotAvailableException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<DimmerData> getDataFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addDataObserver(Observer<DataProvider<DimmerData>, DimmerData> observer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeDataObserver(Observer<DataProvider<DimmerData>, DimmerData> observer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void waitForData() throws CouldNotPerformException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }
}
