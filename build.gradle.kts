\

// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
<<<<<<< HEAD
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    
    // DELETE THIS LINE causing the crash:
    // alias(libs.plugins.kotlin.compose) apply false
}
=======
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}
>>>>>>> 6636fc7 (updated the payment screen)
