package io.github.sandrogekeler.glotze.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start of the home screen, to the first frame and to the app list on screen
 * (timeToInitialDisplayMs and timeToFullDisplayMs). The number for the budget in
 * agent_docs/HEALTH_CHECKLIST.md is `withBaselineProfile` on the TV.
 *
 *   ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Worst case: nothing compiled ahead of time, as right after an install without a profile. */
    @Test
    fun withoutCompilation() = startup(CompilationMode.None())

    /** What the TV runs once ProfileInstaller has applied the Baseline Profile. */
    @Test
    fun withBaselineProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilation: CompilationMode) =
        rule.measureRepeated(
            packageName = PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilation,
            startupMode = StartupMode.COLD,
            iterations = 10,
        ) {
            startHome()
        }
}
