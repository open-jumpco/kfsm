/*
 * Copyright (c) 2024. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class TimeoutSecureTurnstileTests {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun testNormalOperation() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile, coroutineScope)
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
    fun testInvalidCard() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile, coroutineScope)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        runBlocking {
            fsm.card(2)
            assertTrue { fsm.allowEvent() == setOf("card") }
            assertTrue { turnstile.locked }
            fsm.pass()
            assertTrue { turnstile.locked }
        }
    }

    @Test
    fun testInvalidCardOverride() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile, coroutineScope)
        // when
        assertTrue { turnstile.locked }
        runBlocking {
            fsm.card(2)
            assertTrue { turnstile.locked }
            fsm.card(42) // override card
            fsm.card(2)
            assertTrue { !turnstile.locked }
            fsm.pass()
            assertTrue { turnstile.locked }
        }
    }

    @Test
    fun testCancelOverrideToLock() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile, coroutineScope)
        // when
        assertTrue { turnstile.locked }
        runBlocking {
            fsm.card(2)
            assertTrue { turnstile.locked }
            fsm.card(42)  // override card
            fsm.card(2)
            assertTrue { !turnstile.locked }
            assertTrue { fsm.allowEvent() == setOf("card", "pass") }
            fsm.card(42) // override card
            assertTrue { turnstile.locked }
        }
    }

    @Test
    fun testCancelOverride() {
        // given
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile, coroutineScope)
        // when
        assertTrue { turnstile.locked }
        runBlocking {
            fsm.card(2)
            assertTrue { turnstile.locked }
            fsm.card(42)  // override card
            assertTrue { turnstile.overrideActive }
            fsm.card(42) // override card
            assertTrue { !turnstile.overrideActive }
            fsm.card(2)
            assertTrue { turnstile.locked }
        }
    }

    // tag::test[]
    @Test
    fun testTimeout() {
        val turnstile = TimerSecureTurnstile()
        val fsm = TimerSecureTurnstileFSM(turnstile, coroutineScope)
        assertTrue { turnstile.locked }

        println("Card 1")
        runBlocking {
            fsm.card(1)
            println("Assertion")
            assertTrue { !turnstile.locked }
            println("Delay:100")
            delay(100L)
            assertTrue { !turnstile.locked }
            println("Delay:1000")
            delay(1000L)
            println("Assertion")
            assertTrue { turnstile.locked }
        }
    }
    // end::test[]
}
