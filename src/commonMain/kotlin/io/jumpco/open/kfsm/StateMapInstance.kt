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
 */
class StateMapInstance<S, E : Enum<E>, C>(
    val context: C,
    val newState: S,
    val name: String?,
    val parentFsm: StateMachineInstance<S, E, C>,
    val definition: StateMapDefinition<S, E, C>
) {
    init {
        require(definition.validStates.contains(newState)) { "Initial state is $newState and not in ${definition.validStates}" }
    }

    var currentState: S = newState
        internal set

    internal fun execute(transition: Transition<S, E, C>, args: Array<out Any>) {
        transition.execute(context, this, args)
        if (transition.isExternal()) {
            currentState = transition.targetState!!
        }
    }

    internal fun executeEntry(context: C, targetState: S, args: Array<out Any>) {
        val entryAction =
            definition.entryActions[targetState] ?: parentFsm.definition.defaultStateMap.entryActions[targetState]
        entryAction?.invoke(context, currentState, targetState, args)
        val defaultEntry = definition.defaultEntryAction ?: parentFsm.definition.defaultStateMap.defaultEntryAction
        defaultEntry?.invoke(context, currentState, targetState, args)
    }

    internal fun executeExit(context: C, targetState: S, args: Array<out Any>) {
        val exitAction =
            definition.exitActions[currentState] ?: parentFsm.definition.defaultStateMap.exitActions[targetState]
        exitAction?.invoke(context, currentState, targetState, args)
        val defaultExitAction = definition.defaultExitAction ?: parentFsm.definition.defaultStateMap.defaultExitAction
        defaultExitAction?.invoke(context, currentState, targetState, args)
    }

    private fun executeDefaultAction(event: E, args: Array<out Any>) {
        with(
            definition.defaultActions[currentState]
                ?: definition.globalDefault
                ?: parentFsm.definition.defaultStateMap.defaultActions[currentState]
                ?: parentFsm.definition.defaultStateMap.globalDefault
                ?: error("Transition from $currentState on $event not defined")
        ) {
            invoke(context, currentState, event, args)
        }
    }

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    fun sendEvent(event: E, vararg args: Any) {
        definition.transitionRules[Pair(currentState, event)]?.apply rule@{
            this.findGuard(context, args)?.apply {
                parentFsm.execute(this, args)
            } ?: run {
                this@rule.transition?.apply {
                    parentFsm.execute(this, args)
                } ?: run {
                    executeDefaultAction(event, args)
                }
            }
        } ?: run {
            definition.defaultTransitions[event]?.apply {
                parentFsm.execute(this, args)
            } ?: run {
                executeDefaultAction(event, args)
            }
        }
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

    internal fun executeAutomatic(currentTransition: Transition<S, E, C>, state: S, args: Array<out Any>): Boolean {
        definition.automaticTransitions[state]?.apply rule@{
            val defaultTransition = this.transition
            findGuard(context, args)?.apply {
                if (currentTransition !== this) {
                    parentFsm.execute(this, args)
                    return true
                }
            } ?: run {
                if (defaultTransition != null) {
                    if (currentTransition !== defaultTransition) {
                        parentFsm.execute(defaultTransition, args)
                        return true
                    }
                }
            }
        }
        return false
    }
}
