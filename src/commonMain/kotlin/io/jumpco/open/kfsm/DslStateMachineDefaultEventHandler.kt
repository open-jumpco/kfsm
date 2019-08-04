/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import io.jumpco.open.kfsm.EventState
import io.jumpco.open.kfsm.DefaultChangeAction
import io.jumpco.open.kfsm.DefaultStateAction
import io.jumpco.open.kfsm.StateAction
import io.jumpco.open.kfsm.StateGuard

/**
 * This handler will be active inside the default section of the statemachine.
 */
class DslStateMachineDefaultEventHandler<S : Enum<S>, E : Enum<E>, C>(private val fsm: StateMachineBuilder<S, E, C>) {
    /**
     * Define a default action that will be applied when no other transitions are matched.
     * @param action Will be invoked when no transitions matches
     */
    fun action(action: DefaultStateAction<C, S, E>) {
        fsm.defaultAction(action)
    }

    /**
     * Defines an action to perform before a change in the currentState of the FSM
     * @param action This action will be performed when entering a new state.
     */
    fun entry(action: DefaultChangeAction<C, S>) {
        fsm.defaultEntry(action)
    }

    /**
     * Defines an action to be performed after the currentState was changed.
     * @param action The action will be performed when leaving any state.
     */
    fun exit(action: DefaultChangeAction<C, S>) {
        fsm.defaultExit(action)
    }

    /**
     * Defines a default transition when an on is received to a specific state.
     * @param event Pair representing an on and targetState for transition. Can be written as EVENT to STATE
     * @param action The action will be performed before transition is completed
     */
    fun transition(event: EventState<E, S>, action: StateAction<C>?): DslStateMachineDefaultEventHandler<S, E, C> {
        fsm.default(event, action)
        return this
    }

    /**
     * Defines a default internal transition for a specific event with no change in state.
     * @param event The event that triggers this transition
     * @param action The action will be invoked for this transition
     */
    fun transition(event: E, action: StateAction<C>?): DslStateMachineDefaultEventHandler<S, E, C> {
        fsm.default(event, action)
        return this
    }
}
