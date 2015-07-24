package org.jenkinsci.plugins.drupal.builders;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.drupal.beans.DrushInvocation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Run Simpletest on Drupal.
 * 
 * @author Fengtan https://github.com/Fengtan/
 *
 */
public class SimpletestBuilder extends Builder {

    public final String uri;
    public final String root;
    public final String logs;
	
    @DataBoundConstructor
    public SimpletestBuilder(String uri, String root, String logs) {
        this.uri = uri;
        this.root = root;
        this.logs = logs;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    	// Make sure logs directory exists.
    	File logsDir = new File(build.getWorkspace().getRemote(), logs);
    	if (!logsDir.exists()) {
    		listener.getLogger().println("[DRUPAL] Creating logs directory "+logs);
    		logsDir.mkdir();
    	}

    	// Enable Simpletest if necessary.
    	File rootDir = new File(build.getWorkspace().getRemote(), root);
    	DrushInvocation drush = new DrushInvocation(new FilePath(rootDir), build.getWorkspace(), launcher, listener);
    	if (drush.isModuleInstalled("simpletest", true)) {
    		listener.getLogger().println("[DRUPAL] Simpletest is already enabled");
    	} else {
    		listener.getLogger().println("[DRUPAL] Simpletest is not enabled. Enabling Simpletest...");
    		drush.enable("simpletest");
    	}
    	
    	// Run Simpletest.
    	drush.testRun(logsDir, uri);

    	return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Load the persisted global configuration.
         */
        public DescriptorImpl() {
            load();
        }
        
        /**
         * This builder can be used with all kinds of project types.
         */
        public boolean isApplicable(Class<? extends AbstractProject> aClass) { 
            return true;
        }

        /**
         * Human readable name used in the configuration screen.
         */
        public String getDisplayName() {
            return "Run Simpletest on Drupal";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /**
         * Field 'root' should be a valid directory.
         */
        public FormValidation doCheckRoot(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (value.length() == 0) {
            	return FormValidation.warning("Workspace root will be used as Drupal root");
            }
            if (project != null) {
                return FilePath.validateFileMask(project.getSomeWorkspace(), value);
            }
        	return FormValidation.ok();
        }
        
        /**
         * Field 'logs' should not be empty.
         */
        public FormValidation doCheckLogs(@QueryParameter String value) throws IOException {
            if (value.length() == 0) {
            	return FormValidation.error("Please set a logs directory.");
            }
            return FormValidation.ok();
        }
        
    }
}

