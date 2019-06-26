package com.github.corneil.kfsm

import com.github.corneil.kfsm.LockEvents.LOCK
import com.github.corneil.kfsm.LockEvents.UNLOCK
import com.github.corneil.kfsm.LockStates.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KfsmTests {

    @Test
    fun `test creation of fsm`() {
        // given
        val definition = StateMachine<LockStates, LockEvents, Lock>()
        definition.deriveInitialState = { context ->
            when (context.locked) {
                0 -> UNLOCKED
                1 -> LOCKED
                2 -> DOUBLE_LOCKED
                else -> error("Invalid state locked=${context.locked}")
            }
        }
        definition.transition(UNLOCK, LOCKED, UNLOCKED) { context ->
            context.unlock()
        }
        definition.transition(UNLOCK, DOUBLE_LOCKED, LOCKED) { context ->
            context.doubleUnlock()
        }
        definition.transition(LOCK, UNLOCKED, LOCKED) { context ->
            context.lock()
        }
        definition.transition(LOCK, LOCKED, DOUBLE_LOCKED) { context ->
            context.doubleLock()
        }

        val lock = Lock()
        // when
        val fsm = definition.instance(lock, LOCKED)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked == 1 }
        // when
        fsm.event(UNLOCK)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { lock.locked == 0 }
        try {
            // when
            fsm.event(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Statemachine doesn't provide for UNLOCK in UNLOCKED", x.message)
        }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked == 1 }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == DOUBLE_LOCKED }
        assertTrue { lock.locked == 2 }
        try {
            // when
            fsm.event(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Statemachine doesn't provide for LOCK in DOUBLE_LOCKED", x.message)
        }
    }

    @Test
    fun `test dsl creation of fsm`() {
        // given
        val definition = StateMachine<LockStates, LockEvents, Lock>().dsl {
            initial { context ->
                when (context.locked) {
                    0 -> UNLOCKED
                    1 -> LOCKED
                    2 -> DOUBLE_LOCKED
                    else -> error("Invalid state locked=${context.locked}")
                }
            }
            event(UNLOCK) {
                state(LOCKED to UNLOCKED) { context ->
                    context.unlock()
                }
                state(DOUBLE_LOCKED to LOCKED) { context ->
                    context.doubleUnlock()
                }
                state(UNLOCKED) {
                    error("Already Unlocked")
                }
            }
            event(LOCK) {
                state(UNLOCKED to LOCKED) { context ->
                    context.lock()
                }
                state(LOCKED to DOUBLE_LOCKED) { context ->
                    context.doubleLock()
                }
                state(DOUBLE_LOCKED) {
                    error("Already Double Locked")
                }
            }
        }.build()

        val lock = Lock()
        // when
        val fsm = definition.instance(lock)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked == 1 }
        // when
        fsm.event(UNLOCK)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { lock.locked == 0 }

        try {
            // when
            fsm.event(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already Unlocked", x.message)
        }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked == 1 }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == DOUBLE_LOCKED }
        assertTrue { lock.locked == 2 }
        try {
            // when
            fsm.event(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already Double Locked", x.message)
        }
    }

    @Test
    fun `Uncle Bob's Turnstile`() {
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().dsl {
            initial { ts ->
                when (ts.locked) {
                    true -> TurnstileStates.LOCKED
                    false -> TurnstileStates.UNLOCKED
                }
            }
            event(TurnstileEvents.COIN) {
                state(TurnstileStates.LOCKED to TurnstileStates.UNLOCKED) { ts ->
                    ts.unlock()
                }
                state(TurnstileStates.UNLOCKED to TurnstileStates.UNLOCKED) { ts ->
                    ts.thankYou()
                }
            }
            event(TurnstileEvents.PASS) {
                state(TurnstileStates.UNLOCKED to TurnstileStates.LOCKED) { ts ->
                    ts.lock();
                }
                state(TurnstileStates.LOCKED to TurnstileStates.LOCKED) { ts ->
                    ts.alarm()
                }
            }
        }.build()
        val turnstile = mockk<Turnstile>()
        val fsm = definition.instance(turnstile, TurnstileStates.LOCKED)

        assertTrue { fsm.currentState == TurnstileStates.LOCKED }

        every { turnstile.unlock() } returns Unit
        fsm.event(TurnstileEvents.COIN)
        verify { turnstile.unlock() }
        assertTrue { fsm.currentState == TurnstileStates.UNLOCKED }

        every { turnstile.thankYou() } returns Unit
        fsm.event(TurnstileEvents.COIN)
        verify { turnstile.thankYou() }
        assertTrue { fsm.currentState == TurnstileStates.UNLOCKED }

        every { turnstile.lock() } returns Unit
        fsm.event(TurnstileEvents.PASS)
        verify { turnstile.lock() }
        assertTrue { fsm.currentState == TurnstileStates.LOCKED }

        every { turnstile.alarm() } returns Unit
        fsm.event(TurnstileEvents.PASS)
        verify { turnstile.alarm() }
        assertTrue { fsm.currentState == TurnstileStates.LOCKED }
    }
}
