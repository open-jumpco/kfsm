/*
 * Copyright (c) 2021. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
