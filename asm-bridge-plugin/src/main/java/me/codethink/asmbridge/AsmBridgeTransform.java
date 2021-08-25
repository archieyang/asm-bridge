package me.codethink.asmbridge;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;


public class AsmBridgeTransform extends Transform {
    private final AsmBridgeExtension extension;
    public AsmBridgeTransform(Project project) {
        extension = project.getExtensions().create("asmBridge", AsmBridgeExtension.class);
    }


    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(final TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();

        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {

                transformJarInputs(jarInput, outputProvider);
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                transformDirectory(directoryInput.getFile(), dest);
            }
        }

        super.transform(transformInvocation);
    }

    private void transformJarInputs(final JarInput jarInput, final TransformOutputProvider outputProvider) throws IOException {
        if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
            String jarName = jarInput.getName();
            String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());

            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4);
            }

            JarFile jarFile = new JarFile(jarInput.getFile());


            Enumeration<JarEntry> enumeration = jarFile.entries();

            File tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_temp.jar");

            if (tmpFile.exists()) {
                tmpFile.delete();
            }

            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);

                if (entryName.endsWith(".class") && !entryName.startsWith("R$") && !"R.class".equals(entryName)) {

                    jarOutputStream.putNextEntry(zipEntry);
                    ClassReader classReader =  new ClassReader(IOUtils.toByteArray(inputStream));
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = createClassVisitor(classWriter);
                    classReader.accept(cv, EXPAND_FRAMES);
                    byte[] code = classWriter.toByteArray();
                    jarOutputStream.write(code);
                } else {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }

            jarOutputStream.close();
            jarFile.close();
            File dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            FileUtils.copyFile(tmpFile, dest);
            tmpFile.delete();
        }
    }


    private void transformDirectory(final File input, final File dest) throws IOException {
        if (dest.exists()) {
            FileUtils.forceDelete(dest);
        }
        FileUtils.forceMkdir(dest);
        String srcDirPath = input.getAbsolutePath();
        String destDirPath = dest.getAbsolutePath();
        for (File file : input.listFiles()) {
            String destFilePath = file.getAbsolutePath().replace(srcDirPath, destDirPath);
            File destFile = new File(destFilePath);
            if (file.isDirectory()) {
                transformDirectory(file, destFile);
            } else if (file.isFile()) {
                FileUtils.touch(destFile);

                InputStream inputStream = new FileInputStream(file.getAbsolutePath());
                ClassReader cr = new ClassReader(inputStream);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                ClassVisitor adapter = createClassVisitor(cw);
                cr.accept(adapter, 0);
                OutputStream fos = new FileOutputStream(destFile.getAbsolutePath());
                fos.write(cw.toByteArray());
                fos.close();

                FileUtils.copyFile(file, destFile);
            }
        }
    }

    private ClassVisitor createClassVisitor(final ClassVisitor classVisitor) {
        try {
            return (ClassVisitor) AsmBridgeTransform.class.getClassLoader().loadClass(extension.implementation).getConstructor(ClassVisitor.class).newInstance(classVisitor);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
