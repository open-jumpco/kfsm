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
 * @suppress
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class StateMapInstance<S, E, C, A, R>(
    val context: C,
    val newState: S,
    val name: String?,
    val parentFsm: StateMachineInstance<S, E, C, A, R>,
    val definition: StateMapDefinition<S, E, C, A, R>
) {
    init {
        require(definition.validStates.contains(newState)) { "Initial state is $newState and not in ${definition.validStates}" }
    }

    var currentState: S = newState
        internal set

    internal fun changeState(targetState: S) {
        if (currentState != targetState) {
            val oldState = currentState
            currentState = targetState
            if (definition.afterStateChangeAction != null) {
                try {
                    definition.afterStateChangeAction.invoke(context, oldState, currentState)
                } catch (x: Throwable) {
                    println("changeState:ignoring:exception:$x")
                }
            }
        }
    }
    internal fun execute(transition: SyncTransition<S, E, C, A, R>, arg: A?): R? {
        val result = transition.execute(context, this, arg)
        if (transition.isExternal()) {
            changeState(transition.targetState!!)
        }
        return result
    }

    internal fun executeEntry(context: C, targetState: S, arg: A?) {
        val entryAction =
            definition.entryActions[targetState] ?: parentFsm.definition.defaultStateMap.entryActions[targetState]
        entryAction?.invoke(context, currentState, targetState, arg)
        val defaultEntry = definition.defaultEntryAction ?: parentFsm.definition.defaultStateMap.defaultEntryAction
        defaultEntry?.invoke(context, currentState, targetState, arg)
    }

    internal fun executeExit(context: C, targetState: S, arg: A?) {
        val exitAction =
            definition.exitActions[currentState] ?: parentFsm.definition.defaultStateMap.exitActions[targetState]
        exitAction?.invoke(context, currentState, targetState, arg)
        val defaultExitAction = definition.defaultExitAction ?: parentFsm.definition.defaultStateMap.defaultExitAction
        defaultExitAction?.invoke(context, currentState, targetState, arg)
    }

    private fun executeDefaultAction(event: E, arg: A?): R? {
        return with(
            definition.defaultActions[currentState]
                ?: definition.globalDefault
                ?: parentFsm.definition.defaultStateMap.defaultActions[currentState]
                ?: parentFsm.definition.defaultStateMap.globalDefault
                ?: error("Transition from $currentState on $event not defined")
        ) {
            invoke(context, currentState, event, arg)
        }
    }

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    fun sendEvent(event: E, arg: A? = null): R? {
        var result: R? = null
        definition.transitionRules[Pair(currentState, event)]?.apply rule@{
            this.findGuard(context, arg)?.apply {
                result = parentFsm.execute(this, arg)
            } ?: run {
                this@rule.transition?.apply {
                    result = parentFsm.execute(this, arg)
                } ?: run {
                    result = executeDefaultAction(event, arg)
                }
            }
        } ?: run {
            definition.defaultTransitions[event]?.apply {
                result = parentFsm.execute(this, arg)
            } ?: run {
                result = executeDefaultAction(event, arg)
            }
        }
        return result
    }

    /**
     * This function will provide the list of allowed events given the current state of the machine.
     * @param includeDefaults When `true` will include default transitions in the list of allowed events.
     * @see StateMachineDefinition.allowed
     */
    fun allowed(includeDefaults: Boolean = false) = definition.allowed(currentState, includeDefaults)

    /**
     * This function will provide an indication whether the given event is allow in the current state.
     * @param event The given event that will be used in combination with current state.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        definition.eventAllowed(event, currentState, includeDefault)

    internal fun executeAutomatic(currentTransition: SyncTransition<S, E, C, A, R>, state: S, arg: A?): Boolean {
        var result: Boolean = false
        definition.automaticTransitions[state]?.apply rule@{
            val defaultTransition = this.transition
            findGuard(context, arg)?.apply {
                if (currentTransition !== this) {
                    parentFsm.execute(this, arg)
                    result = true
                }
            } ?: run {
                if (defaultTransition != null) {
                    if (currentTransition !== defaultTransition) {
                        parentFsm.execute(defaultTransition, arg)
                        result = true
                    }
                }
            }
        }
        return result
    }
}
