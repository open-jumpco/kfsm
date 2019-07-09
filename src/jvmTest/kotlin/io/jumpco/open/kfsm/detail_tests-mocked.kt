/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import io.mockk.*
import kotlin.test.Test


class DetailMockedTests {
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

    class TestDetailFSM(private val context: TestContext) {
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
                    entry { context, startState, endState ->
                        println("entering:to $endState from $startState for:$context")
                        context.defaultEntry()
                    }
                    exit { context, startState, endState ->
                        println("exiting:from $endState to $startState for:$context")
                        context.defaultExit()
                    }
                    on(TestEvents.EVENT1 to TestStates.STATE1) { context ->
                        println("default:EVENT1 to STATE1 for $context")
                        context.action1()

                    }
                    on(TestEvents.EVENT2 to TestStates.STATE2) { context ->
                        println("default:on EVENT2 to STATE2 for $context")
                        context.action2()
                    }
                    on(TestEvents.EVENT3 to TestStates.STATE3) { context ->
                        println("default:on EVENT3 to STATE3 for $context")
                        context.defaultAction()
                    }
                    action { context, currentState, event ->
                        println("default:$event from $currentState for $context")
                        context.defaultAction()
                    }
                }
                state(TestStates.STATE1) {
                    on(TestEvents.EVENT1) { context ->
                        context.action1()
                    }
                    entry { context, _, _ ->
                        context.entry1()
                    }

                }
                state(TestStates.STATE2) {
                    entry { context, _, _ ->
                        context.entry2()
                    }
                    on(TestEvents.EVENT2, guard = { it.state == 2 }) { context ->
                        println("EVENT2:guarded:from STATE2 for $context")
                        context.action2()
                    }
                    exit { context, _, _ ->
                        context.exit2()
                    }
                }
                state(TestStates.STATE3) {
                    exit { context, _, _ ->
                        context.exit3()
                    }
                    on(TestEvents.EVENT2, guard = { it.state == 2 }) {
                        error("should never be called")
                    }
                }
            }.build()

            private val definition by lazy { define() }
        }

        private val fsm = definition.create(context)
        fun event1() {
            println("--event1")
            fsm.sendEvent(TestEvents.EVENT1)
        }

        fun event2() {
            println("--event2")
            fsm.sendEvent(TestEvents.EVENT2)
        }

        fun event3() {
            println("--event3")
            fsm.sendEvent(TestEvents.EVENT3)
        }
    }
    @Test
    fun `test actions mockked`() {
        // given
        val context = mockk<TestContext>()
        every { context.toString() } returns "MockedTestContext"
        every { context.state } returns 3
        every { context.action1() } just Runs
        every { context.action2() } just Runs
        every { context.defaultAction() } just Runs
        every { context.defaultEntry() } just Runs
        every { context.defaultExit() } just Runs
        every { context.entry1() } just Runs
        every { context.entry2() } just Runs
        every { context.exit2() } just Runs
        every { context.exit3() } just Runs

        val fsm = TestDetailFSM(context)
        // when
        fsm.event1()
        every { context.state } returns 1
        fsm.event2()
        every { context.state } returns 2
        fsm.event3()
        every { context.state } returns 3
        fsm.event2()

        // then
        verify {
            // event1
            context.toString()
            context.exit3()
            context.toString()
            context.defaultExit()
            context.action1()
            context.toString()
            context.entry1()
            context.toString()
            context.defaultEntry()
            // event2
            context.defaultExit()
            context.action2()
            context.entry2()
            context.toString()
            context.defaultEntry()
            // event3
            context.defaultAction()
            // event2
        }
    }
}
