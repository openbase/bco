package org.openbase.bco.authentication.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.authentication.lib.future.AuthenticationFutureList
import org.openbase.jps.core.JPService
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.schedule.FutureProcessor
import java.util.concurrent.Future
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.cancellation.CancellationException

class AuthenticationFutureListTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            JPService.setupJUnitTestMode()
        }
    }

    @Timeout(3)
    @Test
    fun testTakeIfDone() {
        val completedFuture = FutureProcessor.completedFuture()
        AuthenticationFutureList.takeIfDone(completedFuture) shouldBe completedFuture

        val failedFuture = FutureProcessor.toCompletableFuture { throw CouldNotPerformException("Failed") }
        AuthenticationFutureList.takeIfDone(failedFuture) shouldBe failedFuture

        val canceledFuture = FutureProcessor.canceledFuture(CancellationException("Cancelled"))
        AuthenticationFutureList.takeIfDone(canceledFuture) shouldBe canceledFuture

        val timeout = 100L
        val runningFuture = FutureProcessor.toCompletableFuture { Thread.sleep(timeout) }
        AuthenticationFutureList.takeIfDone(runningFuture) shouldBe null
    }

    @Timeout(5)
    @Test
    fun testScheduledTask() {
        val lock = ReentrantLock()
        val condition: Condition = lock.newCondition()

        val futures = listOf<Future<*>>(
            FutureProcessor.completedFuture(),
            FutureProcessor.toCompletableFuture { throw CouldNotPerformException("Failed") },
            FutureProcessor.canceledFuture(CancellationException("Cancelled")),
            FutureProcessor.toCompletableFuture {
                lock.withLock {
                    condition.await()
                }
            }
        ).onEach { AuthenticationFutureList.addFuture(it) }

        // first, all futures should be on the incoming list
        AuthenticationFutureList.incomingSize shouldBe futures.size
        AuthenticationFutureList.size shouldBe 0

        // the incoming list should be cleared and all futures except the one still running should be removed
        AuthenticationFutureList.waitForIteration()
        AuthenticationFutureList.incomingSize shouldBe 0
        AuthenticationFutureList.size shouldBe 1

        // finish the running future
        lock.withLock {
            condition.signalAll()
        }

        // all tasks should be removed
        AuthenticationFutureList.waitForIteration()
        AuthenticationFutureList.size shouldBe 0
    }
}