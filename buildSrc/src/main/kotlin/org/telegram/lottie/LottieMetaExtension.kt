package org.telegram.lottie

import org.gradle.api.provider.Property

abstract class LottieMetaExtension {
    abstract val packageName: Property<String>
    abstract val className: Property<String>
}
