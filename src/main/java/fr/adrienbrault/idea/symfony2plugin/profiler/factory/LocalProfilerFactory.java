package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.LocalProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalProfilerFactory implements ProfilerFactoryInterface {
    @Nullable
    @Override
    public ProfilerIndexInterface createProfilerIndex(@NotNull Project project) {
        File csvIndex = findCsvProfilerFile(project);
        if (csvIndex == null) {
            return null;
        }

        String profilerLocalUrl = Settings.getInstance(project).profilerLocalUrl;

        // fine: user defined base url use this one
        // else no profiler given let profiler try to find url
        String profilerUrl = null;
        if (StringUtils.isNotBlank(profilerLocalUrl)) {
            profilerUrl = profilerLocalUrl;
        }

        return new LocalProfilerIndex(csvIndex, profilerUrl);
    }

    /**
     * find csv on settings or container configuration
     */
    @Nullable
    private File findCsvProfilerFile(@NotNull Project project) {
        String profilerCsvPath = Settings.getInstance(project).profilerCsvPath;
        if (StringUtils.isBlank(profilerCsvPath)) {
            return getCsvIndex(project);
        }

        VirtualFile relativeFile = VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), profilerCsvPath.replace("\\", "/").split("/"));
        if (relativeFile != null) {
            return VfsUtil.virtualToIoFile(relativeFile);
        }

        return null;
    }

    @Nullable
    protected File getCsvIndex(@NotNull Project project) {
        for(File file: Symfony2ProjectComponent.getContainerFiles(project)) {
            if(file.exists()) {
                File csvFile = new File(file.getParentFile().getPath() + "/profiler/index.csv");
                if (csvFile.exists()) {
                    return csvFile;
                }
            }
        }

        return null;
    }

    @Override
    public boolean accepts(@NotNull Project project) {
        return Settings.getInstance(project).profilerLocalEnabled;
    }
}
