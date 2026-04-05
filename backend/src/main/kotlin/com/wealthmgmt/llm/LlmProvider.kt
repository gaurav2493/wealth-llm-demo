package com.wealthmgmt.llm

import com.wealthmgmt.model.IntentResult

interface LlmProvider {
    fun resolveIntent(message: String): IntentResult
}
