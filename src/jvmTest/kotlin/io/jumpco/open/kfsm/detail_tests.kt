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
        companion object {
            private fun define() = StateMachine<TestStates, TestEvents, TestContext>().stateMachine {
                initial {
                    when (it.state) {
                        1 -> TestStates.STATE1
                        2 -> TestStates.STATE2
                        3 -> TestStates.STATE3
                        else -> error("Invalid state ${it.state}")
                    }
                }
                default {
                    entry { context, startState, endState, args ->
                        val msg = args[0] as String
                        println("entering:to $endState from $startState for:$context:$msg")
                        context.defaultEntry()
                    }
                    exit { context, startState, endState, args ->
                        val msg = args[0] as String
                        println("exiting:from $endState to $startState for:$context:$msg")
                        context.defaultExit()
                    }
                    on(TestEvents.EVENT1 to TestStates.STATE1) { context, args ->
                        val msg = args[0] as String
                        println("default:EVENT1 to STATE1 for $context:$msg")
                        context.action1()
                        context.state = 1

                    }
                    on(TestEvents.EVENT2 to TestStates.STATE2) { context, args ->
                        val msg = args[0] as String
                        println("default:on EVENT2 to STATE2 for $context:$msg")
                        context.action2()
                        context.state = 2
                    }
                    on(TestEvents.EVENT3 to TestStates.STATE3) { context, args ->
                        val msg = args[0] as String
                        println("default:on EVENT3 to STATE3 for $context:$msg")
                        context.defaultAction()
                        context.state = 3
                    }
                    action { context, currentState, event, args ->
                        val msg = args[0] as String
                        println("default:$event from $currentState for $context:$msg")
                        context.defaultAction()
                    }
                }
                state(TestStates.STATE1) {
                    on(TestEvents.EVENT1) { context, _ ->
                        context.action1()
                    }
                    entry { context, _, _, _ ->
                        context.entry1()
                    }

                }
                state(TestStates.STATE2) {
                    entry { context, _, _, _ ->
                        context.entry2()
                    }
                    on(TestEvents.EVENT2, guard = { it, _ -> it.state == 2 }) { context, args ->
                        val msg = args[0] as String
                        println("EVENT2:guarded:from STATE2 for $context:$msg")
                        context.action2()
                    }
                    exit { context, _, _, _ ->
                        context.exit2()
                    }
                }
                state(TestStates.STATE3) {
                    exit { context, _, _, _ ->
                        context.exit3()
                    }
                    on(TestEvents.EVENT2, guard = { it, _ -> it.state == 2 }) { _, _ ->
                        error("should never be called")
                    }
                }
            }.build()

            private val definition by lazy { define() }
        }

        private val fsm = definition.create(context)
        fun allowedEvents() = fsm.allowed()
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
    }

    @Test
    fun `test actions`() {
        // given
        val context = TestContext(3)
        val fsm = TestDetailFSM(context)
        var msgNo = 1
        // when
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT2))
        fsm.event1((++msgNo).toString())
        assertEquals(1, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT1))
        fsm.event2((++msgNo).toString())
        assertEquals(2, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT2))
        fsm.event2((++msgNo).toString())
        assertEquals(2, context.state)
        fsm.event3((++msgNo).toString())
        assertEquals(3, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT2))
        fsm.event2((++msgNo).toString())
        assertEquals(3, context.state)
        fsm.event1((++msgNo).toString())
        assertEquals(1, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT1))
    }
}
