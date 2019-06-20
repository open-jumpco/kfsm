package com.github.corneil.kfsm

enum class LockStates {
    LOCKED,
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

    fun unlock() {
        println("Unlock")
        locked = false
    }
}
