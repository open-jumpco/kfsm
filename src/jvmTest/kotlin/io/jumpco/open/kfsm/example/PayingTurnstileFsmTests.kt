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
