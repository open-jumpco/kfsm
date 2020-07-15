/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.jumpco.open.kfsm

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * @suppress
 */
class PayingTurnstileFsmTests {
    @Test
    fun `fsm component test`() {
        // tag::test[]
        val fsm = PayingTurnstileFSM(50)
        assertTrue(fsm.turnstile.locked)
        println("External:${fsm.externalState()}")
        println("--coin1")
        fsm.coin(10)
        assertTrue(fsm.turnstile.locked)
        assertTrue(fsm.turnstile.coins == 10)
        println("--coin2")
        println("External:${fsm.externalState()}")
        val externalState = fsm.externalState()
        PayingTurnstileFSM(50, externalState).apply {
            coin(60)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
            println("External:${externalState()}")
            println("--pass1")
            pass()
            assertTrue(turnstile.locked)
            println("--pass2")
            pass()
            println("--pass3")
            pass()
            println("--coin3")
            coin(40)
            assertTrue(turnstile.coins == 40)
            println("--coin4")
            coin(10)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
        }
        // end::test[]
    }

    @Test
    fun `fsm component test external state`() {
        val fsm = PayingTurnstileFSM(50)
        assertTrue(fsm.turnstile.locked)
        println("External:${fsm.externalState()}")
        println("--coin1")
        fsm.coin(10)
        assertTrue(fsm.turnstile.locked)
        assertTrue(fsm.turnstile.coins == 10)
        println("--coin2")
        val es1 = fsm.externalState()
        println("External:$es1")
        val es2 = PayingTurnstileFSM(50, es1).apply {
            this.coin(60)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
            println("External:${this.externalState()}")
            println("--pass1")
        }.externalState()
        val es3 = PayingTurnstileFSM(50, es2).apply {
            this.pass()
            assertTrue(turnstile.locked)
            println("--pass2")
        }.externalState()
        val es4 = PayingTurnstileFSM(50, es3).apply {
            this.pass()
            println("--pass3")
        }.externalState()
        val es5 = PayingTurnstileFSM(50, es4).apply {
            this.pass()
            println("--coin3")
        }.externalState()
        val es6 = PayingTurnstileFSM(50, es5).apply {
            this.coin(40)
            assertTrue(turnstile.coins == 40)
            println("--coin4")
        }.externalState()
        PayingTurnstileFSM(50, es6).apply {
            this.coin(10)
            assertTrue(turnstile.coins == 0)
            assertTrue(!turnstile.locked)
        }
    }
}
