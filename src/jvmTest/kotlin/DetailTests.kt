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
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetailTests {
    enum class TestStates {
        STATE1,
        STATE2,
        STATE3
    }

    enum class TestEvents {
        EVENT1,
        EVENT2,
        EVENT3
    }

    class TestContext(var state: Int) {
        fun defaultAction() {
            println("defaultAction")
        }

        fun defaultEntry() {
            println("defaultEntry")
        }

        fun defaultExit() {
            println("defaultEntry")
        }

        fun action1() {
            println("action1")
        }

        fun action2() {
            println("action2")
        }

        fun none() {}
        fun entry1() {
            println("entry1")
        }

        fun entry2() {
            println("entry2")
        }

        fun exit2() {
            println("exit2")
        }

        fun exit3() {
            println("exit3")
        }

        override fun toString(): String {
            return "TestContext(state=$state)"
        }
    }

    class TestDetailFSM(context: TestContext) {
        val fsm = definition.create(context)
        fun allowedEvents() = fsm.allowed()
        fun eventAllowed(event: TestEvents, includeDefaults: Boolean = false) =
            fsm.eventAllowed(event, includeDefaults)

        fun event1(msg: String) {
            println("--event1")
            fsm.sendEvent(TestEvents.EVENT1, msg)
        }

        fun event2(msg: String) {
            println("--event2")
            fsm.sendEvent(TestEvents.EVENT2, msg)
        }

        fun event3(msg: String) {
            println("--event3")
            fsm.sendEvent(TestEvents.EVENT3, msg)
        }

        companion object {
            val definition = stateMachine(
                TestStates.values().toSet(),
                TestEvents.values().toSet(),
                TestContext::class,
                String::class
            ) {
                initialStates {
                    mutableListOf<StateMapItem<TestStates>>().apply {
                        when (state) {
                            1 -> add(TestStates.STATE1 to "default")
                            2 -> add(TestStates.STATE2 to "default")
                            3 -> add(TestStates.STATE3 to "default")
                            else -> error("Invalid state $state")
                        }
                    }
                }
                default {
                    onEntry { startState, targetState, msg ->
                        println("entering:to $targetState from $startState for:$this:$msg")
                        defaultEntry()
                    }
                    onExit { startState, targetState, msg ->
                        println("exiting:from $targetState to $startState for:$this:$msg")
                        defaultExit()
                    }
                    onEvent(TestEvents.EVENT1 to TestStates.STATE1) { msg ->
                        println("default:EVENT1 to STATE1 for $this:$msg")
                        action1()
                        state = 1
                        none()
                    }
                    onEvent(TestEvents.EVENT2 to TestStates.STATE2) { msg ->
                        println("default:on EVENT2 to STATE2 for $this:$msg")
                        action2()
                        state = 2
                        none()
                    }
                    onEvent(TestEvents.EVENT3 to TestStates.STATE3) { msg ->
                        println("default:on EVENT3 to STATE3 for $this:$msg")
                        defaultAction()
                        state = 3
                        none()
                    }
                    action { currentState, event, msg ->
                        println("default:$event from $currentState for $this:$msg")
                        defaultAction()
                    }
                }
                stateMap("map1", setOf(TestStates.STATE1, TestStates.STATE2)) {
                    whenState(TestStates.STATE1) {
                        onEventPop(TestEvents.EVENT1) { msg ->
                            println("pop")
                        }
                    }
                    whenState(TestStates.STATE2) {
                        automatic(TestStates.STATE1) { msg ->
                            println("automatic -> TestStates.STATE1")
                        }
                    }
                }
                whenState(TestStates.STATE1) {
                    onEvent(TestEvents.EVENT1) { msg ->
                        action1()
                    }
                    onEntry { _, _, _ ->
                        entry1()
                    }
                }
                whenState(TestStates.STATE2) {
                    onEntry { _, _, _ ->
                        entry2()
                    }
                    onEvent(TestEvents.EVENT2, guard = { state == 2 }) { msg ->
                        println("EVENT2:guarded:from STATE2 for $this:$msg")
                        action2()
                    }
                    onEventPush(TestEvents.EVENT1, "map1", TestStates.STATE2) { msg ->
                        println("EVENT1:push:from STATE2 for $this:$msg")
                        action2()
                    }
                    onExit { _, _, _ ->
                        exit2()
                    }
                }
                whenState(TestStates.STATE3) {
                    onExit { _, _, _ ->
                        exit3()
                    }
                    onEvent(TestEvents.EVENT2, guard = { state == 2 }) {
                        error("should never be called")
                    }
                }
            }.build()
        }
    }

    @Test
    fun `test actions`() {
        // given
        val context = TestContext(3)
        val fsm = TestDetailFSM(context)
        var msgNo = 1
        // when
        fsm.event1((++msgNo).toString())
        // then
        assertEquals(1, context.state)
        assertTrue(fsm.eventAllowed(TestEvents.EVENT1))
        // when
        fsm.event2((++msgNo).toString())
        // then
        assertEquals(2, context.state)
        assertTrue(fsm.eventAllowed(TestEvents.EVENT2))
        // when
        fsm.event2((++msgNo).toString())
        assertEquals(2, context.state)
        // then
        fsm.event3((++msgNo).toString())
        assertEquals(3, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT2))
        // when
        fsm.event2((++msgNo).toString())
        assertEquals(3, context.state)
        // then
        fsm.event1((++msgNo).toString())
        assertEquals(1, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT1))
        fsm.event2((++msgNo).toString())
        assertEquals(2, context.state)
        assertEquals(fsm.fsm.currentState, TestStates.STATE2)
        fsm.event1((++msgNo).toString())
        assertEquals(fsm.fsm.currentStateMap.name, "map1")
        assertEquals(fsm.fsm.currentState, TestStates.STATE1)
        fsm.event1((++msgNo).toString())
        assertNull(fsm.fsm.currentStateMap.name)
        assertEquals(fsm.fsm.currentState, TestStates.STATE2)
    }
}
