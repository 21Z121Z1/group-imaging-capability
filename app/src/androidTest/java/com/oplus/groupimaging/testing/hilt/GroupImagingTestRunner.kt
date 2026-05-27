package com.oplus.groupimaging.testing.hilt

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class GroupImagingTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        name: String,
        context: Context,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
