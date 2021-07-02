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
