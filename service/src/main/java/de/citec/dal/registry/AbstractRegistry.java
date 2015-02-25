/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.schedule.SyncObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <KEY>
 * @param <VALUE>
 */
public abstract class AbstractRegistry<KEY, VALUE extends Identifiable<KEY>> {

	private final SyncObject SYNC = new SyncObject(AbstractRegistry.class);
	private final Map<KEY, VALUE> registry;

	public AbstractRegistry() {
		this.registry = new HashMap<>();
	}

	public void register(final VALUE entry) throws CouldNotPerformException {
		synchronized (SYNC) {
			if (registry.containsKey(entry.getId())) {
				throw new CouldNotPerformException("Could not register " + entry + "! Entry with same id already registered!");
			}
			registry.put(entry.getId(), entry);
		}
	}

	public VALUE remove(final VALUE entry) throws CouldNotPerformException {
		synchronized (SYNC) {
			if (!registry.containsKey(entry.getId())) {
				throw new CouldNotPerformException("Could not remove " + entry + "! Entry not registered!");
			}
			return registry.remove(entry.getId());
		}
	}

	public VALUE get(final KEY key) throws NotAvailableException {
		synchronized (SYNC) {
			if (!registry.containsKey(key)) {
                TreeMap<KEY, VALUE> sortedMap = new TreeMap<>(registry);
				throw new NotAvailableException("Entry[" + key + "]", "Nearest neighbor is [" + sortedMap.floorKey(key) + "] or [" + sortedMap.ceilingKey(key) + "].");
			}
			return registry.get(key);
		}
	}

	public List<VALUE> getEntries() {
		synchronized (SYNC) {
			return new ArrayList<>(registry.values());
		}
	}
    
    public boolean contrains(KEY key) {
        return registry.containsKey(key);
    }

	public void clean() {
		synchronized (SYNC) {
			registry.clear();
		}
	}
}
