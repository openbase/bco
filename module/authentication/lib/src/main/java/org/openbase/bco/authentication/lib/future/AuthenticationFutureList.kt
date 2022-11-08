package org.openbase.bco.authentication.lib.future

import org.openbase.jul.iface.Shutdownable
import org.openbase.jul.schedule.GlobalScheduledExecutorService
import org.openbase.jul.schedule.SyncObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object AuthenticationFutureList {

    private const val SCHEDULE_RATE_IN_S = 5L
    private const val INITIAL_DELAY_IN_S = 1L
    private const val FUTURE_TIMEOUT_IN_MS = 1L

    private val listSync = SyncObject("AuthenticatedFutureListSync")
    private val authenticatedFutureList: MutableList<AbstractAuthenticationFuture<*, *>> = ArrayList()
    private val logger = LoggerFactory.getLogger("AuthenticationFuture")

    private fun isFailed(future: AbstractAuthenticationFuture<*, *>): Boolean {
        try {
            // Note: the abstract authentication future will remove itself from this list if
            //       get finished successfully.
            future.get(FUTURE_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
        } catch (ex: ExecutionException) {
            return true
        } catch (ex: TimeoutException) {
            return false
        }
        return false
    }

    init {
        synchronized(listSync) {
            // create a task which makes sure that get is called on all of these futures so that tickets are renewed
            val responseVerificationFuture = GlobalScheduledExecutorService.scheduleAtFixedRate(
                {
                    synchronized(listSync) {
                        authenticatedFutureList
                            .filter { it.isCancelled || isFailed(it) }
                            .let { authenticatedFutureList.removeAll(it) }

                    }
                }, INITIAL_DELAY_IN_S, SCHEDULE_RATE_IN_S, TimeUnit.SECONDS
            )
            Shutdownable.registerShutdownHook { responseVerificationFuture.cancel(true) }
        }
    }

    fun addFuture(authenticationFuture: AbstractAuthenticationFuture<*, *>) {
        synchronized(listSync) { authenticatedFutureList.add(authenticationFuture) }
    }

    fun removeFuture(authenticationFuture: AbstractAuthenticationFuture<*, *>) {
        synchronized(listSync) { authenticatedFutureList.remove(authenticationFuture) }
    }
}
