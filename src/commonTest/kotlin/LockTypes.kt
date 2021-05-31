/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

/**
 * @suppress
 */
class Lock(initial: Int = 1) {
    var locked: Int = initial
        private set

    fun lock() {
        require(locked == 0)
        println("Lock")
        locked += 1
    }

    fun doubleLock() {
        require(locked == 1)
        println("DoubleLock")
        locked += 1
    }

    fun unlock() {
        require(locked == 1)
        println("Unlock")
        locked -= 1
    }

    fun doubleUnlock() {
        require(locked == 2)
        println("DoubleUnlock")
        locked -= 1
    }

    override fun toString(): String {
        return "Lock(locked=$locked)"
    }
}

enum class LockStates {
    LOCKED,
    DOUBLE_LOCKED,
    UNLOCKED
}

enum class LockEvents {
    LOCK,
    UNLOCK
}

class LockFSM(context: Lock) {
    private val fsm = definition.create(context)

    fun allowedEvents() = fsm.allowed()
    fun unlock() = fsm.sendEvent(LockEvents.UNLOCK)
    fun lock() = fsm.sendEvent(LockEvents.LOCK)

    companion object {
        private val definition = stateMachine(
            LockStates.values().toSet(),
            LockEvents.values().toSet(),
            Lock::class
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
            invariant("invalid locked value") { locked in 0..2 }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            default {
                action { state, event, _ ->
                    println("Default action for state($state) -> on($event) for $this")
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
