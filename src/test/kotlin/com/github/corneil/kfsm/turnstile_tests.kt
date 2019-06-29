package com.github.corneil.kfsm

import com.github.corneil.kfsm.TurnstileEvents.COIN
import com.github.corneil.kfsm.TurnstileEvents.PASS
import com.github.corneil.kfsm.TurnstileStates.LOCKED
import com.github.corneil.kfsm.TurnstileStates.UNLOCKED
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertTrue


class TurnstileFsmTests {
    private fun verifyTurnstileFSM(
        fsm: StateMachine.StateMachineInstance<TurnstileStates, TurnstileEvents, Turnstile>,
        turnstile: Turnstile
    ) {
        every { turnstile.alarm() } just Runs
        every { turnstile.unlock() } just Runs
        every { turnstile.thankYou() } just Runs
        every { turnstile.lock() } just Runs

        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.event(COIN)
        // then
        verify { turnstile.unlock() }
        assertTrue { fsm.currentState == UNLOCKED }
        // when
        fsm.event(COIN)
        // then
        verify { turnstile.thankYou() }
        assertTrue { fsm.currentState == UNLOCKED }
        // when
        fsm.event(TurnstileEvents.PASS)
        // then
        verify { turnstile.lock() }
        assertTrue { fsm.currentState == LOCKED }
        // when
        fsm.event(TurnstileEvents.PASS)
        // then
        verify { turnstile.alarm() }
        assertTrue { fsm.currentState == LOCKED }
    }

    @Test
    fun `Uncle Bob's Turnstile plain`() {
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>()
        definition.initial { if (it.locked) LOCKED else UNLOCKED }
        definition.transition(LOCKED, COIN, UNLOCKED) { ts ->
            ts.unlock()
        }
        definition.transition(LOCKED, PASS) { ts ->
            ts.alarm()
        }
        definition.transition(UNLOCKED, COIN) { ts ->
            ts.thankYou()
        }
        definition.transition(UNLOCKED, PASS, LOCKED) { ts ->
            ts.lock();
        }
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
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().dsl {
            initial { if (it.locked) LOCKED else UNLOCKED }
            state(LOCKED) {
                event(COIN to UNLOCKED) { ts ->
                    ts.unlock()
                }
                event(PASS) { ts ->
                    ts.alarm()
                }
            }
            state(UNLOCKED) {
                event(COIN) { ts ->
                    ts.thankYou()
                }
                event(PASS to LOCKED) { ts ->
                    ts.lock();
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
        val definition = StateMachine<TurnstileStates, TurnstileEvents, Turnstile>().dsl {
            initial { if (it.locked) LOCKED else UNLOCKED }
            state(LOCKED) {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                event(COIN to UNLOCKED) { ts ->
                    ts.unlock()
                }
                event(PASS) { ts ->
                    ts.alarm()
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
            state(UNLOCKED) {
                entry { context, startState, endState ->
                    println("entering:$startState -> $endState for $context")
                }
                event(COIN) { ts ->
                    ts.thankYou()
                }
                event(PASS to LOCKED) { ts ->
                    ts.lock();
                }
                exit { context, startState, endState ->
                    println("exiting:$startState -> $endState for $context")
                }
            }
        }.build()

        val turnstile = Turnstile()
        val fsm = definition.create(turnstile)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.event(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.event(PASS)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.event(PASS)

        assertTrue { turnstile.locked }
        assertTrue { fsm.currentState == LOCKED }

        fsm.event(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }

        fsm.event(COIN)

        assertTrue { !turnstile.locked }
        assertTrue { fsm.currentState == UNLOCKED }
    }

    @Test
    fun `fsm component test`() {
        val turnstile = Turnstile()
        val fsm = TurnstileFSM(turnstile)
        println("--coin1")
        fsm.coin()
        println("--pass1")
        fsm.pass()
        println("--pass2")
        fsm.pass()
        println("--pass3")
        fsm.pass()
        println("--coin2")
        fsm.coin()
        println("--coin3")
        fsm.coin()
    }
}
