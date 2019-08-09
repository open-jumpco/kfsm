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
 * This class represents an instance of a state machine.
 * It will process events, matching transitions, invoking entry, transition and exit actions.
 * @param context The events may trigger actions on the context of class C
 * @param definition The defined state machine that provides all the behaviour
 * @param initialState The initial state of the instance.
 */
class StateMachineInstance<S, E : Enum<E>, C>(
    /**
     * The transition actions are performed by manipulating the context.
     */
    private val context: C,
    /**
     * The Immutable definition of the state machine.
     */
    val definition: StateMachineDefinition<S, E, C>,
    /**
     * The initialState will be assigned to the currentState
     */
    initialState: S?
) {

    internal val namedInstances: MutableMap<String, StateMapInstance<S, E, C>> = mutableMapOf()

    internal val mapStack: Stack<StateMapInstance<S, E, C>> = Stack()

    /**
     * This represents the current state of the state machine.
     * It will be modified during transitions
     */
    internal var currentStateMap: StateMapInstance<S, E, C>

    init {
        currentStateMap = definition.create(context, this, initialState)
    }

    val currentState: S
        get() = currentStateMap.currentState


    internal fun pushMap(
        defaultInstance: StateMapInstance<S, E, C>,
        initial: S,
        name: String,
        stateMap: StateMapDefinition<S, E, C>
    ): StateMapInstance<S, E, C> {
        mapStack.push(defaultInstance)
        val pushedMap = StateMapInstance(context, initial, name, this, stateMap)
        currentStateMap = pushedMap
        return pushedMap
    }

    internal fun pushMap(stateMap: StateMapInstance<S, E, C>): StateMapInstance<S, E, C> {
        mapStack.push(stateMap)
        return stateMap
    }

    internal fun execute(transition: Transition<S, E, C>, args: Array<out Any>) {
        if (transition.type == TransitionType.PUSH) {
            require(transition.targetState != null) { "Target state cannot be null for pushTransition" }
            require(transition.targetMap != null) { "Target map cannot be null for pushTransition" }
            if (transition.targetMap != currentStateMap.name) {
                executePush(transition, args)
            } else {
                currentStateMap.execute(transition, args)
            }
        } else if (transition.type == TransitionType.POP) {
            val sourceMap = currentStateMap
            val targetMap = mapStack.pop()
            currentStateMap = targetMap
            if (transition.targetMap == null) {
                if (transition.targetState == null) {
                    transition.execute(context, sourceMap, targetMap, args)
                } else {
                    transition.execute(context, sourceMap, null, args)
                    targetMap.executeEntry(context, transition.targetState, args)
                    currentStateMap.currentState = transition.targetState
                }
            } else {
                executePush(transition, args)
                if (transition.targetState != null) {
                    currentStateMap.currentState = transition.targetState
                }
            }
        } else {
            currentStateMap.execute(transition, args)
        }
        currentStateMap.executeAutomatic(transition, currentStateMap.currentState, args)
    }

    private fun executePush(transition: Transition<S, E, C>, args: Array<out Any>) {
        val targetStateMap = namedInstances.getOrElse(transition.targetMap!!) {
            val stateMapInstance =
                definition.create(transition.targetMap, context, this, transition.targetState!!)
            namedInstances.put(transition.targetMap, stateMapInstance)
            stateMapInstance
        }
        mapStack.push(currentStateMap)
        transition.execute(context, currentStateMap, targetStateMap, args)
        currentStateMap = targetStateMap
        currentStateMap.currentState = transition.targetState!!
    }

    /**
     * This function will provide the set of allowed events given a specific state. It isn't a guarantee that a
     * subsequent transition will be successful since a guard may prevent a transition. Default state handlers are not considered.
     * @param given The specific state to consider
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
     */
    fun allowed(includeDefault: Boolean = false): Set<E> = currentStateMap.allowed(includeDefault)

    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        currentStateMap.eventAllowed(event, includeDefault)

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    fun sendEvent(event: E, vararg args: Any) = currentStateMap.sendEvent(event, *args)
}
