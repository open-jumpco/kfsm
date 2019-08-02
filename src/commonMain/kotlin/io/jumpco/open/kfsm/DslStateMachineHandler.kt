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
 * This handler will be active inside the top level of the stateMachine definition.
 */
class DslStateMachineHandler<S : Enum<S>, E : Enum<E>, C>(private val fsm: StateMachineBuilder<S, E, C>) {
    /**
     * Defines an expression that will determine the initial state of the state machine based on the values of the context.
     * @param deriveInitialState A lambda expression receiving context:C and returning state S.
     */
    fun initial(deriveInitialState: StateQuery<C, S>): DslStateMachineHandler<S, E, C> {
        fsm.initial(deriveInitialState)
        return this
    }

    /**
     * Defines a section for a specific state.
     * @param currentState The give state
     * @param handler A lambda with definitions for the given state
     */
    fun state(currentState: S, handler: DslStateMachineEventHandler<S, E, C>.() -> Unit):
            DslStateMachineEventHandler<S, E, C> =
        DslStateMachineEventHandler(currentState, fsm).apply(handler)

    /**
     * Defines a section for default behaviour for the state machine.
     * @param handler A lambda with definition for the default behaviour of the state machine.
     */
    fun default(handler: DslStateMachineDefaultEventHandler<S, E, C>.() -> Unit):
            DslStateMachineDefaultEventHandler<S, E, C> =
        DslStateMachineDefaultEventHandler(fsm).apply(handler)

    /**
     * Returns the completed fsm.
     */
    fun build() = fsm.complete()
}
