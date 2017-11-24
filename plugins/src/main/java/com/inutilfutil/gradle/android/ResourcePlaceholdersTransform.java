package com.inutilfutil.gradle.android;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.api.BaseVariant;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourcePlaceholdersTransform extends OneToOneTransform {
    private Map<String, String> placeholders;

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
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        BaseVariant variant = getVariant(transformInvocation.getContext());
        this.placeholders = new HashMap<>();
        this.placeholders.putAll((Map)variant.getMergedFlavor().getManifestPlaceholders());
        this.placeholders.put("applicationId", variant.getApplicationId());
        super.transform(transformInvocation);
    }

    @Override
    public void transformFile(String path, InputStream in, OutputStream out) throws TransformException, IOException {
        if (!path.endsWith(".class")) {
            super.transformFile(path, in, out);
            return;
        }

        System.out.println("Transforming XML " + path);

        //FIXME: It treats string as ISO-8859-1 so that binary shit doesn't break
        //UTF-8 is used on the placeholder / replaced values, so re-encoding is necessary sometimes.
        String originalString = IOUtils.toString(in, "ISO-8859-1");
        StringBuffer resultString = new StringBuffer();
        int changedCount = 0;
        Matcher regexMatcher = Pattern.compile("\\$\\{(.*)\\}").matcher(originalString);
        while (regexMatcher.find()) {
            changedCount++;
            String key = new String(regexMatcher.group(1).getBytes("ISO-8859-1"), "UTF-8");
            String replacement = placeholders.get(key);
            if (replacement == null) {
                replacement = regexMatcher.group();
                System.err.println("Skipping unknown placeholder " + replacement + " on " + path);
            }
            System.out.println("Placeholder replaced: " + regexMatcher.group() + " -> " + replacement);
            regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(new String(replacement.getBytes("UTF-8"), "ISO-8859-1")));
        }
        regexMatcher.appendTail(resultString);

        if (changedCount > 0) {
            System.out.println("Replaced " + changedCount + " placedholders on " + path);
        }
        out.write(resultString.toString().getBytes("ISO-8859-1"));
    }
}
