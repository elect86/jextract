package jextract

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class JdkExtension {

    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val dir: Property<File>

    init {
        name.convention("jdk-17")
        dir.convention(File(System.getProperty("java.io.tmpdir")))
    }
}