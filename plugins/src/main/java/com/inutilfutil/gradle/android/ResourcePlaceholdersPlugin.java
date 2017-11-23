package com.inutilfutil.gradle.android;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.TestExtension;
import com.android.build.gradle.TestPlugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

public class ResourcePlaceholdersPlugin implements Plugin<Project> {
    public void apply(final Project project) {
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                BaseExtension android = getAndroidExtension(project);
                if (android != null) {
                    Map<String, String> placeholders = new HashMap<>(); //FIXME: use actual placeholders
                    android.registerTransform(new ResourcePlaceholdersTransform(placeholders));
                }
            }
        });
    }

    private static BaseExtension getAndroidExtension(Project project) {
        if (project.getPlugins().hasPlugin(LibraryPlugin.class)) {
            return project.getExtensions().getByType(LibraryExtension.class);
        } else if (project.getPlugins().hasPlugin(AppPlugin.class)) {
            return project.getExtensions().getByType(AppExtension.class);
        } else if (project.getPlugins().hasPlugin(TestPlugin.class)) {
            return project.getExtensions().getByType(TestExtension.class);
        }
        return null;
    }
}

