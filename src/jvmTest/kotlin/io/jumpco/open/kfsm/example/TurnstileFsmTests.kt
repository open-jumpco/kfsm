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

import io.jumpco.open.kfsm.AnyStateMachineBuilder
import io.jumpco.open.kfsm.AnyStateMachineInstance
import io.jumpco.open.kfsm.example.TurnstileEvents.COIN
import io.jumpco.open.kfsm.example.TurnstileEvents.PASS
import io.jumpco.open.kfsm.example.TurnstileStates.LOCKED
import io.jumpco.open.kfsm.example.TurnstileStates.UNLOCKED
import io.jumpco.open.kfsm.stateMachine

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


        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.sendEvent(COIN)
        // then

        assertTrue { fsm.currentState == UNLOCKED }
        // when
        fsm.sendEvent(COIN)
        // then

        assertTrue { fsm.currentState == UNLOCKED }
        // when
        fsm.sendEvent(PASS)
        // then

        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.sendEvent(PASS)
        // then

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
        val turnstile = Turnstile(true)


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
        val turnstile = Turnstile(true)

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
