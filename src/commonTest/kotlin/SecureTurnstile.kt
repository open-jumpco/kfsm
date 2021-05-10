/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

// tag::states-events[]
enum class SecureTurnstileEvents {
    CARD,
    PASS
}
// end::states-events[]

enum class SecureTurnstileStates {
    LOCKED,
    UNLOCKED
}

// tag::context[]
class SecureTurnstile {
    var locked: Boolean = true
        private set
    var overrideActive: Boolean = false
        private set

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
class SecureTurnstileFSM(secureTurnstile: SecureTurnstile) {
    companion object {
        val definition = stateMachine(
            SecureTurnstileStates.values().toSet(),
            SecureTurnstileEvents.values().toSet(),
            SecureTurnstile::class,
            Int::class
        ) {
            defaultInitialState = SecureTurnstileStates.LOCKED
            initialState { if (locked) SecureTurnstileStates.LOCKED else SecureTurnstileStates.UNLOCKED }
            default {
                action { _, _, _ ->
                    buzzer()
                }
            }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            whenState(SecureTurnstileStates.LOCKED) {
                onEvent(
                    SecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId) && overrideActive
                    }
                ) {
                    cancelOverride()
                }
                onEvent(
                    SecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    activateOverride()
                }
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.UNLOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        overrideActive || isValidCard(cardId)
                    }
                ) {
                    unlock()
                }
                onEvent(
                    SecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId) { "cardId is required" }
                        !isValidCard(cardId)
                    }
                ) { cardId ->
                    requireNotNull(cardId)
                    invalidCard(cardId)
                }
            }
            whenState(SecureTurnstileStates.UNLOCKED) {
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.LOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    lock()
                }
                onEvent(SecureTurnstileEvents.PASS to SecureTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(secureTurnstile)
    fun card(cardId: Int) = fsm.sendEvent(SecureTurnstileEvents.CARD, cardId)
    fun pass() = fsm.sendEvent(SecureTurnstileEvents.PASS)
    fun allowEvent(): Set<String> = fsm.allowed().map { it.name.toLowerCase() }.toSet()
}
// end::fsm[]
