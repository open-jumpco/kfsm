/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

/**
 * This class represents an immutable definition of a state machine.
 */
class StateMachineDefinition<S, E : Enum<E>, C>(
    private val deriveInitialState: StateQuery<C, S>?,
    private val deriveInitialMap: StateMapQuery<C, S>?,
    val defaultStateMap: StateMapDefinition<S, E, C>,
    val namedStateMaps: Map<String, StateMapDefinition<S, E, C>>

) {
    private fun createMap(
        mapName: String,
        context: C,
        parentFsm: StateMachineInstance<S, E, C>,
        initial: S
    ) {
        if (mapName == "default") {
            parentFsm.pushMap(StateMapInstance(context, initial, null, parentFsm, defaultStateMap))
        } else {
            val stateMap = namedStateMaps[mapName] ?: error("Invalid map $mapName")
            parentFsm.pushMap(StateMapInstance(context, initial, mapName, parentFsm, stateMap))
        }
    }

    /**
     * This function will create a state machine instance provided with content and optional initialState.
     * @param context The context will be provided to actions
     * @param initialState If this is not provided the function defined in `initial` will be invoked to derive the initialState.
     * @see StateMachineBuilder.initial
     */
    internal fun create(
        context: C,
        parentFsm: StateMachineInstance<S, E, C>,
        initialState: S? = null,
        intitialExternalState: ExternalState<S>? = null
    ): StateMapInstance<S, E, C> {
        if (intitialExternalState != null) {
            intitialExternalState.forEach { (initial, mapName) ->
                createMap(mapName, context, parentFsm, initial)
            }
            return parentFsm.mapStack.pop()
        } else if (deriveInitialMap != null) {
            deriveInitialMap.invoke(context).forEach { (initial, mapName) ->
                createMap(mapName, context, parentFsm, initial)
            }
            return parentFsm.mapStack.pop()
        } else {
            val initial = initialState ?: deriveInitialState?.invoke(context)
            ?: error("Definition requires deriveInitialState or deriveInitialMap")
            return StateMapInstance(context, initial, null, parentFsm, defaultStateMap)
        }
    }

    internal fun createStateMap(
        name: String,
        context: C,
        parentFsm: StateMachineInstance<S, E, C>,
        initialState: S
    ): StateMapInstance<S, E, C> =
        StateMapInstance(
            context,
            initialState,
            name,
            parentFsm,
            namedStateMaps[name] ?: error("Named map $name not found")
        )

    fun create(context: C, initialExternalState: ExternalState<S>): StateMachineInstance<S, E, C> =
        StateMachineInstance<S, E, C>(context, this, null, initialExternalState)

    fun create(context: C): StateMachineInstance<S, E, C> = StateMachineInstance<S, E, C>(context, this, null)
}
