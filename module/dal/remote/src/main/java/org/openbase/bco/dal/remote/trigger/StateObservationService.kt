package org.openbase.bco.dal.remote.trigger

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.pattern.trigger.TriggerPriority

object StateObservationService {

    private val observerMap = mutableMapOf<UnitRemote<*>, InternalObserver<*>>()

    fun <DT : Message> registerTrigger(dataObserver: Observer<DataProvider<DT>, DT>, unitRemote: UnitRemote<DT>, priority: TriggerPriority) {
        observerMap.getOrPut(unitRemote) {
            InternalObserver<DT>().also { unitRemote.addDataObserver(it) }
        }
            .let { it as InternalObserver<DT> }
            .let { internalObserver ->
                when(priority) {
                    TriggerPriority.HIGH -> internalObserver.highPrioDataObserver.add(dataObserver)
                    TriggerPriority.LOW -> internalObserver.lowPrioDataObserver.add(dataObserver)
                }
            }
    }

    class InternalObserver<DT>: Observer<DataProvider<DT>, DT> {
        val highPrioDataObserver = mutableListOf<Observer<DataProvider<DT>, DT>>()
        val lowPrioDataObserver=  mutableListOf<Observer<DataProvider<DT>, DT>>()

        override fun update(source: DataProvider<DT>, data: DT) {
            highPrioDataObserver.forEach { it.update(source, data) }
            lowPrioDataObserver.forEach { it.update(source, data) }
        }
    }
}
