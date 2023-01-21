package org.openbase.bco.authentication.lib.future

import org.openbase.jul.iface.Shutdownable
import org.openbase.jul.schedule.GlobalScheduledExecutorService
import org.openbase.jul.schedule.SyncObject
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object AuthenticationFutureList {

    private const val SCHEDULE_RATE_IN_S = 5L
    private const val INITIAL_DELAY_IN_S = 1L
    private const val FUTURE_TIMEOUT_IN_MS = 1L

    private val authenticatedFuturesLock = SyncObject("AuthenticatedFutureListSync")
    private val incomingFuturesLock = SyncObject("IncomingFuturesLock")
    private val incomingFutures: MutableList<AbstractAuthenticationFuture<*, *>> = ArrayList()
    private val authenticatedFutures: MutableList<AbstractAuthenticationFuture<*, *>> = ArrayList()

    fun isDone(future: AbstractAuthenticationFuture<*, *>): Boolean = try {
        future
            .get(FUTURE_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
            .let { true }
    } catch (e: TimeoutException) {
        false
    } catch (e: ExecutionException) {
        true
    }

    init {
        // create a task which makes sure that get is called on all of these futures so that tickets are renewed
        val responseVerificationFuture = GlobalScheduledExecutorService.scheduleAtFixedRate(
            {
                synchronized(authenticatedFuturesLock) {

                    // handle new incoming futures moving it to the main list
                    synchronized(incomingFuturesLock) {
                        authenticatedFutures.addAll(incomingFutures)
                        incomingFutures.clear()
                    }

                    // Note: the abstract authentication future will remove itself from this list if get finished successfully.
                    authenticatedFutures
                        .toList() // ATTENTION: this is important because the futures may remove themselves from the list on get calls
                        .filter { it.isCancelled || isDone(it) }
                        .toList()
                        .let { authenticatedFutures.removeAll(it) }

                }
            }, INITIAL_DELAY_IN_S, SCHEDULE_RATE_IN_S, TimeUnit.SECONDS
        )
        Shutdownable.registerShutdownHook { responseVerificationFuture.cancel(true) }
    }

    fun addFuture(authenticationFuture: AbstractAuthenticationFuture<*, *>) {
        synchronized(incomingFuturesLock) { incomingFutures.add(authenticationFuture) }
    }
}
