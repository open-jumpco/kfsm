/*
 * Copyright (c) 2020-2021. Open JumpCO
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jumpco.open.kfsm.viz

import io.jumpco.open.kfsm.TransitionType
import io.jumpco.open.kfsm.async.AsyncGuardedTransition
import io.jumpco.open.kfsm.async.AsyncStateMachineDefinition
import io.jumpco.open.kfsm.async.AsyncStateMapDefinition
import io.jumpco.open.kfsm.async.AsyncTransition
import io.jumpco.open.kfsm.async.AsyncTransitionRules
import io.jumpco.open.kfsm.async.DefaultAsyncTransition
import io.jumpco.open.kfsm.async.SimpleAsyncTransition

/**
 * The visualization provided by these methods will not be able to provide any detail about guard expressions or actions.
 * This visualization only provides states and transitions.
 * The separate kfsm-viz module and associated Gradle plugin will provide visualization after parsing the definition DSL.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */

fun <S, E, C, A, R> visualize(
    definition: AsyncStateMachineDefinition<S, E, C, A, R>
): Iterable<TransitionView> {
    val output = mutableListOf<TransitionView>()
    if (definition.defaultInitialState != null) {
        output.add(TransitionView("default", "<<start>>", null, definition.defaultInitialState.toString()))
    }
    val stateMap = definition.defaultStateMap
    makeStateMap(definition, "default", stateMap, output)
    definition.namedStateMaps.entries.forEach { mapEntry ->
        makeStateMap(definition, mapEntry.key, mapEntry.value, output)
    }
    return output.distinct()
}

private fun <A, C, E, R, S> makeStateMap(
    definition: AsyncStateMachineDefinition<S, E, C, A, R>,
    stateMapName: String,
    stateMap: AsyncStateMapDefinition<S, E, C, A, R>,
    output: MutableList<TransitionView>
) {
    stateMap.transitionRules.forEach { entry ->
        makeView(definition, stateMapName, entry.key.first.toString(), entry.key.second, entry.value, output)
    }
    stateMap.transitionRules.entries.forEach { entry ->
        entry.value.guardedTransitions.forEach { transition ->
            makeView(definition, stateMapName, entry.key.first.toString(), entry.key.second, transition, output)
        }
    }
    stateMap.defaultTransitions.forEach { entry ->
        makeView(definition, stateMapName, null, entry.key, entry.value, output)
    }
    stateMap.automaticTransitions.forEach { entry ->
        makeView(definition, stateMapName, entry.key.toString(), null, entry.value, output)
    }
    stateMap.automaticTransitions.forEach { entry ->
        makeView(definition, stateMapName, entry.key.toString(), null, entry.value, output)
        entry.value.guardedTransitions.forEach { transition ->
            makeView(definition, stateMapName, entry.key.toString(), null, transition, output)
        }
    }
}

fun <S, E, C, A, R> makeView(
    definition: AsyncStateMachineDefinition<S, E, C, A, R>,
    mapName: String,
    from: String?,
    event: E?,
    transition: AsyncTransition<S, E, C, A, R>?,
    output: MutableList<TransitionView>
) {
    val mapDefinition = definition.namedStateMaps[mapName]
    val targetMap = transition?.targetMap
    val targetState = transition?.targetState
    // Ensure that target states are declared within their own map or default map not in a foreign named map.
    val sourceMap = if (mapDefinition != null && targetState != null) {
        if (mapDefinition.validStates.contains(targetState)) mapName else targetMap ?: "default"
    } else {
        mapName
    }
    val result = when (transition) {
        is AsyncGuardedTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            from.toString(),
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            transition.type,
            transition.guard.toString()
        )
        is SimpleAsyncTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            from.toString(),
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            transition.type
        )
        is DefaultAsyncTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            null,
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            TransitionType.DEFAULT
        )
        is AsyncTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            null,
            null,
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            transition.type
        )
        null -> TransitionView(sourceMap, from.toString(), event?.toString())
        else -> error("Unknown Transition Type:$transition")
    }
    output.add(result)
}

fun <S, E, C, A, R> makeView(
    definition: AsyncStateMachineDefinition<S, E, C, A, R>,
    mapName: String,
    from: String?,
    event: E?,
    rules: AsyncTransitionRules<S, E, C, A, R>?,
    output: MutableList<TransitionView>
) {
    makeView(definition, mapName, from, event, rules?.transition, output)
}
