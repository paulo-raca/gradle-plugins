package com.inutilfutil.gradle.android;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;

import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public abstract class OneToOneTransform extends Transform {
    public boolean isIncremental() {
        return true;
    }

    public void transformFile(String path, InputStream in, OutputStream out) throws TransformException, IOException {
        System.err.println("Transforming " + path);
        IOUtils.copy(in, out);
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        if (!transformInvocation.isIncremental()) {
            transformInvocation.getOutputProvider().deleteAll();
        }

        for (TransformInput input : transformInvocation.getInputs()) {
            for (JarInput jarInput : input.getJarInputs()) {
                System.out.println("Processando jar " + jarInput.getFile());
                File inputJarFile = jarInput.getFile();
                File outputJarFile = transformInvocation.getOutputProvider().getContentLocation(
                        jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);

                Status status = transformInvocation.isIncremental() ? jarInput.getStatus() : Status.ADDED;
                if (status == Status.NOTCHANGED) {
                    // Ignore
                } else if (status == Status.REMOVED) {
                    outputJarFile.delete();
                } else {
                    try (
                        JarFile inputJar = new JarFile(inputJarFile);
                        JarOutputStream outputJar = new JarOutputStream(new FileOutputStream(outputJarFile));
                    ) {
                        for (Enumeration<JarEntry> inputEntries = inputJar.entries(); inputEntries.hasMoreElements(); ) {
                            JarEntry entry = inputEntries.nextElement();
                            outputJar.putNextEntry(entry);

                            try (
                                    InputStream in = inputJar.getInputStream(entry);
                                    OutputStream out = new NonClosingOutputStream(outputJar)
                            ) {
                                transformFile(entry.getName(), in, out);
                            }

                        }
                    }
                }
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                System.out.println("Processando pasta " + directoryInput.getFile());
                File inDir = directoryInput.getFile();
                File outDir = transformInvocation.getOutputProvider().getContentLocation(
                        directoryInput.getName(),
                        directoryInput.getContentTypes(),
                        directoryInput.getScopes(),
                        Format.DIRECTORY);
                outDir.mkdirs();

                Iterable<Map.Entry<File, Status>> changedFiles;
                if (transformInvocation.isIncremental()) {
                    changedFiles = directoryInput.getChangedFiles().entrySet();
                } else {
                    changedFiles = nonIncrementalFiles(inDir);
                }

                for (Map.Entry<File, Status> entry : changedFiles) {
                    File srcFile = entry.getKey();
                    Path relativePath = inDir.toPath().relativize(srcFile.toPath());
                    File destFile = outDir.toPath().resolve(relativePath).toFile();

                    //System.err.println(relativePath + " -> " + entry.getValue());

                    if (entry.getValue() == Status.NOTCHANGED) {
                        // Ignore
                    } else if (entry.getValue() == Status.REMOVED) {
                        destFile.delete();
                    } else {
                        destFile.getParentFile().mkdirs();
                        try (
                            InputStream in =  new FileInputStream(srcFile);
                            OutputStream out = new FileOutputStream(destFile);
                        ) {
                            transformFile(relativePath.toString(), in, out);
                        }
                    }
                }
            }
        }
    }


    private static Iterable<Map.Entry<File, Status>> nonIncrementalFiles(File dir) throws IOException {
        List<Map.Entry<File, Status>> ret = new ArrayList<>();
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                ret.add(new AbstractMap.SimpleEntry<File, Status>(path.toFile(), Status.ADDED));
                return FileVisitResult.CONTINUE;
            }
        });
        return ret;
    }


    private static class NonClosingOutputStream extends FilterOutputStream {
        public NonClosingOutputStream(OutputStream os) {
            super(os);
        }
        @Override
        public void close() throws IOException {
            //Ignore
        }
    }
}
