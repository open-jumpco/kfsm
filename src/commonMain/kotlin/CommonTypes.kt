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

/**
 * This represents an action that may be invoked during a transition.
 * @param C The context: C will be available to the lambda.
 */
typealias StateAction<C, A, R> = C.(A?) -> R?

/**
 * This represents a guard expression that may be used to select a specific transition.
 * @param C The context: C will be available to the lambda
 */
typealias StateGuard<C, A> = C.(A?) -> Boolean

/**
 * This represents an expression to determine the state of the context. This is useful when creating an instance of a
 * statemachine multiple times during the lifecycle of a context represented by a persisted or serializable entity.
 * @param C The context:C will be available to the lambda.
 * @param S The type of the state that should be returned by the lambda.
 */
typealias StateQuery<C, S> = (C.() -> S)

/**
 * Alias for a pair of S, String
 */
typealias StateMapItem<S> = Pair<S, String>

/**
 * Alias for a list of pairs of S,String
 */
typealias StateMapList<S> = List<StateMapItem<S>>

/**
 * Alias for a lambda that will have the context as this and return a `StateMapList`
 */
typealias StateMapQuery<C, S> = (C.() -> StateMapList<S>)

/**
 * This represents a default action that will be invoked when entering or exiting a state with an external state transitions.
 * @param C The context: C will be available to the lambda
 * @param S currentState: S and targetState: S will be available to the lambda as 1st and 2nd arguments.
 * @param A argumentType: A an arg: will be available to the lambda as the 3rd argument.
 */
typealias DefaultEntryExitAction<C, S, A> = C.(S, S, A?) -> Unit

/**
 * This represents a default action for a specific event. These action will not cause changes in state.
 * They are internal transitions.
 * @param C The context: C will be available to the lambda
 * @param S The currentState:S will be available to the lambda
 * @param E The event: E will be available to the lambda
 * @param A argumentType: A an arg: will be available to the lambda as the 3rd argument.
 */
typealias DefaultStateAction<C, S, E, A, R> = C.(S, E, A?) -> R?

/**
 * This represents an event and targetState pair that can be written as `event to state`
 * @param E The event: E will be the first element of the pair
 * @param S The targetState: S will be the second element of the pair.
 */
typealias EventState<E, S> = Pair<E, S>

/**
 * Represents the state of one or more pushed state maps and can be stored and used to initialise the state machine.
 */
typealias ExternalState<S> = StateMapList<S>

/**
 * Represents the different kinds of transitions.
 */
enum class TransitionType {
    /**
     * Transitions are triggered and may change to a new state or remain at the same state while performing an action.
     */
    NORMAL,
    /**
     * A push transition will place the current state map on a stack and make the named statemap the current map and change to the given state,
     */
    PUSH,
    /**
     * A pop transition will pop the stack and make the transition current. If the pop transition provided a new targetMap or targetState that will result in push or normal transition behaviour.
     */
    POP
}

/**
 * Defines the start of a state machine DSL declaration
 * @param validStates A set of the possible states supported by the top-level state map
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
 * @param returnClass The class of the return type of events/actions
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
) = stateMachine<S, E, C, A, Any>(validStates, validEvents, contextClass, argumentClass, Any::class, handler)

/**
 * Defines the start of a state machine DSL declaration with `Any` as the type of arguments and returns types for events/actions
 * @param validStates A set of the possible states supported by the top-level state map
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @sample io.jumpco.open.kfsm.TurnstileFSM.definition
 */
inline fun <S, E, C : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, Any, Any>.() -> Unit
) = stateMachine<S, E, C, Any, Any>(validStates, validEvents, contextClass, Any::class, Any::class, handler)

inline fun <S, E, C : Any> functionalStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, C, C>.() -> Unit
) = StateMachineBuilder<S, E, C, C, C>(validStates, validEvents).stateMachine(handler)

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

/**
 * An extension function that evaluates the expression and invokes the provided `block` if true.
 */
inline fun <T> T.ifApply(expression: Boolean, block: T.() -> Unit): T {
    if (expression) {
        this.apply(block)
    }
    return this
}

/**
 * This class implements a simple generic stack.
 */
class Stack<T> {
    private val elements: MutableList<T> = mutableListOf()
    /**
     * Push an element onto the stack.
     * @param value The element will be pushed
     */
    fun push(value: T) = elements.add(value)

    /**
     * Returns and removed the element on the top of the stack.
     * @return The element on the top of the stack
     * @throws Will throw IllegalStateException is the stack is empty
     */
    fun pop(): T {
        check(elements.isNotEmpty()) { "stack is empty" }
        return elements.removeAt(elements.lastIndex)
    }

    /**
     * Indicates that the stack is empty.
     * @return true if the stack is empty
     */
    fun isEmpty() = elements.isEmpty()

    /**
     * Indicates that the stack is not empty.
     * @return true if the stack is not empty
     */
    fun isNotEmpty() = elements.isNotEmpty()

    /**
     * Provides access to the top of the stack.
     * @return the element at the top of the stack without removing or `null` is the stack is empty.
     */
    fun peek(): T? = elements.lastOrNull()

    /**
     * Provides access to all the entries on the stack.
     * @return An immutable list of entries with the top of the stack at the end.
     */
    fun peekContent(): List<T> = elements.toList()
}
