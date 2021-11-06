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

package io.jumpco.open.kfsm

/**
 * This handler will be active inside the default section of the statemachine.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class DslStateMapDefaultEventHandler<S, E, C, A, R>(private val fsm: StateMapBuilder<S, E, C, A, R>) {
    /**
     * Define a default action that will be applied when no other transitions are matched.
     * @param action Will be invoked when no transitions matches
     */
    fun action(action: DefaultStateAction<C, S, E, A, R>) {
        fsm.defaultAction(action)
    }

    /**
     * Defines an action to perform before a change in the currentState of the FSM
     * @param action This action will be performed when entering a new state.
     */
    fun onEntry(action: DefaultEntryExitAction<C, S, A>) {
        fsm.defaultEntry(action)
    }

    /**
     * Defines an action to be performed after the currentState was changed.
     * @param action The action will be performed when leaving any state.
     */
    fun onExit(action: DefaultEntryExitAction<C, S, A>) {
        fsm.defaultExit(action)
    }

    /**
     * Defines a default transition when an on is received to a specific state.
     * @param event Pair representing an on and targetState for transition. Can be written as EVENT to STATE
     * @param action The action will be performed before transition is completed
     */
    fun onEvent(event: EventState<E, S>, action: SyncStateAction<C, A, R>?): DslStateMapDefaultEventHandler<S, E, C, A, R> {
        fsm.default(event, action)
        return this
    }

    /**
     * Defines a default internal transition for a specific event with no change in state.
     * @param event The event that triggers this transition
     * @param action The action will be invoked for this transition
     */
    fun onEvent(event: E, action: SyncStateAction<C, A, R>?): DslStateMapDefaultEventHandler<S, E, C, A, R> {
        fsm.default(event, action)
        return this
    }
}
