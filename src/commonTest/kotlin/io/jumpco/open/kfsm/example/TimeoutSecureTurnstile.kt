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

import io.jumpco.open.kfsm.async.asyncStateMachine

// tag::states-events[]
enum class TimeoutSecureTurnstileEvents {
    CARD,
    PASS
}
// end::states-events[]

enum class TimeoutSecureTurnstileStates {
    LOCKED,
    UNLOCKED
}

// tag::context[]
class TimerSecureTurnstile {
    var locked: Boolean = true
        private set
    var overrideActive: Boolean = false
        private set
    val timeout = 500L
    fun activateOverride() {
        overrideActive = true
        println("override activated")
    }

    fun cancelOverride() {
        overrideActive = false
        println("override canceled")
    }

    fun lock() {
        println("lock")
        locked = true
        overrideActive = false
    }

    fun unlock() {
        println("unlock")
        locked = false
        overrideActive = false
    }

    fun buzzer() {
        println("BUZZER")
    }

    fun invalidCard(cardId: Int) {
        println("Invalid card $cardId")
    }

    fun isOverrideCard(cardId: Int): Boolean {
        return cardId == 42
    }

    fun isValidCard(cardId: Int): Boolean {
        return cardId % 2 == 1
    }
}
// end::context[]

@OptIn(ExperimentalStdlibApi::class)
// tag::fsm[]
class TimerSecureTurnstileFSM(secureTurnstile: TimerSecureTurnstile) {
    companion object {
        val definition = asyncStateMachine(
            TimeoutSecureTurnstileStates.values().toSet(),
            TimeoutSecureTurnstileEvents.values().toSet(),
            TimerSecureTurnstile::class,
            Int::class
        ) {
            defaultInitialState = TimeoutSecureTurnstileStates.LOCKED
            initialState { if (locked) TimeoutSecureTurnstileStates.LOCKED else TimeoutSecureTurnstileStates.UNLOCKED }
            default {
                action { _, _, _ ->
                    buzzer()
                }
            }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            whenState(TimeoutSecureTurnstileStates.LOCKED) {
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId) && overrideActive
                    }
                ) {
                    cancelOverride()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    activateOverride()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD to TimeoutSecureTurnstileStates.UNLOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        overrideActive || isValidCard(cardId)
                    }
                ) {
                    unlock()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId) { "cardId is required" }
                        !isValidCard(cardId)
                    }
                ) { cardId ->
                    requireNotNull(cardId)
                    invalidCard(cardId)
                }
            }
            whenState(TimeoutSecureTurnstileStates.UNLOCKED) {
                timeout(TimeoutSecureTurnstileStates.LOCKED, { timeout }) {
                    println("Timeout. Locking")
                    lock()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD to TimeoutSecureTurnstileStates.LOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    lock()
                }
                onEvent(TimeoutSecureTurnstileEvents.PASS to TimeoutSecureTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(secureTurnstile)
    suspend fun card(cardId: Int) = fsm.sendEvent(TimeoutSecureTurnstileEvents.CARD, cardId)
    suspend fun pass() = fsm.sendEvent(TimeoutSecureTurnstileEvents.PASS)
    fun allowEvent(): Set<String> = fsm.allowed().map { it.name.lowercase() }.toSet()
}
// end::fsm[]
