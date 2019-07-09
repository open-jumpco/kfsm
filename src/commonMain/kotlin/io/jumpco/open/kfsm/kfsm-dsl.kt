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
 * @suppress
 */
class DslStateMachineEventHandler<S : Enum<S>, E : Enum<E>, C>(
    private val currentState: S,
    private val fsm: StateMachine<S, E, C>
) {
    /**
     * Defines a default action when no other transition are matched
     * @param action The action will be performed
     */
    fun default(action: (C, S, E) -> Unit) {
        fsm.default(currentState, action)
    }

    /**
     * Defines an action to be performed before transition causes a change in state
     * @param action The action will be performed
     */
    fun entry(action: (C, S, S) -> Unit) {
        fsm.entry(currentState, action)
    }

    /**
     * Defines an action to be performed after a transition has changed the state
     */
    fun exit(action: (C, S, S) -> Unit) {
        fsm.exit(currentState, action)
    }

    /**
     * Defines a transition when the state is the currentState and the on is received. The state is changed to the endState.
     * @param event A Pair with the first being the on and the second being the endState.
     * @param action The action will be performed
     */
    fun on(event: Pair<E, S>, action: ((C) -> Unit)?): DslStateMachineEventHandler<S, E, C> {
        fsm.transition(currentState, event.first, event.second, action)
        return this
    }

    /**
     * Defines a guarded transition. Where the transition will only be used if the guared expression is met
     */
    fun on(event: Pair<E, S>, guard: ((C) -> Boolean), action: ((C) -> Unit)?): DslStateMachineEventHandler<S, E, C> {
        fsm.transition(currentState, event.first, event.second, guard, action)
        return this
    }

    /**
     * Defines a transition where an on causes an action but doesn't change the state.
     */
    fun on(event: E, action: ((C) -> Unit)?): DslStateMachineEventHandler<S, E, C> {
        fsm.transition(currentState, event, action)
        return this
    }

    /**
     * Defines a guarded transition where an on causes an action but doesn't change the state and will only be used
     * if the guard expression is met
     */
    fun on(event: E, guard: ((C) -> Boolean), action: ((C) -> Unit)?): DslStateMachineEventHandler<S, E, C> {
        fsm.transition(currentState, event, guard, action)
        return this
    }

}

/**
 * @suppress
 */
class DslStateMachineDefaultEventHandler<S : Enum<S>, E : Enum<E>, C>(private val fsm: StateMachine<S, E, C>) {
    /**
     * Define a default action that will be applied when no other transitions are matched.
     */
    fun action(action: (C, S, E) -> Unit) {
        fsm.defaultAction(action)
    }

    /**
     * Defines an action to perform before a change in the currentState of the FSM
     * @param action This action will be performed
     */
    fun entry(action: (C, S, S) -> Unit) {
        fsm.defaultEntry(action)
    }

    /**
     * Defines an action to be performed after the currentState was changed.
     * @param action The action will be performed
     */
    fun exit(action: (C, S, S) -> Unit) {
        fsm.defaultExit(action)
    }

    /**
     * Defines a default transition when an on is received to a specific state.
     * @param event Pair representing an on and endState for transition. Can be written as EVENT to STATE
     * @param action The action will be performed before transition is completed
     */
    fun on(event: Pair<E, S>, action: ((C) -> Unit)?): DslStateMachineDefaultEventHandler<S, E, C> {
        fsm.default(event, action)
        return this
    }

    fun on(event: E, action: ((C) -> Unit)?): DslStateMachineDefaultEventHandler<S, E, C> {
        fsm.default(event, action)
        return this
    }
}

/**
 * @suppress
 */
class DslStateMachineHandler<S : Enum<S>, E : Enum<E>, C>(private val fsm: StateMachine<S, E, C>) {

    fun initial(deriveInitialState: (C) -> S): DslStateMachineHandler<S, E, C> {
        fsm.initial(deriveInitialState)
        return this
    }

    fun state(currentState: S, handler: DslStateMachineEventHandler<S, E, C>.() -> Unit):
            DslStateMachineEventHandler<S, E, C> {
        return DslStateMachineEventHandler(currentState, fsm).apply(handler)
    }

    fun default(handler: DslStateMachineDefaultEventHandler<S, E, C>.() -> Unit):
            DslStateMachineDefaultEventHandler<S, E, C> {
        return DslStateMachineDefaultEventHandler(fsm).apply(handler)
    }

    fun build() = fsm
}
