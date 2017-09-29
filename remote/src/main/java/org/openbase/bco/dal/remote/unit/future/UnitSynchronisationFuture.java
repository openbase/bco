/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.remote.unit.future;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.schedule.AbstractSynchronizationFuture;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observer;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;

/**
 *
 * @author pleminoq
 */
public class UnitSynchronisationFuture extends AbstractSynchronizationFuture<ActionFuture> {

    private final UnitRemote unitRemote;

    public UnitSynchronisationFuture(final Future<ActionFuture> internalFuture, final UnitRemote unitRemote) {
        super(internalFuture, unitRemote);
        this.unitRemote = unitRemote;
    }

    @Override
    protected void addObserver(Observer observer) {
        unitRemote.addDataObserver(observer);
    }

    @Override
    protected void removeObserver(Observer observer) {
        unitRemote.removeDataObserver(observer);
    }

    @Override
    protected void beforeWaitForSynchronization() throws CouldNotPerformException {
        try {
            unitRemote.waitForData();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected boolean check(ActionFuture actionFuture) throws CouldNotPerformException {
        ActionDescription actionDescription = actionFuture.getActionDescription(0);
        return unitRemote.getTransactionIdByServiceType(actionDescription.getServiceStateDescription().getServiceType()) >= actionDescription.getTransactionId();
    }

}
