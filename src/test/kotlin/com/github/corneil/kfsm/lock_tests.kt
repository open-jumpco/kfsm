package com.github.corneil.kfsm

import com.github.corneil.kfsm.LockEvents.LOCK
import com.github.corneil.kfsm.LockEvents.UNLOCK
import com.github.corneil.kfsm.LockStates.*
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class LockFsmTests {

    private fun verifyLockFSM(fsm: StateMachine.StateMachineInstance<LockStates, LockEvents, Lock>, lock: Lock) {
        // when
        every { lock.locked } returns 1
        every { lock.unlock() } just Runs
        every { lock.lock() } just Runs
        every { lock.doubleLock() } just Runs
        // then
        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.event(UNLOCK)
        // then
        verify { lock.unlock() }
        assertTrue { fsm.currentState == UNLOCKED }
        try {
            // when
            fsm.event(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already unlocked", x.message)
        }
        // when
        fsm.event(LOCK)
        // then
        verify { lock.lock() }
        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.event(LOCK)
        verify { lock.doubleLock() }
        // then
        assertTrue { fsm.currentState == DOUBLE_LOCKED }
        try {
            // when
            fsm.event(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already double locked", x.message)
        }
    }

    @Test
    fun `test plain creation of fsm`() {
        // given
        val definition = StateMachine<LockStates, LockEvents, Lock>()
        definition.initial { context ->
            when (context.locked) {
                0 -> UNLOCKED
                1 -> LOCKED
                2 -> DOUBLE_LOCKED
                else -> error("Invalid state locked=${context.locked}")
            }
        }
        definition.transition(LOCKED, UNLOCK, UNLOCKED) { context ->
            context.unlock()
        }
        definition.transition(LOCKED, LOCK, DOUBLE_LOCKED) { context ->
            context.doubleLock()
        }
        definition.transition(DOUBLE_LOCKED, UNLOCK, LOCKED) { context ->
            context.doubleUnlock()
        }
        definition.transition(DOUBLE_LOCKED, LOCK) {
            error("Already double locked")
        }
        definition.transition(UNLOCKED, LOCK, LOCKED) { context ->
            context.lock()
        }
        definition.transition(UNLOCKED, UNLOCK) {
            error("Already unlocked")
        }
        // when
        val lock = mockk<Lock>()
        every { lock.locked } returns 1
        val fsm = definition.create(lock)
        // then
        verifyLockFSM(fsm, lock)
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

            state(LOCKED) {
                event(LOCK to DOUBLE_LOCKED) { context ->
                    context.doubleLock()
                }
                event(UNLOCK to UNLOCKED) { context ->
                    context.unlock()
                }
            }
            state(DOUBLE_LOCKED) {
                event(UNLOCK to LOCKED) { context ->
                    context.doubleUnlock()
                }
                event(LOCK) {
                    error("Already double locked")
                }
            }
            state(UNLOCKED) {
                event(LOCK to LOCKED) { context ->
                    context.lock()
                }
                event(UNLOCK) {
                    error("Already unlocked")
                }
            }
        }.build()
        // when
        val lock = mockk<Lock>()
        every { lock.locked } returns 1
        val fsm = definition.create(lock)
        // then
        verifyLockFSM(fsm, lock)
    }
}
