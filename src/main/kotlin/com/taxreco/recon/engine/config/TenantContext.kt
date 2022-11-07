package com.taxreco.recon.engine.config

import java.lang.ThreadLocal
import com.taxreco.recon.engine.config.TenantContext
import org.springframework.stereotype.Component

/**
 * A Simple ThreadLocals bound context object for multi-tenancy.
 */
@Component
class TenantContext {
    private val CONTEXT = ThreadLocal<String>()
    var tenantId: String?
        get() = CONTEXT.get()
        set(tenantId) {
            CONTEXT.set(tenantId)
        }

    fun clear() {
        CONTEXT.remove()
    }
}