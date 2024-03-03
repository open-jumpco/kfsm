/*
    Copyright 2019-2024 Open JumpCO

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

import io.jumpco.open.kfsm.ExternalState
import io.jumpco.open.kfsm.Stack
import io.jumpco.open.kfsm.TransitionType
import kotlinx.coroutines.CoroutineScope

class AsyncStateMachineInstance<S, E, C, A, R>(
    /**
     * The transition actions are performed by manipulating the context.
     */
    private val context: C,
    /**
     * The Immutable definition of the state machine.
     */
    val definition: AsyncStateMachineDefinition<S, E, C, A, R>,
    /**
     * The initialState will be assigned to the currentState
     */
    val coroutineScope: CoroutineScope,
    initialState: S? = null,
    initialExternalState: ExternalState<S>? = null,
) {
    internal val namedInstances: MutableMap<String, AsyncStateMapInstance<S, E, C, A, R>> = mutableMapOf()

    internal val mapStack: Stack<AsyncStateMapInstance<S, E, C, A, R>> =
        Stack()

    /**
     * This represents the current state of the state machine.
     * It will be modified during transitions
     */
    internal var currentStateMap: AsyncStateMapInstance<S, E, C, A, R>

    val currentState: S
        get() = currentStateMap.currentState

    /**
     * Create a state machine using a specific definition and a previous externalised state.
     * @param context The instane will operate on the context
     * @param definition The definition of the state machine instance.
     * @param initialExternalState The previously externalised state.
     */
    constructor(
        context: C,
        definition: AsyncStateMachineDefinition<S, E, C, A, R>,
        initialExternalState: ExternalState<S>,
        coroutineScope: CoroutineScope
    ) :
        this(context, definition, coroutineScope, null, initialExternalState)

    init {
        currentStateMap = definition.create(context, this, coroutineScope, initialState, initialExternalState)
    }

    internal fun pushMap(
        defaultInstance: AsyncStateMapInstance<S, E, C, A, R>,
        initial: S,
        name: String,
        stateMap: AsyncStateMapDefinition<S, E, C, A, R>
    ): AsyncStateMapInstance<S, E, C, A, R> {
        mapStack.push(defaultInstance)
        val pushedMap = AsyncStateMapInstance(context, initial, name, this, stateMap, coroutineScope)
        currentStateMap = pushedMap
        return pushedMap
    }

    internal fun pushMap(stateMap: AsyncStateMapInstance<S, E, C, A, R>): AsyncStateMapInstance<S, E, C, A, R> {
        mapStack.push(stateMap)
        return stateMap
    }

    internal suspend fun execute(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
        val result = if (transition.type == TransitionType.PUSH) {
            require(transition.targetState != null) { "Target state cannot be null for pushTransition" }
            require(transition.targetMap != null) { "Target map cannot be null for pushTransition" }
            if (transition.targetMap != currentStateMap.name) {
                executePush(transition, arg)
            } else {
                currentStateMap.execute(transition, arg)
            }
        } else if (transition.type == TransitionType.POP) {
            executePop(transition, arg)
        } else {
            currentStateMap.execute(transition, arg)
        }
        currentStateMap.executeAutomatic(transition, currentStateMap.currentState, arg)
        return result
    }

    private suspend fun executePop(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
        val sourceMap = currentStateMap
        val targetMap = mapStack.pop()
        currentStateMap = targetMap
        return if (transition.targetMap == null) {
            if (transition.targetState == null) {
                transition.execute(context, sourceMap, targetMap, arg)
            } else {
                val interim = transition.execute(context, sourceMap, null, arg)
                targetMap.executeEntry(context, transition.targetState, arg)
                currentStateMap.changeState(transition.targetState)
                interim
            }
        } else {
            val interim = executePush(transition, arg)
            if (transition.targetState != null) {
                currentStateMap.changeState(transition.targetState)
            }
            interim
        }
    }

    private suspend fun executePush(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
        val targetStateMap = namedInstances.getOrElse(transition.targetMap!!) {
            definition.createStateMap(transition.targetMap, context, this, transition.targetState!!, coroutineScope)
                .apply {
                    namedInstances[transition.targetMap] = this
                }
        }
        mapStack.push(currentStateMap)
        val result = transition.execute(context, currentStateMap, targetStateMap, arg)
        currentStateMap = targetStateMap
        if (transition.targetState != null) {
            currentStateMap.changeState(transition.targetState)
        }
        return result
    }

    /**
     * This function will provide the set of allowed events given a specific state. It isn't a guarantee that a
     * subsequent transition will be successful since a guard may prevent a transition. Default state handlers are not considered.
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
     */
    fun allowed(includeDefault: Boolean = false): Set<E> = currentStateMap.allowed(includeDefault)

    /**
     * This function will return an indicator if the given event is allowed given the current state.
     * @param event The given event.
     * @param includeDefault Will consider defined default event handlers.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        currentStateMap.eventAllowed(event, includeDefault)

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    suspend fun sendEvent(event: E, arg: A? = null) = currentStateMap.sendEvent(event, arg)

    /**
     * This function will provide the external state that can be use when creating the instance at a later time.
     * @return The external state a collection of state and map name pairs.
     */
    fun externalState(): ExternalState<S> {
        return mapStack.peekContent()
            .map { Pair(it.currentState, it.name ?: "default") }
            .toMutableList()
            .apply {
                add(Pair(currentStateMap.currentState, currentStateMap.name ?: "default"))
            }.toList()
    }
}

typealias AsyncAnyStateMachineInstance<S, E, C> = AsyncStateMachineInstance<S, E, C, Any, Any>
