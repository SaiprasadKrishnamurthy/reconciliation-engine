package com.taxreco.recon.engine.repository

import com.taxreco.recon.engine.model.JobProgressState
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface JobProgressStateRepository : CrudRepository<JobProgressState, String> {
    fun deleteByJobId(jobId: String)
    fun countByJobId(jobId: String): Long
}

