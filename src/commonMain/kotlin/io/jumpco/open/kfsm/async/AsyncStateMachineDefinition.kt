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

import io.jumpco.open.kfsm.ExternalState
import io.jumpco.open.kfsm.StateMapQuery
import io.jumpco.open.kfsm.StateQuery
import kotlinx.coroutines.CoroutineScope

class AsyncStateMachineDefinition<S, E, C, A, R>(
    val defaultInitialState: S?,
    private val deriveInitialState: StateQuery<C, S>?,
    private val deriveInitialMap: StateMapQuery<C, S>?,
    /**
     * The top level state map will be created when the state machine is created.
     */
    val defaultStateMap: AsyncStateMapDefinition<S, E, C, A, R>,
    /**
     * The named state maps can be accessed via a push transition.
     */
    val namedStateMaps: Map<String, AsyncStateMapDefinition<S, E, C, A, R>>

) {
    private fun createMap(
        mapName: String,
        context: C,
        parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
        initial: S,
        coroutineScope: CoroutineScope
    ) {
        if (mapName == "default") {
            parentFsm.pushMap(
                AsyncStateMapInstance(
                    context,
                    initial,
                    null,
                    parentFsm,
                    defaultStateMap,
                    coroutineScope
                )
            )
        } else {
            val stateMap = namedStateMaps[mapName] ?: error("Invalid map $mapName")
            parentFsm.pushMap(
                AsyncStateMapInstance(
                    context,
                    initial,
                    mapName,
                    parentFsm,
                    stateMap,
                    coroutineScope
                )
            )
        }
    }

    /**
     * This function will create a state machine instance provided with content and optional initialState.
     * @param context The context will be provided to actions
     * @param initialState If this is not provided the function defined in `initial` will be invoked to derive the initialState.
     */
    internal fun create(
        context: C,
        parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
        coroutineScope: CoroutineScope,
        initialState: S? = null,
        intitialExternalState: ExternalState<S>? = null
    ): AsyncStateMapInstance<S, E, C, A, R> {
        when {
            intitialExternalState != null -> {
                intitialExternalState.forEach { (initial, mapName) ->
                    createMap(mapName, context, parentFsm, initial, coroutineScope)
                }
                return parentFsm.mapStack.pop()
            }

            deriveInitialMap != null -> {
                deriveInitialMap.invoke(context).forEach { (initial, mapName) ->
                    createMap(mapName, context, parentFsm, initial, coroutineScope)
                }
                return parentFsm.mapStack.pop()
            }

            else -> {
                val initial = initialState ?: deriveInitialState?.invoke(context) ?: defaultInitialState
                ?: error("Definition requires deriveInitialState or deriveInitialMap")
                return AsyncStateMapInstance(
                    context,
                    initial,
                    null,
                    parentFsm,
                    defaultStateMap,
                    coroutineScope
                )
            }
        }
    }

    internal fun createStateMap(
        name: String,
        context: C,
        parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
        initialState: S,
        coroutineScope: CoroutineScope
    ): AsyncStateMapInstance<S, E, C, A, R> =
        AsyncStateMapInstance(
            context,
            initialState,
            name,
            parentFsm,
            namedStateMaps[name] ?: error("Named map $name not found"),
            coroutineScope
        )

    /**
     * This function will create a state machine instance and set it to the state to a previously externalised state.
     * @param context The instance will operate on the provided context
     * @param initialExternalState The previously externalised state
     */
    fun create(
        context: C,
        coroutineScope: CoroutineScope,
        initialExternalState: ExternalState<S>,
    ): AsyncStateMachineInstance<S, E, C, A, R> =
        AsyncStateMachineInstance<S, E, C, A, R>(
            context,
            this,
            coroutineScope,
            null,
            initialExternalState
        )

    /**
     * This function will create a state machine instance and set it to the initial state.
     * @param context The instance will operate on the provided context
     * @param initial The initial state
     *
     */
    fun create(
        context: C,
        coroutineScope: CoroutineScope,
        initial: S? = null
    ): AsyncStateMachineInstance<S, E, C, A, R> =
        AsyncStateMachineInstance<S, E, C, A, R>(context, this, coroutineScope, initial)

    /**
     * This function will provide a list of possible events given a specific state.
     * The actual events may fail because of guard conditions or named state maps and the default state map behaviour being different.
     * @param state The given state
     * @param includeDefault consider the default state and event handlers
     */
    fun possibleEvents(state: S, includeDefault: Boolean): Set<E> {
        val result = mutableSetOf<E>()
        result.addAll(defaultStateMap.allowed(state, includeDefault))
        namedStateMaps.values.forEach {
            if (it.validStates.contains(state)) {
                result.addAll(it.allowed(state, includeDefault))
            }
        }
        return result.toSet()
    }
}
