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

/**
 * @suppress
 */
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

/**
 * @suppress
 */
class PayingTurnstileFSM(turnstile: PayingTurnstile, initialState: ExternalState<PayingTurnstileStates>? = null) {
    val fsm = if (initialState != null) definition.create(turnstile, initialState) else definition.create(turnstile, PayingTurnstileStates.LOCKED)

    fun coin(value: Int) {
        println("sendEvent:COIN:$value")
        fsm.sendEvent(PayingTurnstileEvents.COIN, value)
    }

    fun pass() {
        println("sendEvent:PASS")
        fsm.sendEvent(PayingTurnstileEvents.PASS)
    }

    fun allowedEvents() = fsm.allowed().map { it.name.toLowerCase() }.toSet()
    fun externalState() = fsm.externalState()

    companion object {
        val definition = stateMachine(
            setOf(PayingTurnstileStates.LOCKED, PayingTurnstileStates.UNLOCKED),
            PayingTurnstileEvents::class,
            PayingTurnstile::class
        ) {
            default {
                entry { _, targetState, args ->
                    if (args.isNotEmpty()) {
                        println("entering:$targetState (${args.toList()}) for $this")
                    } else {
                        println("entering:$targetState for $this")
                    }
                }
                action { state, event, args ->
                    if (args.isNotEmpty()) {
                        println("Default action for state($state) -> on($event, ${args.toList()}) for $this")
                    } else {
                        println("Default action for state($state) -> on($event) for $this")
                    }
                    alarm()
                }
                exit { startState, _, args ->
                    if (args.isNotEmpty()) {
                        println("exiting:$startState (${args.toList()}) for $this")
                    } else {
                        println("exiting:$startState for $this")
                    }
                }
            }
            stateMap("coins", setOf(PayingTurnstileStates.COINS)) {
                state(PayingTurnstileStates.COINS) {
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
                    transition(PayingTurnstileEvents.COIN) { args ->
                        val value = args[0] as Int
                        coin(value)
                        println("Coins=$coins")
                        if (coins < requiredCoins) {
                            println("Please add ${requiredCoins - coins}")
                        }
                    }
                }
            }
            state(PayingTurnstileStates.LOCKED) {
                // The coin brings amount to exact amount
                pushTransition(PayingTurnstileEvents.COIN, "coins", PayingTurnstileStates.COINS) { args ->
                    val value = args[0] as Int
                    coin(value)
                    unlock()
                    reset()
                }
                // The coins add up to more than required
                pushTransition(PayingTurnstileEvents.COIN, "coins", PayingTurnstileStates.COINS,
                    guard = { args ->
                        val value = args[0] as Int
                        value + this.coins < this.requiredCoins
                    }) { args ->
                    val value = args[0] as Int
                    println("PUSH TRANSITION")
                    coin(value)
                    println("Coins=$coins, Please add ${requiredCoins - coins}")
                }
            }
            state(PayingTurnstileStates.UNLOCKED) {
                transition(PayingTurnstileEvents.COIN) { args ->
                    val value = args[0] as Int
                    returnCoin(coin(value))
                }
                transition(PayingTurnstileEvents.PASS to PayingTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }
}
