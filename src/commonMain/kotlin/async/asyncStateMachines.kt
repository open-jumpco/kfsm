/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
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
 * @param validEvents The class of the possible events*
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
 * @param handler The state machine handler
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
 * @param validEvents The class of the possible events
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
