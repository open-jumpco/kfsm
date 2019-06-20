# Kotlin Finite State Machine

This is a small implementation of an FSM in Kotlin

Assume we and to manage the state on a simple lock.
We want to ensure that the `lock()` function is only called when the lock is not locked and we want `unlock()` to be called when locked.

```kotlin
class Lock(l: Boolean = false) {
    var locked: Boolean = l
        private set

    fun lock() {
        println("Lock")
        locked = true
    }

    fun unlock() {
        println("Unlock")
        locked = false
    }
}
```
We declare 2 enums, one for the possible states and one for the possible events.

```kotlin
enum class LockStates {
    LOCKED,
    UNLOCKED
}

enum class LockEvents {
    LOCK,
    UNLOCK
}
```

Then we declare a statemachine
```kotlin
val definition = StateMachine<LockStates, LockEvents, Lock>() {
        if(it.locked) LOCKED else UNLOCKED
    }
    .on(UNLOCK, LOCKED, UNLOCKED) { context ->
        context.unlock()
    }
    .on(LOCK, UNLOCKED, LOCKED) { context ->
        context.lock()
    }
```
With this We are saying:
On an event UNLOCK when the state is LOCKED transition to UNLOCKED and execute the code in the lambda 

Then we instantiate the FSM and provide a context to operate on:

```kotlin
val lock = Lock(true)
val fsm = definition.instance(LOCKED, lock)
```

Now we have a context that is independent of the FSM. 

