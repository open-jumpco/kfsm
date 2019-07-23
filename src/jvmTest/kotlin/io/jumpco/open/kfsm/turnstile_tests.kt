/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.jumpco.open.kfsm

import io.jumpco.open.kfsm.TurnstileEvents.COIN
import io.jumpco.open.kfsm.TurnstileEvents.PASS
import io.jumpco.open.kfsm.TurnstileStates.LOCKED
import io.jumpco.open.kfsm.TurnstileStates.UNLOCKED
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * @suppress
 */
class TurnstileFsmTests {
    private fun verifyTurnstileFSM(
        fsm: StateMachineInstance<TurnstileStates, TurnstileEvents, Turnstile>,
        turnstile: Turnstile
    ) {
        every { turnstile.alarm() } just Runs
        every { turnstile.unlock() } just Runs
        every { turnstile.returnCoin() } just Runs
        every { turnstile.lock() } just Runs

        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.sendEvent(COIN)
        // then
        verify { turnstile.unlock() }
        assertTrue { fsm.currentState == UNLOCKED }
        // when
        fsm.sendEvent(COIN)
        // then
        verify { turnstile.returnCoin() }
        assertTrue { fsm.currentState == UNLOCKED }
        // when
        fsm.sendEvent(PASS)
        // then
        verify { turnstile.lock() }
        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.sendEvent(PASS)
        // then
        verify { turnstile.alarm() }
        assertTrue { fsm.currentState == LOCKED }
    }

    @Test
    fun `Uncle Bob's Turnstile plain`() {
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>()
        definition.initial { if (locked) LOCKED else UNLOCKED }
        definition.defaultAction { _, _, _ ->
            alarm()
        }
        definition.transition(LOCKED, COIN, UNLOCKED) {
            unlock()
        }
        definition.transition(UNLOCKED, COIN) {
            returnCoin()
        }
        definition.transition(UNLOCKED, PASS, LOCKED) {
            lock()
        }
        definition.complete()
        // when
        val turnstile = mockk<Turnstile>()

        every { turnstile.locked } returns true
        val fsm = definition.create(turnstile)
        // then
        verifyTurnstileFSM(fsm, turnstile)
    }

    @Test
    fun `Uncle Bob's Turnstile DSL`() {
        // given
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().stateMachine {
            initial { if (locked) LOCKED else UNLOCKED }
            default {
                action { _, _, _ ->
                    alarm()
                }
            }
            state(LOCKED) {
                on(COIN to UNLOCKED) {
                    unlock()
                }
            }
            state(UNLOCKED) {
                on(COIN) {
                    returnCoin()
                }
                on(PASS to LOCKED) {
                    lock()
                }
            }
        }.build()
        // when
        val turnstile = mockk<Turnstile>()

        every { turnstile.locked } returns true
        val fsm = definition.create(turnstile)
        // then
        verifyTurnstileFSM(fsm, turnstile)
    }

    @Test
    fun `Simple Turnstile Test`() {
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().stateMachine {
            initial { if (locked) LOCKED else UNLOCKED }
            state(LOCKED) {
                entry { startState, endState, _ ->
                    println("entering:$startState -> $endState for $this")
                }
                on(COIN to UNLOCKED) {
                    unlock()
                }
                on(PASS) {
                    alarm()
                }
                exit { startState, endState, _ ->
                    println("exiting:$startState -> $endState for $this")
                }
            }
            state(UNLOCKED) {
                entry { startState, endState, _ ->
                    println("entering:$startState -> $endState for $this")
                }
                on(COIN) {
                    returnCoin()
                }
                on(PASS to LOCKED) {
                    lock()
                }
                exit { startState, endState, _ ->
                    println("exiting:$startState -> $endState for $this")
                }
            }
        }.build()

        val turnstile = Turnstile()
        val fsm = definition.create(turnstile)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.sendEvent(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.sendEvent(PASS)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.sendEvent(PASS)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.sendEvent(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.sendEvent(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }
    }

    @Test
    fun `fsm component test`() {
        val turnstile = Turnstile()
        val fsm = TurnstileFSM(turnstile)
        println("--coin1")
        fsm.coin()
        println("--coin2")
        fsm.coin()
        println("--pass1")
        fsm.pass()
        println("--pass2")
        fsm.pass()
        println("--pass3")
        fsm.pass()
        println("--coin3")
        fsm.coin()
    }
}
