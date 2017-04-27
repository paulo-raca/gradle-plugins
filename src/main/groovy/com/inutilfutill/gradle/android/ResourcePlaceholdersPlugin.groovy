package com.inutilfutil.gradle.android

import org.gradle.api.Project
import org.gradle.api.Plugin

class ResourcePlaceholdersPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.afterEvaluate {
            //Recursively apply to subprojects
            project.childProjects.values().each { childProject ->
                apply(childProject)
            }

            // Hook placeholder replacement in resource generation
            if (project.hasProperty('android') && Class.forName("com.android.build.gradle.AppExtension").isInstance(project.android)) {
                println("Enabling resource placeholders on ${project.name}");

                project.android.applicationVariants.all { variant ->
                    variant.outputs.each { output ->
                        output.processResources.doFirst {
                            println("Replacing placeholders in XML resource files")

                            def placeholders = variant.mergedFlavor.manifestPlaceholders + [applicationId: variant.applicationId]
                            def files = project.fileTree(resDir).include('**/*.xml')

                            files.each { file ->
                                def content = file.getText('UTF-8')
                                def new_content = content

                                placeholders.each { entry ->
                                    new_content = new_content.replaceAll("\\\$\\{${entry.key}\\}", entry.value)
                                }

                                if (new_content != content) {
                                    println(" - ${file}")
                                    file.write(new_content, 'UTF-8')
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

