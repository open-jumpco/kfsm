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
// tag::context[]
class PayingTurnstile(
    val requiredCoins: Int,
    locked: Boolean = true,
    coins: Int = 0
) {
    var coins: Int = coins
        private set
    var locked: Boolean = locked
        private set

    fun unlock() {
        require(locked) { "Cannot unlock when not locked" }
        require(coins >= requiredCoins) { "Not enough coins. ${requiredCoins - coins} required" }
        println("Unlock")
        locked = false
    }

    fun lock() {
        require(!locked) { "Cannot lock when locked" }
        require(coins == 0) { "Coins $coins must be returned" }
        println("Lock")
        locked = true
    }

    fun alarm() {
        println("Alarm")
    }

    fun coin(value: Int): Int {
        coins += value
        println("Coin received=$value, Total=$coins")
        return coins
    }

    fun returnCoin(returnCoins: Int) {
        println("Return Coin:$returnCoins")
        coins -= returnCoins
    }

    fun reset() {
        coins = 0
        println("Reset coins=$coins")
    }

    override fun toString(): String {
        return "Turnstile(locked=$locked,coins=$coins)"
    }
}
// end::context[]
/**
 * @suppress
 */
// tag::states-events[]
enum class PayingTurnstileStates {
    LOCKED,
    COINS,
    UNLOCKED
}

/**
 * @suppress
 */
enum class PayingTurnstileEvents {
    COIN,
    PASS
}

// end::states-events[]

/**
 * @suppress
 */
// tag::packaged[]
data class PayingTurnstileFSMExternalState(
    val coins: Int,
    val locked: Boolean,
    val initialState: ExternalState<PayingTurnstileStates>
)

class PayingTurnstileFSM(
    requiredCoins: Int,
    savedState: PayingTurnstileFSMExternalState? = null
) {
    // not private for visibility for tests
    val turnstile: PayingTurnstile = PayingTurnstile(requiredCoins, savedState?.locked ?: true, savedState?.coins ?: 0)
    val fsm = if (savedState != null) {
        definition.create(turnstile, savedState.initialState)
    } else {
        definition.create(turnstile)
    }

    fun coin(value: Int) {
        println("sendEvent:COIN:$value")
        fsm.sendEvent(PayingTurnstileEvents.COIN, value)
    }

    fun pass() {
        println("sendEvent:PASS")
        fsm.sendEvent(PayingTurnstileEvents.PASS)
    }

    fun allowedEvents() = fsm.allowed().map { it.name.toLowerCase() }.toSet()
    fun externalState(): PayingTurnstileFSMExternalState {
        return PayingTurnstileFSMExternalState(turnstile.coins, turnstile.locked, fsm.externalState())
    }

    companion object {
        val definition = stateMachine(
            setOf(PayingTurnstileStates.LOCKED, PayingTurnstileStates.UNLOCKED),
            PayingTurnstileEvents.values().toSet(),
            PayingTurnstile::class,
            Int::class
        ) {
            defaultInitialState = PayingTurnstileStates.LOCKED
            default {
                onEntry { _, targetState, arg ->
                    if (arg != null) {
                        println("entering:$targetState ($arg) for $this")
                    } else {
                        println("entering:$targetState for $this")
                    }
                }
                action { state, event, arg ->
                    if (arg != null) {
                        println("Default action for state($state) -> on($event, $arg) for $this")
                    } else {
                        println("Default action for state($state) -> on($event) for $this")
                    }
                    alarm()
                }
                onExit { startState, _, arg ->
                    if (arg != null) {
                        println("exiting:$startState ($arg) for $this")
                    } else {
                        println("exiting:$startState for $this")
                    }
                }
            }
            stateMap("coins", setOf(PayingTurnstileStates.COINS)) {
                whenState(PayingTurnstileStates.COINS) {
                    automaticPop(PayingTurnstileStates.UNLOCKED, guard = { coins > requiredCoins }) {
                        println("automaticPop:returnCoin")
                        returnCoin(coins - requiredCoins)
                        unlock()
                        reset()
                    }
                    automaticPop(PayingTurnstileStates.UNLOCKED, guard = { coins == requiredCoins }) {
                        println("automaticPop")
                        unlock()
                        reset()
                    }
                    onEvent(PayingTurnstileEvents.COIN) { value ->
                        requireNotNull(value) { "argument required for COIN" }
                        coin(value)
                        println("Coins=$coins")
                        if (coins < requiredCoins) {
                            println("Please add ${requiredCoins - coins}")
                        }
                    }
                }
            }
            whenState(PayingTurnstileStates.LOCKED) {
                // The coin brings amount to exact amount
                onEventPush(PayingTurnstileEvents.COIN, "coins", PayingTurnstileStates.COINS) { value ->
                    requireNotNull(value) { "argument required for COIN" }
                    coin(value)
                    unlock()
                    reset()
                }
                // The coins add up to more than required
                onEventPush(PayingTurnstileEvents.COIN, "coins", PayingTurnstileStates.COINS,
                    guard = { value ->
                        requireNotNull(value) { "argument required for COIN" }
                        value + coins < requiredCoins
                    }) { value ->
                    requireNotNull(value) { "argument required for COIN" }
                    println("PUSH TRANSITION")
                    coin(value)
                    println("Coins=$coins, Please add ${requiredCoins - coins}")
                }
            }
            whenState(PayingTurnstileStates.UNLOCKED) {
                onEvent(PayingTurnstileEvents.COIN) { value ->
                    requireNotNull(value) { "argument required for COIN" }
                    returnCoin(coin(value))
                }
                onEvent(PayingTurnstileEvents.PASS to PayingTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }
}
// end::packaged[]
