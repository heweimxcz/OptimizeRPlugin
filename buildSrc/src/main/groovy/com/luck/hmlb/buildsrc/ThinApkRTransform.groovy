package com.luck.hmlb.buildsrc

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
/**
 * 对R文件进行瘦身
 */
class ThinApkRTransform extends Transform {

    Project project
    ThinApkRExtension extension

    ThinApkRTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "ThinApkRTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        extension = project.extensions.getByName("thinRConfig")

        println "----------------------------------"
        println "------ThinApkRTransform start-----"

        //先删除原来的缓存文件
        outputProvider.deleteAll()
        RClassUtil.clear()

        //记录所有的jar包
        def jarList = []

        println "----第一次遍历，开始收集R类信息----"
        inputs.each { TransformInput input ->
            //第一次循环，只是为了收集 R.java 类信息
            input.directoryInputs.each { DirectoryInput directoryInput ->

                println("directoryInput--->" + directoryInput.file.absolutePath)
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        if (file.isFile()) {
                            //收集R.java类的信息
                            println("--->eachFileRecurse")
                            RClassUtil.collectRInfo(file)
                        }
                    }
                } else {
                    //收集R.java类的信息
                    RClassUtil.collectRInfo(directoryInput.file)
                }
            }

            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name
                def md5 = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = outputProvider.getContentLocation(jarName + md5, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                def src = jarInput.file
                FileUtils.copyFile(src, dest)

                //所有的jar包
                if (src.path.contains("com.squareup")) {
                    //TODO 有很多jar其实是不需要处理的
                } else {
                    jarList.add(dest)
                }
            }
        }
        println "----R类信息收集完毕----"

        println "----开始删除替换所有引用R.class的地方----"
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse {File file ->
                        if (file.isFile()) {
                            RClassUtil.replaceAndDeleteRInfo(file, extension)
                        }
                    }
                } else {
                    RClassUtil.replaceAndDeleteRInfo(directoryInput.file, extension)
                }

                def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }

        //处理 jar 包里的 class
        for (File jarFile : jarList) {
            println "处理Jar包里的R信息：${jarFile.getAbsolutePath()}"
            RClassUtil.replaceAndDeleteRInfoFromJar(jarFile, extension)
        }

        println "------ThinApkRTransform end-------"
        println "----------------------------------"
    }

}