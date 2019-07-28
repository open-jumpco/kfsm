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
        println("Value=$value, Total=$coins")
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
class PayingTurnstileFSM(turnstile: PayingTurnstile) {
    companion object {
        private val definition = stateMachine(
            PayingTurnstileStates::class,
            PayingTurnstileEvents::class,
            PayingTurnstile::class
        ) {
            initial {
                when {
                    coins > 0 -> PayingTurnstileStates.COINS
                    locked ->
                        PayingTurnstileStates.LOCKED
                    else ->
                        PayingTurnstileStates.UNLOCKED
                }
            }
            default {
                entry { startState, targetState, args ->
                    if (args != null && args.isNotEmpty()) {
                        println("entering:$targetState (${args.toList()}) for $this")
                    } else {
                        println("entering:$targetState for $this")
                    }
                }
                action { state, event, args ->
                    if (args != null && args.isNotEmpty()) {
                        println("Default action for state($state) -> on($event, ${args.toList()}) for $this")
                    } else {
                        println("Default action for state($state) -> on($event) for $this")
                    }
                    alarm()
                }
                exit { startState, _, args ->
                    if (args != null && args.isNotEmpty()) {
                        println("entering:$startState (${args.toList()}) for $this")
                    } else {
                        println("exiting:$startState for $this")
                    }
                }
            }
            state(PayingTurnstileStates.LOCKED) {
                on(PayingTurnstileEvents.COIN to PayingTurnstileStates.UNLOCKED,
                    guard = { args -> args[0] as Int + this.coins > this.requiredCoins }) { args ->
                    val value = args[0] as Int
                    returnCoin(coin(value) - requiredCoins)
                    unlock()
                    reset()
                }
                on(PayingTurnstileEvents.COIN to PayingTurnstileStates.COINS,
                    guard = { args -> args[0] as Int + this.coins < this.requiredCoins }) { args ->
                    val value = args[0] as Int
                    coin(value)
                    println("Coins=$coins, Please add ${requiredCoins - coins}")
                }
                on(PayingTurnstileEvents.COIN to PayingTurnstileStates.UNLOCKED,
                    guard = { args -> args[0] as Int + coins == requiredCoins }) { args ->
                    val value = args[0] as Int
                    coin(value)
                    unlock()
                    reset()
                }
            }
            state(PayingTurnstileStates.COINS) {
                on(PayingTurnstileEvents.COIN to PayingTurnstileStates.UNLOCKED,
                    guard = { args -> args[0] as Int + this.coins > this.requiredCoins }) { args ->
                    val value = args[0] as Int
                    returnCoin(coin(value) - requiredCoins)
                    unlock()
                    reset()
                }
                on(PayingTurnstileEvents.COIN to PayingTurnstileStates.COINS,
                    guard = { args -> args[0] as Int + this.coins < this.requiredCoins }) { args ->
                    val value = args[0] as Int
                    coin(value)
                    println("Coins=$coins, Please add ${requiredCoins - coins}")
                }
                on(PayingTurnstileEvents.COIN to PayingTurnstileStates.UNLOCKED) { args ->
                    val value = args[0] as Int
                    coin(value)
                    returnCoin(coins - requiredCoins)
                    unlock()
                    reset()
                }
            }
            state(PayingTurnstileStates.UNLOCKED) {
                on(PayingTurnstileEvents.COIN) { args ->
                    val value = args[0] as Int
                    returnCoin(coin(value))
                }
                on(PayingTurnstileEvents.PASS to PayingTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(turnstile)

    fun coin(value: Int) = fsm.sendEvent(PayingTurnstileEvents.COIN, value)
    fun pass() = fsm.sendEvent(PayingTurnstileEvents.PASS)
}
