package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;

public class RubyMotionCommandLauncher {
    private final AbstractBuild build;
    private final Launcher launcher;
    private final BuildListener listener;

    public RubyMotionCommandLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public String getProjectWorkspace() {
        return build.getWorkspace().getRemote();
    }

    public FilePath getWorkspaceFilePath(String fileName) {
        if (build.getWorkspace().isRemote()) {
            return new FilePath(build.getWorkspace().getChannel(), build.getWorkspace().getRemote() + "/" + fileName);
        }
        return new FilePath(build.getWorkspace(), fileName);
    }

    public boolean exec(String command) {
        command = "bash -c \"" + command + "\"";
        try {
            int r = launcher.launch()
                .cmdAsSingleString(command)
                .envs(build.getEnvironment(listener))
                .stdout(listener.getLogger())
                .pwd(getProjectWorkspace())
                .join();
            return r == 0;
        }
        catch (Exception e) {
            e.printStackTrace();
            listener.getLogger().println("Exception !");
            return false;
        }
    }

    public boolean exec(String command, OutputStream outputStream) {
        command = "bash -c \"" + command + "\"";
        try {
            int r = launcher.launch()
                .cmdAsSingleString(command)
                .envs(build.getEnvironment(listener))
                .stdout(outputStream)
                .stderr(listener.getLogger())
                .pwd(getProjectWorkspace())
                .join();
            return r == 0;
        }
        catch (Exception e) {
            e.printStackTrace();
            listener.getLogger().println("Exception !");
            return false;
        }
    }

    public void removeFile(String file) {
        FilePath fp;
        fp = getWorkspaceFilePath(file);
        try {
            if (fp.exists()) {
                fp.delete();
            }
        }
        catch (Exception e) {
        }
    }

    public void printLog(String string) {
        listener.getLogger().println(string);
    }
 }
