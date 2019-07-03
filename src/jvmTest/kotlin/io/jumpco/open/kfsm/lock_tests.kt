/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.jumpco.open.kfsm

import io.jumpco.open.kfsm.LockEvents.*
import io.jumpco.open.kfsm.LockStates.*
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
/**
 * @suppress
 */
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

    @Test
    fun `simple lock test`() {
        val lock = Lock(0)
        val fsm = LockFSM(lock)
        println("--lock1")
        fsm.lock()
        println("--lock2")
        fsm.lock()
        println("--lock3")
        fsm.lock()
        println("--unlock1")
        fsm.unlock()
        println("--unlock2")
        fsm.unlock()
        println("--unlock3")
        fsm.unlock()
    }
}
