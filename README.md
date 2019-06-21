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

    fun doubleLock() {
        assert(locked)
        println("DoubleLock")
    }

    fun unlock() {
        println("Unlock")
        locked = false
    }
    fun doubleUnlock() {
        assert(locked)
        println("DoubleUnlock")
    }
}
```
We declare 2 enums, one for the possible states and one for the possible events.
For the sake of testing more than 2 states we are simulating double-locking.

```kotlin
enum class LockStates {
    LOCKED,
    DOUBLE_LOCKED,
    UNLOCKED
}

enum class LockEvents {
    LOCK,
    UNLOCK
}
```

Then we declare a statemachine
```kotlin
val definition = StateMachine<LockStates, LockEvents, Lock>().dsl {
    initial { if (it.locked) LOCKED else UNLOCKED }
    event(UNLOCK) {
        state(LOCKED to UNLOCKED) { context ->
            context.unlock()
        }
        state(DOUBLE_LOCKED to LOCKED) { context ->
            context.doubleUnlock()
        }
        state(UNLOCKED) {
            error("Already Unlocked")
        }
    }
    event(LOCK) {
        state(UNLOCKED to LOCKED) { context ->
            context.lock()
        }
        state(LOCKED to DOUBLE_LOCKED) { context ->
            context.doubleLock()
        }
        state(DOUBLE_LOCKED) {
            error("Already Double Locked")
        }
    }
}.build()
```
With this We are saying:
On an event UNLOCK when the state is LOCKED transition to UNLOCKED and execute the code in the lambda 

Then we instantiate the FSM and provide a context to operate on:

```kotlin
val lock = Lock(true)
val fsm = definition.instance(lock)
```

Now we have a context that is independent of the FSM. 

Sending events may invoke actions:
```kotlin
// State state is LOCKED
fsm.event(LOCK)
// Expect DoubleLock and end state is DOUBLE_LOCKED
fsm.event(UNLOCK)
// Expect DoubleUnlock and end state is LOCKED
fsm.event(UNLOCK)
// Expect Unlock and end state is UNLOCKED
```
