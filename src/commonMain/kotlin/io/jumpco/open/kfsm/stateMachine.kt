/*
 * Copyright (c) 2021. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import kotlin.reflect.KClass

/**
 * Defines the start of a state machine DSL declaration
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param validStates A set of the possible states supported by the top-level state map
 * @param validEvents The class of the possible events
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
 * @param returnClass The class of the return type of events/actions
 * @param handler Statemachine handler
 * @sample io.jumpco.open.kfsm.TurnstileFSM.definition
 */
inline fun <S, E, C : Any, A : Any, R : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    returnClass: KClass<out R>,
    handler: DslStateMachineHandler<S, E, C, A, R>.() -> Unit
) = StateMachineBuilder<S, E, C, A, R>(validStates, validEvents).stateMachine(handler)

/**
 * Defines the start of a state machine DSL declaration with `Any` as the return type
 * @param validStates A set of the possible states supported by the top-level state map
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
 * @sample io.jumpco.open.kfsm.TurnstileFSM.definition
 */
inline fun <S, E, C : Any, A : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    handler: DslStateMachineHandler<S, E, C, A, Any>.() -> Unit
) = stateMachine<S, E, C, A, Any>(
    validStates,
    validEvents,
    contextClass,
    argumentClass,
    Any::class,
    handler
)

/**
 * Defines the start of a state machine DSL declaration with `Any` as the type of arguments and returns types for events/actions
 * @param validStates A set of the possible states supported by the top-level state map
 * @param validEvents The class of the possible events
 * @param contextClass The class of the context
 * @param handler The DSL handler
 * @sample io.jumpco.open.kfsm.TurnstileFSM.definition
 */
inline fun <S, E, C : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, Any, Any>.() -> Unit
) = stateMachine<S, E, C, Any, Any>(
    validStates,
    validEvents,
    contextClass,
    Any::class,
    Any::class,
    handler
)

inline fun <S, E, C : Any> functionalStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, C, C>.() -> Unit
) = StateMachineBuilder<S, E, C, C, C>(validStates, validEvents).stateMachine(handler)
