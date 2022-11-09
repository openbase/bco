package org.openbase.bco.dal.remote.trigger

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.pattern.trigger.Trigger
import org.openbase.jul.pattern.trigger.TriggerPriority

object StateObservationService {

    private val observerMap = mutableMapOf<String, InternalObserver<*>>()
    private val lock = Any()

    fun <DT : Message> registerTrigger(
        dataObserver: Observer<DataProvider<DT>, DT>,
        unitRemote: UnitRemote<DT>,
        trigger: Trigger,
    ) {
        synchronized(lock) {
            observerMap
                .getOrPut(unitRemote.id) { InternalObserver<DT>().also { unitRemote.addDataObserver(it) } }
                .let { it as InternalObserver<DT> }
                .also { it.registerTrigger(trigger, dataObserver) }
        }
    }

    fun <DT : Message> removeTrigger(
        dataObserver: Observer<DataProvider<DT>, DT>,
        unitRemote: UnitRemote<DT>,
        trigger: Trigger,
    ) {
        synchronized(lock) {
            observerMap[unitRemote.id]
                .let { it as InternalObserver<DT> }
                .also { it.removeTrigger(trigger, dataObserver) }
                .takeIf { it.noSubscription }
                ?.also { unitRemote.removeDataObserver(it) }
                ?.also { observerMap.remove(unitRemote.id) }
        }
    }

    private class InternalObserver<DT> : Observer<DataProvider<DT>, DT> {
        private var highPrioDataObserver = listOf<Observer<DataProvider<DT>, DT>>()
        private var lowPrioDataObserver = listOf<Observer<DataProvider<DT>, DT>>()

        override fun update(source: DataProvider<DT>, data: DT) {
            highPrioDataObserver.forEach { it.update(source, data) }
            lowPrioDataObserver.forEach { it.update(source, data) }
        }

        fun registerTrigger(trigger: Trigger, observer: Observer<DataProvider<DT>, DT>) {
            when (trigger.priority) {
                TriggerPriority.HIGH -> highPrioDataObserver = highPrioDataObserver.plus(observer)
                TriggerPriority.LOW -> lowPrioDataObserver = lowPrioDataObserver.plus(observer)
            }
        }

        fun removeTrigger(trigger: Trigger, observer: Observer<DataProvider<DT>, DT>) {
            when (trigger.priority) {
                TriggerPriority.HIGH -> highPrioDataObserver = highPrioDataObserver.minus(observer)
                TriggerPriority.LOW -> lowPrioDataObserver = lowPrioDataObserver.minus(observer)
            }
        }

        val noSubscription = highPrioDataObserver.isEmpty() && lowPrioDataObserver.isEmpty()
    }
}
