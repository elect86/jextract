package jextract

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class JdkExtension
    @Inject constructor(dir: File) {

    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val dir: Property<File>

    @get:Input
    abstract val type: Property<Type>

    init {
        name.convention("jdk-17")
        this.dir.convention(dir)
        type.convention(Type.earlyAccess)
    }

    enum class Type { nightly, earlyAccess }
}