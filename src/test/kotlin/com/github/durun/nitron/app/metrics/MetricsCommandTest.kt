package com.github.durun.nitron.app.metrics

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MetricsCommandTest : FreeSpec({
    "isStyleOnlyChange" {
        MetricsCommand.isStyleOnlyChange("if (cond)", "if (cond) {") shouldBe true
        MetricsCommand.isStyleOnlyChange("if (cond) {", "if (cond)") shouldBe true
        MetricsCommand.isStyleOnlyChange("if (cond)", "if (cond)\n{") shouldBe true
        MetricsCommand.isStyleOnlyChange("if (cond1)", "if (cond2) {") shouldBe false
        MetricsCommand.isStyleOnlyChange("if (cond1) {", "if (cond2)") shouldBe false
    }
})