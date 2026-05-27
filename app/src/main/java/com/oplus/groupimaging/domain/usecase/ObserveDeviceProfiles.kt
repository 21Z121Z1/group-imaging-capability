package com.oplus.groupimaging.domain.usecase

import com.oplus.groupimaging.domain.DeviceProfile
import com.oplus.groupimaging.domain.repository.OplusInsightRepository
import javax.inject.Inject

class ObserveDeviceProfiles @Inject constructor(
    private val repository: OplusInsightRepository,
) {
    suspend operator fun invoke(): List<DeviceProfile> = repository.loadDeviceProfiles()
}
