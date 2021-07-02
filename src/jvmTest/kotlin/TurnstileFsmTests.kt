/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
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
        fsm: AnyStateMachineInstance<TurnstileStates, TurnstileEvents, Turnstile>,
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
        val builder = AnyStateMachineBuilder<TurnstileStates, TurnstileEvents, Turnstile>(
            TurnstileStates.values().toSet(),
            TurnstileEvents.values().toSet()
        )
        builder.initialState { if (locked) LOCKED else UNLOCKED }
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
            stateMachine(
                TurnstileStates.values().toSet(),
                TurnstileEvents.values().toSet(),
                Turnstile::class
            ) {
                initialState { if (locked) LOCKED else UNLOCKED }
                default {
                    action { _, _, _ ->
                        alarm()
                    }
                }
                whenState(LOCKED) {
                    onEvent(COIN to UNLOCKED) {
                        unlock()
                    }
                }
                whenState(UNLOCKED) {
                    onEvent(COIN) {
                        returnCoin()
                    }
                    onEvent(PASS to LOCKED) {
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
            stateMachine(
                TurnstileStates.values().toSet(),
                TurnstileEvents.values().toSet(),
                Turnstile::class
            ) {
                initialState { if (locked) LOCKED else UNLOCKED }
                whenState(LOCKED) {
                    onEntry { startState, targetState, _ ->
                        println("entering:$startState -> $targetState for $this")
                    }
                    onEvent(COIN to UNLOCKED) {
                        unlock()
                    }
                    onEvent(PASS) {
                        alarm()
                    }
                    onExit { startState, targetState, _ ->
                        println("exiting:$startState -> $targetState for $this")
                    }
                }
                whenState(UNLOCKED) {
                    onEntry { startState, targetState, _ ->
                        println("entering:$startState -> $targetState for $this")
                    }
                    onEvent(COIN) {
                        returnCoin()
                    }
                    onEvent(PASS to LOCKED) {
                        lock()
                    }
                    onExit { startState, targetState, _ ->
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
