/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

public class AuthorizationBean {
    public boolean isAdminJobsAllowed() {
	return AuthorizationHelper.adminJobs();
    }

    public boolean isAdminSuitesAllowed() {
	return AuthorizationHelper.adminSuites();
    }

    public boolean isBrowseServerFileSystemAllowed() {
	return "true".equals(System.getProperty("allow.input.file.paths")) && AuthorizationHelper.adminServer();
    }

    public boolean isAdminServerAllowed() {
	return AuthorizationHelper.adminServer();
    }

    public boolean isCreateModuleAllowed() {
	return AuthorizationHelper.createModule();
    }

    public boolean isCreatePrivatePipelineAllowed() {
	return AuthorizationHelper.createPrivatePipeline();
    }

    public boolean isCreatePublicPipelineAllowed() {
	return AuthorizationHelper.createPublicPipeline();
    }

    public boolean isCreatePrivateSuiteAllowed() {
	return AuthorizationHelper.createPrivateSuite();
    }

    public boolean isCreatePublicSuiteAllowed() {
	return AuthorizationHelper.createPublicSuite();
    }
}
