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

class Lock {
    var locked: Int = 1
        private set

    fun lock() {
        assert(locked == 0)
        println("Lock")
        locked += 1
    }

    fun doubleLock() {
        assert(locked == 1)
        println("DoubleLock")
        locked += 1
    }

    fun unlock() {
        assert(locked == 1)
        println("Unlock")
        locked -= 1
    }
    fun doubleUnlock() {
        assert(locked == 2)
        println("DoubleUnlock")
        locked -= 1
    }

    override fun toString(): String {
        return "Lock(locked=$locked)"
    }

}
