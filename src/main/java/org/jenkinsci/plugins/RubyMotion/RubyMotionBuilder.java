package org.jenkinsci.plugins.RubyMotion;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

public class RubyMotionBuilder extends Builder {

    private final String platform;
    private final String rakeTask;
    private final String deviceFamily;
    private final String retina;
    private final String outputStyle;
    private final String simulatorVersion;
    private final String outputFileName;
    private final boolean useBundler;
    private final boolean needClean;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RubyMotionBuilder(String platform, String rakeTask, String deviceFamily, String retina, String outputStyle, String simulatorVersion, String outputFileName, boolean useBundler, boolean needClean) {
        this.platform = platform;
        this.rakeTask = rakeTask;
        this.deviceFamily = deviceFamily;
        this.retina = retina;
        this.outputStyle = outputStyle;
        this.simulatorVersion = simulatorVersion;
        this.outputFileName = outputFileName;
        this.useBundler = useBundler;
        this.needClean = needClean;
    }

    public String getPlatform() {
        return platform;
    }

    public String getRakeTask() {
        return rakeTask;
    }

    public String getDeviceFamily() {
        return deviceFamily;
    }

    public String getRetina() {
        return retina;
    }

    public String getOutputStyle() {
        return outputStyle;
    }

    public String getSimulatorVersion() {
        return simulatorVersion;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public boolean getUseBundler() {
        return useBundler;
    }

    public boolean getNeedClean() {
        return needClean;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean result;

        if (useBundler) {
            String cmds = "bundle install";
            result = exec(cmds, build, launcher, listener);
            if (!result) {
                return false;
            }
        }

        if (needClean) {
            String cmds = "";
            if (useBundler) {
                cmds = "bundle exec ";
            }
            cmds = cmds + "rake clean";
            result = exec(cmds, build, launcher, listener);
            if (!result) {
                return false;
            }
        }

        if (platform.equals("ios")) {
            return execiOS(build, launcher, listener);
        }
        else if (platform.equals("osx")) {
            return execOSX(build, launcher, listener);
        }
        return false;
    }

    private boolean execOSX(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String cmds = "rake ";
        if (getUseBundler()) {
            cmds = "bundle exec rake ";
        }

        cmds = cmds + rakeTask;
        cmds = cmds + " output=" + outputStyle;

        String output = build.getProject().getWorkspace() + "/" + outputFileName;
        File outputFile = new File(output);
        if (outputFile.exists()) {
            outputFile.delete();
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            int r = launcher.launch(cmds, build.getEnvVars(), outputStream, build.getProject().getWorkspace()).join();
            return r == 0;
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException !");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException !");
            return false;
        }
    }

    private boolean execiOS(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String cmds = "rake ";
        if (getUseBundler()) {
            cmds = "bundle exec rake ";
        }

        cmds = cmds + rakeTask;
        if (simulatorVersion.length() > 0) {
            cmds = cmds + " target=" + simulatorVersion;
        }
        if (retina.length() > 0) {
            if (simulatorVersion.length() > 0) {
                cmds = cmds + " retina=" + retina;
            }
        }
        cmds = cmds + " device_family=" + deviceFamily;
        cmds = cmds + " output=" + outputStyle;

        String output = build.getProject().getWorkspace() + "/" + outputFileName;
        String error  = build.getProject().getWorkspace() + "/.jenkins-error";
        File outputFile = new File(output);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        File errorFile = new File(output);
        if (outputFile.exists()) {
            errorFile.delete();
        }
        cmds = cmds + " SIM_STDOUT_PATH=" + output + " SIM_STDERR_PATH=" + error;

        exec(cmds, build, launcher, listener);
        return true;
    }

    private boolean exec(String command, AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            int r = launcher.launch(command, build.getEnvVars(), listener.getLogger(), build.getProject().getWorkspace()).join();
            return r == 0;
        } catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("IOException !");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.getLogger().println("InterruptedException !");
            return false;
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "RubyMotion";
        }

        public ListBoxModel doFillPlatformItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("iOS Platform",  "ios");
            items.add("OS X Platform", "osx");
            return items;
        }

        public ListBoxModel doFillRakeTaskItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("spec", "spec");
            return items;
        }

        public ListBoxModel doFillDeviceFamilyItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("iPhone", "iphone");
            items.add("iPad",   "ipad");
            return items;
        }

        public ListBoxModel doFillRetinaItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("off",           "false");
            items.add("on",            "true");
            items.add("on (3.5-inch)", "3.5");
            items.add("on (4-inch)",   "4");
            return items;
        }

        public ListBoxModel doFillOutputStyleItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("tap", "tap");
            // items.add("spec_dox", "spec_dox");
            // items.add("fast", "fast");
            // items.add("test_unit", "test_unit");
            // items.add("knock", "knock");
            // items.add("colorized", "colorized");
            return items;
        }
    }
}
