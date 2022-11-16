package com.taxreco.recon.engine.repository

import com.taxreco.recon.engine.model.ReconciliationSetting
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReconciliationSettingRepository : CrudRepository<ReconciliationSetting, String> {
    fun findByNameAndVersion(name: String, version: Long): ReconciliationSetting
}

