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
        val turnstile = PayingTurnstile(50)
        val fsm = PayingTurnstileFSM(turnstile)
        assertTrue(turnstile.locked)
        println("--coin1")
        fsm.coin(10)
        assertTrue(turnstile.locked)
        assertTrue(turnstile.coins == 10)
        assertTrue(
            fsm.fsm.currentStateMap.name != null && "coins" == fsm.fsm.currentStateMap.name,
            "Expected coins map"
        )
        println("--coin2")
        fsm.coin(60)
        assertTrue(turnstile.coins == 0)
        assertTrue(!turnstile.locked)
        assertTrue(fsm.fsm.currentStateMap.name == null, "Expected default map not ${fsm.fsm.currentStateMap.name}")
        println("--pass1")
        fsm.pass()
        assertTrue(turnstile.locked)
        println("--pass2")
        fsm.pass()
        println("--pass3")
        fsm.pass()
        println("--coin3")
        fsm.coin(40)
        assertTrue(turnstile.coins == 40)
        println("--coin4")
        fsm.coin(10)
        assertTrue(turnstile.coins == 0)
        assertTrue(!turnstile.locked)
    }
}
