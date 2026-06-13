package com.sportspulse.india.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sportspulse.india.core.domain.repository.BroadcastRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker that runs periodically (e.g., every 6 hours) to fetch the latest
 * broadcast schedule from the GitHub Gist and cache it in Room.
 */
@HiltWorker
class BroadcastRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val broadcastRepository: BroadcastRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "BroadcastRefreshWorker"
    }

    override suspend fun doWork(): Result {
        Timber.d("BroadcastRefreshWorker: starting background sync")
        
        val result = broadcastRepository.refreshRemoteConfig()
        
        return if (result.isSuccess) {
            Timber.d("BroadcastRefreshWorker: sync success")
            Result.success()
        } else {
            Timber.e("BroadcastRefreshWorker: sync failed, will retry")
            Result.retry()
        }
    }
}
