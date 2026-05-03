package com.peter.fitness.feature.hello

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HelloViewModelTest {

    @Test
    fun `tap count starts at zero`() {
        val vm = HelloViewModel()
        vm.uiState.value.tapCount shouldBe 0
    }

    @Test
    fun `tap count increments on each tap`() {
        val vm = HelloViewModel()
        vm.onTap()
        vm.onTap()
        vm.onTap()
        vm.uiState.value.tapCount shouldBe 3
    }
}
