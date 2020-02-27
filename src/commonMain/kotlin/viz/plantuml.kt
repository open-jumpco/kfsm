/*
 * Copyright (c) 2020. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.jumpco.open.kfsm.viz

import io.jumpco.open.kfsm.TransitionType
import io.jumpco.open.kfsm.viz.TransitionView

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
