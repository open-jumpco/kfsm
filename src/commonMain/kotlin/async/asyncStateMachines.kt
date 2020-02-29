/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.async

import kotlin.reflect.KClass

/**
 * These function are use to create statemachines where the actions are suspend functions.
 */
inline fun <S, E, C : Any, A : Any, R : Any> asyncStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    returnClass: KClass<out R>,
    handler: AsyncDslStateMachineHandler<S, E, C, A, R>.() -> Unit
) = AsyncStateMachineBuilder<S, E, C, A, R>(validStates, validEvents).stateMachine(handler)

/**
 * Defines the start of a state machine DSL declaration with `Any` as the return type
 * @param validStates A set of the possible states supported by the top-level state map
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
 * @sample io.jumpco.open.kfsm.TurnstileFSM.definition
 */
inline fun <S, E, C : Any, A : Any> asyncStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    handler: AsyncDslStateMachineHandler<S, E, C, A, Any>.() -> Unit
) = asyncStateMachine<S, E, C, A, Any>(validStates, validEvents, contextClass, argumentClass, Any::class, handler)

/**
 * Defines the start of a state machine DSL declaration with `Any` as the type of arguments and returns types for events/actions
 * @param validStates A set of the possible states supported by the top-level state map
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @sample io.jumpco.open.kfsm.TurnstileFSM.definition
 */
inline fun <S, E, C : Any> asyncStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: AsyncDslStateMachineHandler<S, E, C, Any, Any>.() -> Unit
) = asyncStateMachine<S, E, C, Any, Any>(validStates, validEvents, contextClass, Any::class, Any::class, handler)

inline fun <S, E, C : Any> asyncFunctionalStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: AsyncDslStateMachineHandler<S, E, C, C, C>.() -> Unit
) = AsyncStateMachineBuilder<S, E, C, C, C>(validStates, validEvents).stateMachine(handler)

/**
 * An extension function that evaluates the expression and invokes the provided `block` if true or the `otherwise` block is false.
 */
inline fun <T> T.ifApply(expression: Boolean, block: T.() -> Unit, otherwise: T.() -> Unit): T {
    return if (expression) {
        this.apply(block)
    } else {
        this.apply(otherwise)
    }
}
