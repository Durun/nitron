package com.github.durun.nitron.app.metrics

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MetricsCommandTest : FreeSpec({
    "isStyleOnlyChange" {
        AdditionalMetricsCommand.isStyleOnlyChange("if (cond)", "if (cond) {") shouldBe true
        AdditionalMetricsCommand.isStyleOnlyChange("if (cond) {", "if (cond)") shouldBe true
        AdditionalMetricsCommand.isStyleOnlyChange("if (cond)", "if (cond)\n{") shouldBe true
        AdditionalMetricsCommand.isStyleOnlyChange("if (cond1)", "if (cond2) {") shouldBe false
        AdditionalMetricsCommand.isStyleOnlyChange("if (cond1) {", "if (cond2)") shouldBe false
    }
})