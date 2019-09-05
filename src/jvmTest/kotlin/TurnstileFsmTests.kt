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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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
    fun `Turnstile plain`() {
        val builder = StateMachineBuilder<TurnstileStates, TurnstileEvents, Turnstile>(
            TurnstileStates.values().toSet(),
            TurnstileEvents.values().toSet()
        )
        builder.initial { if (locked) LOCKED else UNLOCKED }
        builder.defaultAction { _, _, _ ->
            alarm()
        }
        builder.transition(LOCKED, COIN, UNLOCKED) {
            unlock()
        }
        builder.transition(UNLOCKED, COIN) {
            returnCoin()
        }
        builder.transition(UNLOCKED, PASS, LOCKED) {
            lock()
        }
        val definition = builder.complete()
        // when
        val turnstile = mockk<Turnstile>()

        every { turnstile.locked } returns true
        val fsm = definition.create(turnstile)
        // then
        verifyTurnstileFSM(fsm, turnstile)
    }

    @Test
    fun `Turnstile DSL`() {
        // given
        val definition =
            StateMachineBuilder<TurnstileStates, TurnstileEvents, Turnstile>(
                TurnstileStates.values().toSet(),
                TurnstileEvents.values().toSet()
            ).stateMachine {
                initial { if (locked) LOCKED else UNLOCKED }
                default {
                    action { _, _, _ ->
                        alarm()
                    }
                }
                state(LOCKED) {
                    transition(COIN to UNLOCKED) {
                        unlock()
                    }
                }
                state(UNLOCKED) {
                    transition(COIN) {
                        returnCoin()
                    }
                    transition(PASS to LOCKED) {
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
        val definition =
            StateMachineBuilder<TurnstileStates, TurnstileEvents, Turnstile>(
                TurnstileStates.values().toSet(),
                TurnstileEvents.values().toSet()
            ).stateMachine {
                initial { if (locked) LOCKED else UNLOCKED }
                state(LOCKED) {
                    entry { startState, targetState, _ ->
                        println("entering:$startState -> $targetState for $this")
                    }
                    transition(COIN to UNLOCKED) {
                        unlock()
                    }
                    transition(PASS) {
                        alarm()
                    }
                    exit { startState, targetState, _ ->
                        println("exiting:$startState -> $targetState for $this")
                    }
                }
                state(UNLOCKED) {
                    entry { startState, targetState, _ ->
                        println("entering:$startState -> $targetState for $this")
                    }
                    transition(COIN) {
                        returnCoin()
                    }
                    transition(PASS to LOCKED) {
                        lock()
                    }
                    exit { startState, targetState, _ ->
                        println("exiting:$startState -> $targetState for $this")
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

    @Test
    fun `Alternative Turnstile Test`() {

        val turnstile = Turnstile()
        val fsm = TurnstileAlternate(turnstile)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.coin()

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.pass()

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.pass()

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.coin()

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.coin()

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }
    }
}
