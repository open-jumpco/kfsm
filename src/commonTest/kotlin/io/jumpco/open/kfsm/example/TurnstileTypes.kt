/*
    Copyright 2019-2024 Open JumpCO

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

import io.jumpco.open.kfsm.stateMachine

/**
 * @suppress
 */
// tag::context[]
class Turnstile(locked: Boolean = true) {
    var locked: Boolean = locked
        private set

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
// end::context[]
/**
 * @suppress
 */
// tag::states-events[]
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
// end::states-events[]
/**
 * @suppress
 */

// tag::packaged[]
class TurnstileFSM(turnstile: Turnstile, savedState: TurnstileStates? = null) {
    private val fsm = definition.create(turnstile, savedState)

    fun externalState() = fsm.currentState
    fun coin() = fsm.sendEvent(TurnstileEvents.COIN)
    fun pass() = fsm.sendEvent(TurnstileEvents.PASS)
    fun allowedEvents() = fsm.allowed().map { it.name.lowercase() }.toSet()

    companion object {
        val definition =
            stateMachine(
                TurnstileStates.entries.toSet(),
                TurnstileEvents.entries.toSet(),
                Turnstile::class
            ) {
                defaultInitialState = TurnstileStates.LOCKED
                initialState {
                    if (locked)
                        TurnstileStates.LOCKED
                    else
                        TurnstileStates.UNLOCKED
                }
                onStateChange { oldState, newState ->
                    println("onStateChange:$oldState -> $newState")
                }
                default {
                    onEntry { startState, targetState, _ ->
                        println("entering:$startState -> $targetState for $this")
                    }
                    action { state, event, _ ->
                        println("Default action for state($state) -> on($event) for $this")
                        alarm()
                    }
                    onExit { startState, targetState, _ ->
                        println("exiting:$startState -> $targetState for $this")
                    }
                }
                whenState(TurnstileStates.LOCKED) {
                    onEvent(TurnstileEvents.COIN to TurnstileStates.UNLOCKED) {
                        unlock()
                    }
                }
                whenState(TurnstileStates.UNLOCKED) {
                    onEvent(TurnstileEvents.COIN) {
                        returnCoin()
                    }
                    onEvent(TurnstileEvents.PASS to TurnstileStates.LOCKED) {
                        lock()
                    }
                }
            }.build()

        fun possibleEvents(state: TurnstileStates, includeDefault: Boolean = false) =
            definition.possibleEvents(state, includeDefault)
    }
}
// end::packaged[]
/**
 * @suppress
 */
class TurnstileAlternate(private val turnstile: Turnstile, initialState: TurnstileStates? = null) {
    var currentState: TurnstileStates
        private set

    init {
        currentState = initialState ?: if (turnstile.locked)
            TurnstileStates.LOCKED
        else
            TurnstileStates.UNLOCKED
    }

    private fun defaultEntry(startState: TurnstileStates, targetState: TurnstileStates, event: TurnstileEvents) {
        println("entering:$startState -> $targetState on($event) for $this")
    }

    private fun defaultExit(startState: TurnstileStates, targetState: TurnstileStates, event: TurnstileEvents) {
        println("exiting:$startState -> $targetState on($event) for $this")
    }

    private fun defaultAction(state: TurnstileStates, event: TurnstileEvents) {
        println("Default action for state($state) -> on($event) for $this")
        turnstile.alarm()
    }

    fun coin() {
        when (currentState) {
            TurnstileStates.LOCKED -> run {
                defaultExit(TurnstileStates.LOCKED, TurnstileStates.UNLOCKED, TurnstileEvents.COIN)
                turnstile.unlock()
                defaultEntry(TurnstileStates.LOCKED, TurnstileStates.UNLOCKED, TurnstileEvents.COIN)
                currentState = TurnstileStates.UNLOCKED
            }

            TurnstileStates.UNLOCKED -> run {
                turnstile.returnCoin()
            }
        }
    }

    fun pass() {
        when (currentState) {
            TurnstileStates.UNLOCKED -> run {
                defaultExit(TurnstileStates.UNLOCKED, TurnstileStates.LOCKED, TurnstileEvents.PASS)
                turnstile.lock()
                defaultEntry(TurnstileStates.UNLOCKED, TurnstileStates.LOCKED, TurnstileEvents.PASS)
                currentState = TurnstileStates.LOCKED
            }

            else -> defaultAction(currentState, TurnstileEvents.PASS)
        }
    }
}
