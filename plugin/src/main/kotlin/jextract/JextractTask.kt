package jextract

import de.undercouch.gradle.tasks.download.*
import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


abstract class JextractTask : DefaultTask() {

    /** Arguments which should be passed to clang. */
    @Optional @Input
    val clangArguments: Property<String> = project.objects.property()

    /** Whether to generate sources or precompiled class files */
    @Input
    val sourceMode: Property<Boolean> = project.objects.property<Boolean>().convention(false)

    val Project.jdk17: String
        get() = properties["org.gradle.java.installations.paths"].toString().split(',')
            .first { "jdk" in it && "17" in it }

    /** The JDK home directory containing jextract. */
    @Optional @Input
    val javaHome: Property<String> = project.objects.property<String>().convention(Jvm.current().javaHome.absolutePath)

    /** Directories which should be included during code generation. */
    @Optional @Input
    val includes: ListProperty<String> = project.objects.listProperty()

    /** The output directory in which the generated code will be placed. */
    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty().convention(project.layout.buildDirectory.dir("generated/sources/jextract/main/java"))

    @Nested
    val libraries = ArrayList<Library>()

    fun jdk(action: Action<JdkExtension>) = action.execute(project.extensions.create("jdk"))

    init {
        group = "build"
    }

    @get:Internal
    val download: DownloadExtension
        get() = project.extensions.getByName("download") as DownloadExtension

    @TaskAction
    fun action() {

        println("jextract action")
        // Check if jextract is present
        val javaPath = javaHome.get()
        val jextractPath = Paths.get(javaPath, "bin/jextract")
        if (!Files.exists(jextractPath))
            throw GradleException("jextract binary could not be found (JVM_HOME=$javaPath)")

        //        val jdkEx = project.extensions.getByName<JdkExtension>("jdk")

        for (lib in libraries) {

            // Initialize argument list
            val arguments = ArrayList<String>()

            // Add source mode flag if it was enabled by the user
            if (sourceMode.get())
                arguments += "--source"

            // Add clang arguments if they are present
            clangArguments.orNull?.let {
                arguments += "-C"
                arguments += it
            }

            // Include specified functions
            lib.functions.orNull?.forEach {
                arguments += "--include-function"
                arguments += it
            }

            // Include specified macros
            lib.macros.orNull?.forEach {
                arguments += "--include-macro"
                arguments += it
            }

            // Include specified structs
            lib.structs.orNull?.forEach {
                arguments += "--include-struct"
                arguments += it
            }

            // Include specified typedefs
            lib.typedefs.orNull?.forEach {
                arguments += "--include-typedef"
                arguments += it
            }

            // Include specified functions
            lib.unions.orNull?.forEach {
                arguments += "--include-union"
                arguments += it
            }

            // Include specified functions
            lib.variables.orNull?.forEach {
                arguments += "--include-var"
                arguments += it
            }

            // Add include paths if they are present
            lib.includes.orNull?.forEach {
                arguments += "-I"
                arguments += it
            }

            // Add library names if they are present
            lib.libraries.orNull?.let {
                if (it.isEmpty())
                    throw GradleException("At least on library has to be specified")

                for (library in it) {
                    arguments += "-l"
                    arguments += library
                }
            }

            // Add target package if it is present
            lib.targetPackage.orNull?.let {
                arguments += "--target-package"
                arguments += it
            }

            lib.className.orNull?.let {
                arguments += "--header-class-name"
                arguments += it
            }

            // Set output directory
            arguments += "-d"
            arguments += outputDir.get().toString()

            execute("${jextractPath.toAbsolutePath()} ${arguments.joinToString(" ")} ${lib.header.get()}")
        }
    }

    fun header(header: String, action: Action<Library>) {
        val definition = project.objects.newInstance<Library>()
        definition.header.set(header)
        action.execute(definition)
        libraries += definition
    }

    companion object {
        private fun execute(command: String) {
            println("executing: $command")
            // Create buffers for stdout and stderr streams
            val stdout = StringBuffer()
            val stderr = StringBuffer()
            val result = command.execute()

            // Wait until the process finishes and check if it succeeded
            result.waitForProcessOutput(stdout, stderr)
            println("stdout: $stdout")
            println("stderr: $stderr")
            println("result: $result")
            if (result.exitValue() != 0)
                throw GradleException("Invoking jextract failed.\n\n command: $command\n stdout: $stdout\n stderr: $stderr")
        }

        fun String.execute() = Runtime.getRuntime().exec(this)

        fun Process.waitForProcessOutput(output: Appendable?, error: Appendable?) {
            val tout = ProcessGroovyMethods.consumeProcessOutputStream(this, output)
            val terr = ProcessGroovyMethods.consumeProcessErrorStream(this, error)
            var interrupted = false
            try {
                try {
                    tout.join()
                } catch (var14: InterruptedException) {
                    interrupted = true
                }
                try {
                    terr.join()
                } catch (var13: InterruptedException) {
                    interrupted = true
                }
                try {
                    waitFor()
                } catch (var12: InterruptedException) {
                    interrupted = true
                }
                ProcessGroovyMethods.closeStreams(this)
            } finally {
                if (interrupted)
                    Thread.currentThread().interrupt()
            }
        }
    }
}
