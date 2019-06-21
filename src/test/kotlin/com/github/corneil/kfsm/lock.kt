package com.github.corneil.kfsm

enum class LockStates {
    LOCKED,
    DOUBLE_LOCKED,
    UNLOCKED
}

enum class LockEvents {
    LOCK,
    UNLOCK
}

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
