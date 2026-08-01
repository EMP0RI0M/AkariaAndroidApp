package com.akaria.agent

import android.util.Log

class AkariaEngine {
    init {
        try {
            System.loadLibrary("akaria_engine")
            Log.i("AkariaEngine", "Successfully loaded native C++ engine!")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("AkariaEngine", "Failed to load native C++ engine.", e)
        }
    }

    /**
     * A native method that is implemented by the 'akaria_engine' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
}
