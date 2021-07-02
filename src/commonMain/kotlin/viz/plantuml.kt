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

/**
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
fun plantUml(input: Iterable<TransitionView>): String {
    val output = StringBuilder()
    output.append("@startuml\n")
    val stateMaps = input.filter { it.sourceMap != null && it.sourceMap != "default" }.groupBy { it.sourceMap!! }
    stateMaps.forEach { sm ->
        val entry = input.firstOrNull { it.sourceMap == sm.key }
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
        output.append("  ")
        output.append(printPlantUmlTransition(transition))
        output.append("\n")
    }
    output.append("}\n")
    output.append("@enduml\n")

    return output.toString()
}

private fun printPlantUmlTransition(transition: TransitionView): String {
    val startName =
        if (transition.type == TransitionType.DEFAULT) transition.sourceMap else {
            if ("<<start>>" == transition.start) "[*]" else transition.start
                ?: transition.target
        }
    val endName = transition.target ?: transition.start
    val event = if (transition.automatic) "<<automatic>>" else transition.event
    return if (event != null) {
        "$startName --> $endName : $event"
    } else {
        "$startName --> $endName"
    }
}
