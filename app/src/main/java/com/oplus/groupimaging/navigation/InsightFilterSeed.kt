package com.oplus.groupimaging.navigation

import com.oplus.groupimaging.domain.LensClass
import java.io.Serializable
import java.time.LocalDate

data class InsightFilterSeed(
    val years: Set<Int> = emptySet(),
    val yearMonths: Set<String> = emptySet(),
    val dates: Set<LocalDate> = emptySet(),
    val devices: Set<String> = emptySet(),
    val lenses: Set<LensClass> = emptySet(),
    val isLiveOnly: Boolean = false,
    val isRawOnly: Boolean = false,
    val focalRanges: Set<String> = emptySet(),
) : Serializable
