/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class RunTaskBean {

    private boolean visualizer;
    private boolean pipeline;
    private String name;
    private String lsid;
    private String[] documentationFilenames;
    private Parameter[] parameters;
    private String version;
    private boolean missing;
    private static Logger log = Logger.getLogger(RunTaskBean.class);

    public RunTaskBean() {
        setTask("PreprocessDataset"); // FIXME

    }

    public String getFormAction() {
        if (visualizer) {
            return "preRunVisualizer.jsp";

        }
        else if (pipeline) {
            return "runPromptingPipeline.jsp";
        }
        return "runTaskPipeline.jsp";
    }

    public String[] getDocumentationFilenames() {
        return documentationFilenames;
    }

    public String getLsid() {
        return lsid;
    }

    public String getName() {
        return name;
    }

    public boolean isPipeline() {
        return pipeline;
    }

    public boolean isVisualizer() {
        return visualizer;
    }

    public static class DefaultValueSelectItem extends SelectItem {
        private boolean defaultOption;

        public DefaultValueSelectItem(String value, String label, boolean defaultOption) {
            super(value, label);
            this.defaultOption = defaultOption;
        }

        public boolean isDefaultOption() {
            return defaultOption;
        }

        public String getSelected() {
            return defaultOption ? "true" : "false";
        }
    }

    public static class Parameter {

        private DefaultValueSelectItem[] choices;
        private boolean optional;
        private String displayDesc;
        private String displayName;
        private String defaultValue;
        private String inputType;
        private String name;

        private Parameter(ParameterInfo pi) {
            HashMap pia = pi.getAttributes();
            this.optional = ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
                    .length() > 0;
            this.defaultValue = (String) pia.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

            if (defaultValue == null) {
                defaultValue = "";
            }
            defaultValue = defaultValue.trim();

            String[] choicesArray = pi.hasChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER) ? pi
                    .getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER) : null;
            if (choicesArray != null) {
                choices = new DefaultValueSelectItem[choicesArray.length];
                for (int i = 0; i < choicesArray.length; i++) {
                    String choice = choicesArray[i];
                    String display, option;

                    int equalsCharIndex = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
                    if (equalsCharIndex == -1) {
                        display = choice;
                        option = choice;
                    }
                    else {
                        option = choice.substring(0, equalsCharIndex);
                        display = choice.substring(equalsCharIndex + 1);
                    }
                    display = display.trim();
                    option = option.trim();
                    boolean defaultOption = defaultValue.equals(display) || defaultValue.equals(option);
                    choices[i] = new DefaultValueSelectItem(option, display, defaultOption);
                }
            }
            if (pi.isPassword()) {
                inputType = "password";
            }
            else if (pi.isInputFile()) {
                inputType = "file";
            }
            else if (choices != null && choices.length > 0) {
                inputType = "select";
            }
            else {
                inputType = "text";
            }

            this.displayDesc = (String) pia.get("altDescription");
            if (displayDesc == null) {
                displayDesc = pi.getDescription();
            }
            this.displayName = (String) pia.get("altName");
            if (displayName == null) {
                displayName = pi.getName();
                displayName = displayName.replaceAll("\\.", " ");
            }
            this.name = pi.getName();
        }

        public DefaultValueSelectItem[] getChoices() {
            return choices;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDisplayDescription() {
            return displayDesc;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isOptional() {
            return optional;
        }

        public String getInputType() {
            return inputType;
        }

    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        this.parameters = parameters;
    }

    public String getVersion() {
        return version;
    }

    public void setTask(String taskNameOrLsid) {
        TaskInfo taskInfo = null;
        try {
            taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(taskNameOrLsid);
        }
        catch (WebServiceException e) {
            log.error(e);
        }
        if (taskInfo == null) {
            missing = true;
            return;
        }
        missing = false;
        ParameterInfo[] pi = taskInfo.getParameterInfoArray();
        this.parameters = new Parameter[pi != null ? pi.length : 0];
        if (pi != null) {
            for (int i = 0; i < pi.length; i++) {
                parameters[i] = new Parameter(pi[i]);
            }
        }
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();

        String taskType = tia.get("taskType");
        this.visualizer = "visualizer".equalsIgnoreCase(taskType);
        this.pipeline = "pipeline".equalsIgnoreCase(taskType);
        this.name = taskInfo.getName();
        this.lsid = taskInfo.getLsid(); 
        try {
            this.version = new LSID(lsid).getVersion();
        }
        catch (MalformedURLException e) {
            log.error("LSID:" + lsid, e);
        }
        File[] docFiles = null;
        try {
            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
            docFiles = taskIntegratorClient.getDocFiles(taskInfo);
        }
        catch (WebServiceException e) {
            log.error(e);
        }
        this.documentationFilenames = new String[docFiles != null ? docFiles.length : 0];
        if (docFiles != null) {
            for (int i = 0; i < docFiles.length; i++) {
                documentationFilenames[i] = docFiles[i].getName();
            }
        }

    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }

}
