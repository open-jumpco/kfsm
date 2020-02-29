/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.viz

import io.jumpco.open.kfsm.DefaultSyncTransition
import io.jumpco.open.kfsm.SimpleSyncTransition
import io.jumpco.open.kfsm.StateMachineDefinition
import io.jumpco.open.kfsm.StateMapDefinition
import io.jumpco.open.kfsm.SyncGuardedTransition
import io.jumpco.open.kfsm.SyncTransition
import io.jumpco.open.kfsm.SyncTransitionRules
import io.jumpco.open.kfsm.TransitionType

public fun <S, E, C, A, R> visualize(
    definition: StateMachineDefinition<S, E, C, A, R>
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
    definition: StateMachineDefinition<S, E, C, A, R>,
    stateMapName: String,
    stateMap: StateMapDefinition<S, E, C, A, R>,
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
    definition: StateMachineDefinition<S, E, C, A, R>,
    mapName: String,
    from: String?,
    event: E?,
    transition: SyncTransition<S, E, C, A, R>?,
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
        is SyncGuardedTransition<S, E, C, A, R> -> TransitionView(
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
        is SimpleSyncTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            from.toString(),
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            transition.type
        )
        is DefaultSyncTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            null,
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            TransitionType.DEFAULT
        )
        is SyncTransition<S, E, C, A, R> -> TransitionView(
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
    definition: StateMachineDefinition<S, E, C, A, R>,
    mapName: String,
    from: String?,
    event: E?,
    rules: SyncTransitionRules<S, E, C, A, R>?,
    output: MutableList<TransitionView>
) {
    makeView(definition, mapName, from?.toString(), event, rules?.transition, output)
}
