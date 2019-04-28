package com.luck.hmlb.buildsrc

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project


class OptimizeRPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {

        println("--->OptimizeRPlugin")

        def android = project.extensions.getByType(AppExtension)

        project.extensions.create("thinRConfig", ThinApkRExtension, project)

        project.android.registerTransform(new ThinApkRTransform(project))


    }
}