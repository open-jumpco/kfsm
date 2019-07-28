/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import kotlin.reflect.KClass
import kotlin.Pair

/**
 * This represents an action that may be invoked during a transition.
 * @param C The context: C will be available to the lambda.
 */
typealias StateAction<C> = C.(Array<out Any>) -> Unit

/**
 * This represents a guard expression that may be used to select a specific transition.
 * @param C The context: C will be available to the lambda
 */
typealias StateGuard<C> = C.(Array<out Any>) -> Boolean

/**
 * This represents an expression to determine the state of the context. This is useful when creating an instance of a
 * statemachine multiple times during the lifecycle of a context represented by a persisted or serializable entity.
 * @param C The context:C will be available to the lambda.
 * @param S The type of the state that should be returned by the lambda.
 */
typealias StateQuery<C, S> = (C.() -> S)

/**
 * This represents a default action that will be invoked on a change in state as defined with entry or exit.
 * @param C The context: C will be available to the lambda
 * @param S currentState: S and targetState: S will be available to the lambda.
 */
typealias DefaultChangeAction<C, S> = C.(S, S, Array<out Any>) -> Unit

/**
 * This represents a default action for a specific event. These action will not cause changes in state.
 * They are internal transitions.
 * @param C The context: C will be available to the lambda
 * @param S The currentState:S will be available to the lambda
 * @param E The event: E will be available to the lambda
 */
typealias DefaultStateAction<C, S, E> = C.(S, E, Array<out Any>) -> Unit


/**
 * This represents an event and targetState pair that can be written as `event to state`
 * @param E The event: E will be the first element of the pair
 * @param S The targetState: S will be the second element of the pair.
 */
typealias EventState<E, S> = Pair<E, S>


/**
 * Defines the start of a state machine DSL declaration
 * @param stateClass The class of the possible states
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @sample io.jumpco.open.kfsm.TurnstileFSM.define
 */
inline fun <S : Enum<S>, E : Enum<E>, C: Any> stateMachine(
    stateClass: KClass<S>,
    eventClass: KClass<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C>.() -> Unit
) = StateMachineBuilder<S, E, C>().stateMachine(handler)
