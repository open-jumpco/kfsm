/*
 * Copyright (c) 2024. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.example

import kotlin.test.Test
import kotlin.test.assertTrue

class SecureTurnstileTests {
    @Test
    fun testNormalOperation() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        fsm.card(1)
        assertTrue { !turnstile.locked }
        assertTrue { fsm.allowEvent() == setOf("pass", "card") }
        fsm.pass()
        assertTrue { turnstile.locked }
    }

    @Test
    fun testInvalidCard() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { fsm.allowEvent() == setOf("card") }
        assertTrue { turnstile.locked }
        fsm.pass()
        assertTrue { turnstile.locked }
    }

    @Test
    fun testInvalidCardOverride() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { turnstile.locked }
        fsm.card(42) // override card
        fsm.card(2)
        assertTrue { !turnstile.locked }
        fsm.pass()
        assertTrue { turnstile.locked }
    }

    @Test
    fun testCancelOverrideToLock() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { turnstile.locked }
        fsm.card(42) // override card
        fsm.card(2)
        assertTrue { !turnstile.locked }
        assertTrue { fsm.allowEvent() == setOf("card", "pass") }
        fsm.card(42) // override card
        assertTrue { turnstile.locked }
    }

    @Test
    fun testCancelOverride() {
        // given
        val turnstile = SecureTurnstile()
        val fsm = SecureTurnstileFSM(turnstile)
        // when
        assertTrue { turnstile.locked }
        fsm.card(2)
        assertTrue { turnstile.locked }
        fsm.card(42) // override card
        assertTrue { turnstile.overrideActive }
        fsm.card(42) // override card
        assertTrue { !turnstile.overrideActive }
        fsm.card(2)
        assertTrue { turnstile.locked }
    }
}
