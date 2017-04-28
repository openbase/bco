package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
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

import com.google.protobuf.GeneratedMessage;
import de.citec.csra.allocation.cli.AllocatableResource;
import java.util.UUID;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import rsb.RSBException;
import rsb.Scope;
import rst.communicationpatterns.ResourceAllocationType;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import static rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM;
import static rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Policy.PRESERVE;
import static rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Priority.LOW;
import static rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.State.REQUESTED;
import rst.timing.IntervalType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <D>
 * @param <DB>
 */
public class UnitResourceAllocator<D extends GeneratedMessage, DB extends D.Builder<DB>> {
    
    private UnitController<D, DB> unitController;

    public UnitResourceAllocator(UnitController<D, DB> unitController) {
        this.unitController = unitController;
    }
    
    public AllocatableResource allocate(final ActionDescription actionDescription) throws CouldNotPerformException {
        return allocate((actionDescription.);
    }
    
    public AllocatableResource allocate(final ResourceAllocation allocation) throws CouldNotPerformException {
        final AllocatableResource allocatableResource = new AllocatableResource(allocation);
        try {
            allocatableResource.startup();
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not start Allocation!", ex);
        }
        return allocatableResource;
    }
    
    public void deallocate() throws CouldNotPerformException {
        
    }

    private AllocatableResource allocateResource(Scope scope) throws CouldNotPerformException {
        final String id = UUID.randomUUID().toString();
        ResourceAllocationType.ResourceAllocation allocation = ResourceAllocationType.ResourceAllocation.newBuilder().
                setId(id).setState(REQUESTED).
                setDescription("Generated Allocation").
                setPolicy(PRESERVE).
                setPriority(LOW).
                setInitiator(SYSTEM).
                setSlot(IntervalType.Interval.newBuilder().build()).
                addResourceIds(ScopeGenerator.generateStringRep(scope)).
                build();

        final AllocatableResource allocatableResource = new AllocatableResource(allocation);
        try {
            allocatableResource.startup();
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not start Allocation!", ex);
        }
        return allocatableResource;
    }
}
