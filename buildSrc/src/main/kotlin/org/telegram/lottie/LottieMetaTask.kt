package org.telegram.lottie

import com.google.gson.stream.JsonReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

abstract class LottieMetaTask : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rawResources: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val rPackage: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private data class Entry(val name: String, val fps: Int, val frameCount: Int, val monocolor: Boolean)

    @TaskAction
    fun run() {
        val parsed = rawResources.files
            .filter { it.isFile }
            .mapNotNull { file ->
                val info = parseLottie(file) ?: return@mapNotNull null
                file.nameWithoutExtension to Triple(info.fps, info.frameCount, false)
            }
            .groupBy({ it.first }, { it.second })

        val entries = parsed.toSortedMap().map { (name, values) ->
            if (values.distinct().size > 1) {
                logger.warn("R.raw.$name: conflicting metadata across qualifiers, using the first")
            }
            val (fpsRaw, frameCount, monocolor) = values.first()
            val fps = fpsRaw.roundToInt()
            if (fps !in 0..FPS_MAX) {
                throw GradleException("R.raw.$name: fps=$fps does not fit into 8 bits (0..$FPS_MAX)")
            }
            if (frameCount !in 0..FRAMES_MAX) {
                throw GradleException("R.raw.$name: frameCount=$frameCount does not fit into 23 bits (0..$FRAMES_MAX)")
            }
            Entry(name, fps, frameCount, monocolor)
        }

        val outputPackage = packageName.get()
        val className = className.get()
        val root = outputDir.get().asFile
        root.deleteRecursively()
        val packageDir = File(root, outputPackage.replace('.', '/')).apply { mkdirs() }
        File(packageDir, "$className.java").writeText(renderJava(outputPackage, rPackage.get(), className, entries))
    }

    private data class LottieInfo(val fps: Double, val frameCount: Int)

    private fun parseLottie(file: File): LottieInfo? = try {
        file.inputStream().buffered().use { raw ->
            raw.mark(2)
            val gzip = raw.read() == 0x1f && raw.read() == 0x8b
            raw.reset()
            val stream = if (gzip) GZIPInputStream(raw) else raw

            var fps: Double? = null
            var inPoint: Double? = null
            var outPoint: Double? = null

            JsonReader(stream.reader(Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "fr" -> fps = reader.nextDouble()
                        "ip" -> inPoint = reader.nextDouble()
                        "op" -> outPoint = reader.nextDouble()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }

            val frameRate = fps
            val firstFrame = inPoint
            val lastFrame = outPoint
            if (frameRate == null || firstFrame == null || lastFrame == null || frameRate <= 0.0) {
                return null
            }
            LottieInfo(frameRate, (lastFrame - firstFrame).roundToInt())
        }
    } catch (_: Exception) {
        null
    }

    private fun renderJava(packageName: String, rPackage: String, className: String, entries: List<Entry>): String {
        val rImport = if (packageName == rPackage) "" else "import $rPackage.R;\n"
        val source = StringBuilder()
        source.append("package ").append(packageName).append(";\n\n")
        source.append("import androidx.annotation.RawRes;\n")
        source.append("import java.util.Arrays;\n")
        source.append(rImport).append('\n')
        source.append("public final class ").append(className).append(" {\n\n")
        source.append("    public static final long NOT_FOUND = -1L;\n\n")
        source.append("    private ").append(className).append("() {\n    }\n\n")
        source.append("    private static final class Holder {\n")
        source.append("        private static final long[] DATA = build();\n")
        source.append("    }\n\n")
        source.append("    private static long[] build() {\n")
        source.append("        final long[] data = new long[]{\n")
        entries.forEach { entry ->
            source.append("            pack(R.raw.").append(entry.name).append(", ")
                .append(entry.fps).append(", ").append(entry.frameCount).append(", ")
                .append(entry.monocolor).append("),\n")
        }
        source.append("        };\n")
        source.append("        Arrays.sort(data);\n")
        source.append("        return data;\n")
        source.append("    }\n\n")
        source.append("    private static long pack(int resId, int fps, int frameCount, boolean mono) {\n")
        source.append("        return ((long) resId << 32)\n")
        source.append("                | ((long) (fps & 0xFF) << 24)\n")
        source.append("                | (mono ? (1L << 23) : 0L)\n")
        source.append("                | (frameCount & 0x7FFFFF);\n")
        source.append("    }\n\n")
        source.append("    public static long find(@RawRes int resId) {\n")
        source.append("        final long[] data = Holder.DATA;\n")
        source.append("        int lo = 0, hi = data.length - 1;\n")
        source.append("        while (lo <= hi) {\n")
        source.append("            final int mid = (lo + hi) >>> 1;\n")
        source.append("            final int midId = (int) (data[mid] >>> 32);\n")
        source.append("            if (midId < resId) {\n")
        source.append("                lo = mid + 1;\n")
        source.append("            } else if (midId > resId) {\n")
        source.append("                hi = mid - 1;\n")
        source.append("            } else {\n")
        source.append("                return data[mid];\n")
        source.append("            }\n")
        source.append("        }\n")
        source.append("        return NOT_FOUND;\n")
        source.append("    }\n\n")
        source.append("    public static boolean isLottie(@RawRes int resId) {\n")
        source.append("        return find(resId) != NOT_FOUND;\n")
        source.append("    }\n\n")
        source.append("    public static boolean isMonoColor(@RawRes int resId) {\n")
        source.append("        long packed = find(resId);\n")
        source.append("        return packed != NOT_FOUND && isMonoColorOf(packed);\n")
        source.append("    }\n\n")
        source.append("    public static int fpsOf(long packed) {\n")
        source.append("        return (int) ((packed >>> 24) & 0xFF);\n")
        source.append("    }\n\n")
        source.append("    public static boolean isMonoColorOf(long packed) {\n")
        source.append("        return (packed & (1L << 23)) != 0L;\n")
        source.append("    }\n\n")
        source.append("    public static int frameCountOf(long packed) {\n")
        source.append("        return (int) (packed & 0x7FFFFF);\n")
        source.append("    }\n\n")
        source.append("    public static int resIdOf(long packed) {\n")
        source.append("        return (int) (packed >>> 32);\n")
        source.append("    }\n")
        source.append("}\n")
        return source.toString()
    }

    private companion object {
        const val FPS_MAX = 0xFF
        const val FRAMES_MAX = 0x7FFFFF
    }
}
