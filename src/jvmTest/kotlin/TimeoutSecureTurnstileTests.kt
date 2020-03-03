/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class TimeoutSecureTurnstileTests {
    @Test
    fun `test normal operation`() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        runBlocking {
            println("Card 1")
            fsm.card(1)
            println("Asserting")
            assertTrue { !turnstile.locked }
            assertTrue { fsm.allowEvent() == setOf("pass", "card") }
            println("Pass")
            fsm.pass()
            println("Asserting")
            assertTrue { turnstile.locked }
        }
    }

    @Test
    fun `test invalid card`() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(2) }
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        runBlocking { fsm.pass() }
        assertTrue { turnstile.locked }
    }

    @Test
    fun `test invalid card override`() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(2) }
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(42) } // override card
        runBlocking { fsm.card(2) }
        assertTrue { !turnstile.locked }
        runBlocking { fsm.pass() }
        assertTrue { turnstile.locked }
    }

    @Test
    fun `test cancel override to lock`() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(2) }
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(42) } // override card
        runBlocking { fsm.card(2) }
        assertTrue { !turnstile.locked }
        assertTrue { fsm.allowEvent() == setOf("card", "pass") }
        runBlocking { fsm.card(42) } // override card
        assertTrue { turnstile.locked }
    }

    @Test
    fun `test cancel override`() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(2) }
        assertTrue { turnstile.locked }
        runBlocking { fsm.card(42) } // override card
        assertTrue { turnstile.overrideActive }
        runBlocking { fsm.card(42) } // override card
        assertTrue { !turnstile.overrideActive }
        runBlocking { fsm.card(2) }
        assertTrue { turnstile.locked }
    }
    // tag::test[]
    @Test
    fun `test timeout`() {
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile)
        assertTrue { turnstile.locked }
        println("Card 1")
        runBlocking {
            fsm.card(1)
            println("Assertion")
            assertTrue { !turnstile.locked }
            println("Delay")
            delay(1000L)
            println("Assertion")
            assertTrue { turnstile.locked }
        }
    }
    // end::test[]
}
