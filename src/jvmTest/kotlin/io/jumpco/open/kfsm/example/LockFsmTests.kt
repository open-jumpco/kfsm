/*
    Copyright 2019-2021 Open JumpCO

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
    and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial
    portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
    CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.
 */
package io.jumpco.open.kfsm.example

import io.jumpco.open.kfsm.StateMachineBuilder
import io.jumpco.open.kfsm.StateMachineInstance
import io.jumpco.open.kfsm.example.LockEvents.LOCK
import io.jumpco.open.kfsm.example.LockEvents.UNLOCK
import io.jumpco.open.kfsm.example.LockStates.DOUBLE_LOCKED
import io.jumpco.open.kfsm.example.LockStates.LOCKED
import io.jumpco.open.kfsm.example.LockStates.UNLOCKED
import io.jumpco.open.kfsm.stateMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * @suppress
 */
class LockFsmTests {

    private fun verifyLockFSM(fsm: StateMachineInstance<LockStates, LockEvents, Lock, Any, Any>, lock: Lock) {

        // then
        assertTrue { lock.locked == 1 }
        // when
        fsm.sendEvent(UNLOCK)
        // then
        assertTrue { lock.locked == 0 }
        try {
            // when
            fsm.sendEvent(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already unlocked", x.message)
        }
        // when
        fsm.sendEvent(LOCK)
        // then
        assertTrue { lock.locked == 1 }
        // when
        fsm.sendEvent(LOCK)
        // then
        assertTrue { lock.locked == 2 }
        try {
            // when
            fsm.sendEvent(LOCK)
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
        val builder = StateMachineBuilder<LockStates, LockEvents, Lock, Any, Any>(
            LockStates.values().toSet(),
            LockEvents.values().toSet()
        )
        builder.initialState {
            when (locked) {
                0 -> UNLOCKED
                1 -> LOCKED
                2 -> DOUBLE_LOCKED
                else -> error("Invalid state locked=$locked")
            }
        }
        builder.transition(LOCKED, UNLOCK, UNLOCKED) {
            unlock()
        }
        builder.transition(LOCKED, LOCK, DOUBLE_LOCKED) {
            doubleLock()
        }
        builder.transition(DOUBLE_LOCKED, UNLOCK, LOCKED) {
            doubleUnlock()
        }
        builder.transition(DOUBLE_LOCKED, LOCK) {
            error("Already double locked")
        }
        builder.transition(UNLOCKED, LOCK, LOCKED) {
            lock()
        }
        builder.transition(UNLOCKED, UNLOCK) {
            error("Already unlocked")
        }
        val definition = builder.complete()
        // when
        val lock = Lock(1)
        val fsm = definition.create(lock)
        // then
        verifyLockFSM(fsm, lock)
    }

    @Test
    fun `test dsl creation of fsm`() {
        // given
        val definition = stateMachine(
            LockStates.values().toSet(),
            LockEvents.values().toSet(),
            Lock::class
        ) {
            initialState {
                when (locked) {
                    0 -> UNLOCKED
                    1 -> LOCKED
                    2 -> DOUBLE_LOCKED
                    else -> error("Invalid state locked=$locked")
                }
            }

            whenState(LOCKED) {
                onEvent(LOCK to DOUBLE_LOCKED) {
                    doubleLock()
                }
                onEvent(UNLOCK to UNLOCKED) {
                    unlock()
                }
            }
            whenState(DOUBLE_LOCKED) {
                onEvent(UNLOCK to LOCKED) {
                    doubleUnlock()
                }
                onEvent(LOCK) {
                    error("Already double locked")
                }
            }
            whenState(UNLOCKED) {
                onEvent(LOCK to LOCKED) {
                    lock()
                }
                onEvent(UNLOCK) {
                    error("Already unlocked")
                }
            }
        }.build()
        // when
        val lock = Lock(1)

        val fsm = definition.create(lock)
        // then
        verifyLockFSM(fsm, lock)
    }

    @Test
    fun `simple lock test`() {
        val lock = Lock(0)
        val fsm = LockFSM(lock)
        println("--lock1")
        fsm.lock()
        assertEquals(fsm.allowedEvents(), setOf(LOCK, UNLOCK))
        println("--lock2")
        fsm.lock()
        println("--lock3")
        fsm.lock()
        println("--unlock1")
        fsm.unlock()
        assertEquals(fsm.allowedEvents(), setOf(UNLOCK, LOCK))
        println("--unlock2")
        fsm.unlock()
        assertEquals(fsm.allowedEvents(), setOf(LOCK))
        println("--unlock3")
        fsm.unlock()
    }
}
