package com.inutilfutil.gradle.android;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformException;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourcePlaceholdersTransform extends OneToOneTransform {
    private final Map<String, String> placeholders;

    public ResourcePlaceholdersTransform(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    @Override
    public String getName() {
    return "ResourcePlaceholders";
}

    public Set<QualifiedContent.ContentType> getInputTypes() {
        //FIXME: Resources are never transformed -- But it works with CLASSES
        return ImmutableSet.of(QualifiedContent.DefaultContentType.RESOURCES);
    }

    public Set<? super QualifiedContent.Scope> getScopes() {
        return ImmutableSet.of(QualifiedContent.Scope.PROJECT, QualifiedContent.Scope.SUB_PROJECTS, QualifiedContent.Scope.EXTERNAL_LIBRARIES);
    }

    @Override
    public void transformFile(String path, InputStream in, OutputStream out) throws TransformException, IOException {
        if (!path.endsWith(".class")) {
            super.transformFile(path, in, out);
            return;
        }

        System.out.println("Transforming XML " + path);

        String originalString = IOUtils.toString(in, "UTF-8");
        StringBuffer resultString = new StringBuffer();
        int changedCount = 0;
        Matcher regexMatcher = Pattern.compile("\\$\\{(.*)\\}").matcher(originalString);
        while (regexMatcher.find()) {
            changedCount++;
            System.out.println("Found match: " + regexMatcher.group() + " / " + regexMatcher.group(1));
            String key = regexMatcher.group(1);
            String replacement = placeholders.get(key);
            if (replacement == null) {
                replacement = regexMatcher.group();
                System.err.println("Skipping unknown placeholder " + replacement + " on " + path);
            }
            regexMatcher.appendReplacement(resultString, replacement);
        }
        regexMatcher.appendTail(resultString);

        if (changedCount > 0) {
            System.out.println("Replaced " + changedCount + " placedholders on " + path);
        }
        out.write(resultString.toString().getBytes("UTF-8"));
    }
}
