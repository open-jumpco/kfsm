/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jumpco.open.kfsm.example

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
