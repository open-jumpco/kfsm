package com.github.corneil.kfsm

import com.github.corneil.kfsm.LockEvents.LOCK
import com.github.corneil.kfsm.LockEvents.UNLOCK
import com.github.corneil.kfsm.LockStates.LOCKED
import com.github.corneil.kfsm.LockStates.UNLOCKED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KfsmTests {

    @Test
    fun `test creation of fsm`() {
        val definition = StateMachine<LockStates, LockEvents, Lock>() {
                if(it.locked) LOCKED else UNLOCKED
            }
            .on(UNLOCK, LOCKED, UNLOCKED) { context ->
                context.unlock()
            }
            .on(LOCK, UNLOCKED, LOCKED) { context ->
                context.lock()
            }

        val lock = Lock(true)

        val fsm = definition.instance(LOCKED, lock)
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked }
        fsm.event(UNLOCK)
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { !lock.locked }
        try {
            fsm.event(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            assertEquals("Statemachine doesn't provide for UNLOCK in UNLOCKED", x.message)
        }
        fsm.event(LOCK)
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked }
        try {
            fsm.event(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            assertEquals("Statemachine doesn't provide for LOCK in LOCKED", x.message)
        }
    }
}
