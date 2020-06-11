/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
