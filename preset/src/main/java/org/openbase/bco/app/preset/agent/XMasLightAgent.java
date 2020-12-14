package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote;
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger;
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger.TriggerOperation;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class XMasLightAgent extends AbstractTriggerableAgent {

    public final static String XMAS_SCENE = "XMasLightScene";

    public static final double MIN_ILLUMINANCE_UNTIL_TRIGGER = 100d;

    private LocationRemote locationRemote;
    private SceneRemote xMasScene;

    public XMasLightAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);

            // activation trigger
            registerActivationTrigger(new GenericBoundedDoubleValueTrigger<>(locationRemote, MIN_ILLUMINANCE_UNTIL_TRIGGER, TriggerOperation.LOW_ACTIVE, ServiceType.ILLUMINANCE_STATE_SERVICE, "getIlluminance"), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            final UnitConfig unitConfig = super.applyConfigUpdate(config);

            // create xmas scene if not available otherwise load config of existing one
            UnitConfig xMasLightSceneConfig = null;
            try {
                xMasLightSceneConfig = Registries.getUnitRegistry().getUnitConfigByAliasAndUnitType(XMAS_SCENE, UnitType.SCENE);
            } catch (final NotAvailableException ex) {
                final Builder xMasSceneBuilder = UnitConfig.newBuilder();
                xMasSceneBuilder.setUnitType(UnitType.SCENE);
                xMasSceneBuilder.addAlias(XMAS_SCENE);
                LabelProcessor.addLabel(xMasSceneBuilder.getLabelBuilder(), Locale.ENGLISH, "XMas Lights");
                LabelProcessor.addLabel(xMasSceneBuilder.getLabelBuilder(), Locale.GERMAN, "Weihnachtsbeleuchtung");

                try {
                    xMasLightSceneConfig = Registries.getUnitRegistry().registerUnitConfig(xMasSceneBuilder.build()).get(5, TimeUnit.SECONDS);
                } catch (ExecutionException | TimeoutException exx) {
                    ExceptionPrinter.printHistory("Could not register XMas Light Group", ex, logger);
                }
            }

            // load xmas scene
            xMasScene = Units.getUnit(xMasLightSceneConfig, false, Units.SCENE);
            return unitConfig;
        }
    }

    @Override
    protected void trigger(final ActivationState activationState) throws
            CouldNotPerformException, ExecutionException, InterruptedException, TimeoutException {

        // activate xmas scene
        switch (activationState.getValue()) {
            case ACTIVE:
                observe(xMasScene.setActivationState(State.ACTIVE, getDefaultActionParameter(Timeout.INFINITY_TIMEOUT)));
                break;
            case INACTIVE:
                cancelAllObservedActions();
                break;
        }
    }
}
