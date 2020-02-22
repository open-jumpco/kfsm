/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm.viz

import io.jumpco.open.kfsm.DefaultTransition
import io.jumpco.open.kfsm.GuardedTransition
import io.jumpco.open.kfsm.SimpleTransition
import io.jumpco.open.kfsm.StateMachineDefinition
import io.jumpco.open.kfsm.StateMapDefinition
import io.jumpco.open.kfsm.Transition
import io.jumpco.open.kfsm.TransitionRules
import io.jumpco.open.kfsm.TransitionType

/**
 * The visualization provided by these methods will not be able to provide any detail about guard expressions or actions.
 * This visualization only provides states and transitions.
 * The separate kfsm-viz module and associated Gradle plugin will provide visualization after parsing the definition DSL.
 */
data class TransitionView(
    val sourceMap: String? = null,
    val start: String?,
    val event: String?,
    val target: String? = null,
    val targetMap: String? = null,
    val action: String? = null,
    val automatic: Boolean = false,
    val type: TransitionType = TransitionType.NORMAL,
    val guard: String? = null
)

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
    transition: Transition<S, E, C, A, R>?,
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
        is GuardedTransition<S, E, C, A, R> -> TransitionView(
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
        is SimpleTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            from.toString(),
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            transition.type
        )
        is DefaultTransition<S, E, C, A, R> -> TransitionView(
            sourceMap,
            null,
            event?.toString(),
            targetState?.toString(),
            targetMap,
            transition.action?.toString(),
            transition.automatic,
            TransitionType.DEFAULT
        )
        is Transition<S, E, C, A, R> -> TransitionView(
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
    rules: TransitionRules<S, E, C, A, R>?,
    output: MutableList<TransitionView>
) {
    makeView(definition, mapName, from?.toString(), event, rules?.transition, output)
}

public fun plantUml(input: Iterable<TransitionView>): String {
    val output = StringBuilder()
    output.append("@startuml\n")
    val stateMaps = input.filter { it.sourceMap != null && it.sourceMap != "default" }.groupBy { it.sourceMap!! }
    stateMaps.forEach { sm ->
        val entry = input.filter { it.sourceMap == sm.key }.firstOrNull()
        output.append("state ${entry?.sourceMap} {\n")
        sm.value.forEach { transition ->
            output.append("  ")
            output.append(printPlantUmlTransition(transition))
            output.append("\n")
        }
        output.append("}\n")
    }
    output.append("state default {\n")
    input.filter { it.sourceMap == "default" }.forEach { transition ->
        output.append("  ");
        output.append(printPlantUmlTransition(transition))
        output.append("\n")
    }
    output.append("}\n")
    output.append("@enduml\n")

    return output.toString()
}

private fun printPlantUmlTransition(transition: TransitionView): String {
    val startName =
        if (transition.type == TransitionType.DEFAULT) transition.sourceMap else
            if ("<<start>>" == transition.start) "[*]" else transition.start
                ?: transition.target
    val endName = transition.target ?: transition.start
    val event = if (transition.automatic) "<<automatic>>" else transition.event
    return if (event != null) {
        "$startName --> $endName : $event"
    } else {
        "$startName --> $endName"
    }
}


