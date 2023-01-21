package org.openbase.bco.authentication.lib.future

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.openbase.jul.iface.Shutdownable
import org.openbase.jul.schedule.GlobalScheduledExecutorService
import org.openbase.jul.schedule.SyncObject
import java.util.concurrent.CancellationException
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

    fun <F : AbstractAuthenticationFuture<*, *>> takeIfDone(future: F): F? = try {
        future.apply { get(FUTURE_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS) }
    } catch (e: TimeoutException) {
        null
    } catch (e: ExecutionException) {
        future
    } catch (e: CancellationException) {
        future
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

                    runBlocking {
                        authenticatedFutures
                            .filter { it.isCancelled }
                            .map { async { takeIfDone(it) } }
                            .awaitAll()
                            .filterNotNull()
                            .let { authenticatedFutures.removeAll(it) }
                    }
                }
            }, INITIAL_DELAY_IN_S, SCHEDULE_RATE_IN_S, TimeUnit.SECONDS
        )
        Shutdownable.registerShutdownHook { responseVerificationFuture.cancel(true) }
    }

    fun addFuture(authenticationFuture: AbstractAuthenticationFuture<*, *>) {
        synchronized(incomingFuturesLock) { incomingFutures.add(authenticationFuture) }
    }
}
