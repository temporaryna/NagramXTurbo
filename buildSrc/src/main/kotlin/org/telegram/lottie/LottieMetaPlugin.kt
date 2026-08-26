package org.telegram.lottie

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class LottieMetaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("lottieMeta", LottieMetaExtension::class.java)
        extension.className.convention("ResLottieMeta")

        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
            ?: error("Apply com.android.application/library before org.telegram.lottie-meta")

        androidComponents.onVariants { variant ->
            val suffix = variant.name.replaceFirstChar { it.uppercase() }
            val task = project.tasks.register<LottieMetaTask>("generate${suffix}LottieMeta") {
                className.set(extension.className)
                packageName.set(extension.packageName.orElse(variant.namespace))
                rPackage.set(variant.namespace)

                variant.sources.res?.all?.let { layers ->
                    rawResources.from(
                        layers.map { collections ->
                            collections.flatten().map { directory ->
                                directory.asFileTree.matching { include("raw*/**/*.json") }
                            }
                        }
                    )
                }
            }

            variant.sources.java?.addGeneratedSourceDirectory(task, LottieMetaTask::outputDir)
        }
    }
}
