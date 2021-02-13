/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @suppress
 */
// tag::context[]
data class ImmutableLock(val locked: Int = 1) {

    fun lock(): ImmutableLock {
        require(locked == 0)
        println("Lock")
        return copy(locked + 1)
    }

    fun doubleLock(): ImmutableLock {
        require(locked == 1)
        println("DoubleLock")
        return copy(locked + 1)
    }

    fun unlock(): ImmutableLock {
        require(locked == 1)
        println("Unlock")
        return copy(locked - 1)
    }

    fun doubleUnlock(): ImmutableLock {
        require(locked == 2)
        println("DoubleUnlock")
        return copy(locked - 1)
    }

    override fun toString(): String {
        return "Lock(locked=$locked)"
    }
}
// end::context[]

// tag::definition[]
class ImmutableLockFSM() {

    companion object {
        fun handleEvent(context: ImmutableLock, event: LockEvents): ImmutableLock {
            val fsm = definition.create(context)
            return fsm.sendEvent(event, context) ?: error("Expected context not null")
        }

        private val definition = functionalStateMachine(
            LockStates.values().toSet(),
            LockEvents.values().toSet(),
            ImmutableLock::class
        ) {
            defaultInitialState = LockStates.LOCKED
            initialState {
                when (locked) {
                    0 -> LockStates.UNLOCKED
                    1 -> LockStates.LOCKED
                    2 -> LockStates.DOUBLE_LOCKED
                    else -> error("Invalid state locked=$locked")
                }
            }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            default {
                action { state, event, _ ->
                    println("Default action for state($state) -> on($event) for $this")
                    this
                }
                onEntry { startState, targetState, _ ->
                    println("entering:$startState -> $targetState for $this")
                }
                onExit { startState, targetState, _ ->
                    println("exiting:$startState -> $targetState for $this")
                }
            }
            whenState(LockStates.LOCKED) {
                onEvent(LockEvents.LOCK to LockStates.DOUBLE_LOCKED) {
                    doubleLock()
                }
                onEvent(LockEvents.UNLOCK to LockStates.UNLOCKED) {
                    unlock()
                }
            }
            whenState(LockStates.DOUBLE_LOCKED) {
                onEvent(LockEvents.UNLOCK to LockStates.LOCKED) {
                    doubleUnlock()
                }
            }
            whenState(LockStates.UNLOCKED) {
                onEvent(LockEvents.LOCK to LockStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }
}

operator fun ImmutableLock.plus(event: LockEvents): ImmutableLock {
    return ImmutableLockFSM.handleEvent(this, event)
}
// end::definition[]

class TestFunctionalLock() {
    @Test
    fun testState() {
        // tag::test[]
        val lock = ImmutableLock(0)
        val locked = lock + LockEvents.LOCK
        assertEquals(locked.locked, 1)
        val doubleLocked = locked + LockEvents.LOCK
        assertEquals(doubleLocked.locked, 2)
        // end::test[]
    }
}
