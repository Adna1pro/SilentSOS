package com.example.silentsos

class DistressDetector {
    fun isDistress(acceleration: Float): Boolean {
        // For now, let's assume a simple threshold.
        // We can refine this later.
        return acceleration > 20.0f
    }
}
