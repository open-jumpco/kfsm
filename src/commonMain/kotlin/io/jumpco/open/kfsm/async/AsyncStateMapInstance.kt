/*
    Copyright 2019-2021 Open JumpCO

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
    and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial
    portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
    CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.
 */

package io.jumpco.open.kfsm.async

import io.jumpco.open.kfsm.checkInvariant

class AsyncStateMapInstance<S, E, C, A, R>(
    val context: C,
    val newState: S,
    val name: String?,
    val parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
    val definition: AsyncStateMapDefinition<S, E, C, A, R>
) {
    init {
        require(definition.validStates.contains(newState)) { "Initial state is $newState and not in ${definition.validStates}" }
        definition.invariants.forEach { checkInvariant(context, it) }
    }

    var currentState: S = newState
        internal set
    val timers: MutableList<AsyncTimer<S, E, C, A, R>> = mutableListOf()

    internal suspend fun execute(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
        cancelTimers()
        val result = transition.execute(context, this, arg)
        if (transition.isExternal()) {
            changeState(transition.targetState!!)
        }
        if (transition.targetState != null) {
            createTimers(transition.targetState, context, arg)
        }
        return result
    }

    internal suspend fun changeState(targetState: S) {
        definition.invariants.forEach { checkInvariant(context, it) }
        if (currentState != targetState) {
            val oldState = currentState
            currentState = targetState
            if (definition.afterStateChangeAction != null) {
                try {
                    definition.afterStateChangeAction.invoke(context, oldState, currentState)
                } catch (x: Throwable) {
                    // ignoring
                }
            }
            definition.invariants.forEach { checkInvariant(context, it) }
        }
    }

    internal fun executeEntry(context: C, targetState: S, arg: A?) {
        val entryAction = definition.entryActions[targetState]
            ?: parentFsm.definition.defaultStateMap.entryActions[targetState]
        entryAction?.invoke(context, currentState, targetState, arg)
        val defaultEntry = definition.defaultEntryAction ?: parentFsm.definition.defaultStateMap.defaultEntryAction
        defaultEntry?.invoke(context, currentState, targetState, arg)
    }

    internal fun cancelTimers() {
        timers.forEach { it.cancel() }
        timers.clear()
    }

    internal fun createTimers(targetState: S, context: C, arg: A?) {
        val timerDefinition = definition.timerDefinitions[targetState]
        if (timerDefinition != null) {
            timers.add(AsyncTimer(this, context, arg, timerDefinition))
        }
    }

    internal fun executeExit(context: C, targetState: S, arg: A?) {
        val exitAction =
            definition.exitActions[currentState] ?: parentFsm.definition.defaultStateMap.exitActions[targetState]
        exitAction?.invoke(context, currentState, targetState, arg)
        val defaultExitAction = definition.defaultExitAction ?: parentFsm.definition.defaultStateMap.defaultExitAction
        defaultExitAction?.invoke(context, currentState, targetState, arg)
    }

    private suspend fun executeDefaultAction(event: E, arg: A?): R? {
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
    suspend fun sendEvent(event: E, arg: A? = null): R? {
        return definition.transitionRules[Pair(currentState, event)]?.let { rule ->
            rule.findGuard(context, arg)?.let {
                parentFsm.execute(it, arg)
            } ?: rule.transition?.let {
                parentFsm.execute(it, arg)
            } ?: executeDefaultAction(event, arg)
        } ?: definition.defaultTransitions[event]?.let {
            parentFsm.execute(it, arg)
        } ?: executeDefaultAction(event, arg)
    }

    /**
     * This function will provide the list of allowed events given the current state of the machine.
     * @param includeDefaults When `true` will include default transitions in the list of allowed events.
     */
    fun allowed(includeDefaults: Boolean = false) = definition.allowed(currentState, includeDefaults)

    /**
     * This function will provide an indication whether the given event is allow in the current state.
     * @param event The given event that will be used in combination with current state.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        definition.eventAllowed(event, currentState, includeDefault)

    internal suspend fun executeAutomatic(
        currentTransition: AsyncTransition<S, E, C, A, R>,
        state: S,
        arg: A?
    ): Boolean {
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
