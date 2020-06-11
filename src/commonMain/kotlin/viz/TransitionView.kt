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

/**
 * The visualization provided by these methods will not be able to provide any detail about guard expressions or actions.
 * This visualization only provides states and transitions.
 * The separate kfsm-viz module and associated Gradle plugin will provide visualization after parsing the definition DSL.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
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
