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
