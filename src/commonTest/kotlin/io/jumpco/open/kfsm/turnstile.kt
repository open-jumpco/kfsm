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
class Turnstile(var locked: Boolean = true) {
    fun unlock() {
        require(locked) { "Cannot unlock when not locked" }
        println("Unlock")
        locked = false
    }

    fun lock() {
        require(!locked) { "Cannot lock when locked" }
        println("Lock")
        locked = true
    }

    fun alarm() {
        println("Alarm")
    }

    fun returnCoin() {
        println("Return Coin")
    }

    override fun toString(): String {
        return "Turnstile(locked=$locked)"
    }

}
/**
 * @suppress
 */
enum class TurnstileStates {
    LOCKED,
    UNLOCKED
}
/**
 * @suppress
 */
enum class TurnstileEvents {
    COIN,
    PASS
}

/**
 * @suppress
 */
class TurnstileFSM(turnstile: Turnstile) {
    companion object {
        private fun define() = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().stateMachine {
            initial { if (it.locked) TurnstileStates.LOCKED else TurnstileStates.UNLOCKED }
            default {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                action { ts, state, event ->
                    println("Default action for state($state) -> on($event) for $ts")
                    ts.alarm()
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
            state(TurnstileStates.LOCKED) {
                on(TurnstileEvents.COIN to TurnstileStates.UNLOCKED) { ts ->
                    ts.unlock()
                }
            }
            state(TurnstileStates.UNLOCKED) {
                on(TurnstileEvents.COIN) { ts ->
                    ts.returnCoin()
                }
                on(TurnstileEvents.PASS to TurnstileStates.LOCKED) { ts ->
                    ts.lock()
                }
            }
        }.build()
        private val definition by lazy { define() }
    }
    private val fsm = definition.create(turnstile)

    fun coin() = fsm.sendEvent(TurnstileEvents.COIN)
    fun pass() = fsm.sendEvent(TurnstileEvents.PASS)
}
