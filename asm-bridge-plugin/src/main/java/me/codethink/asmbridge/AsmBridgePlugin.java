package me.codethink.asmbridge;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AsmBridgePlugin implements Plugin<Project> {
    @Override
    public void apply(final Project project) {
        project.getExtensions().getByType(AppExtension.class).registerTransform(new AsmBridgeTransform(project));
    }

}