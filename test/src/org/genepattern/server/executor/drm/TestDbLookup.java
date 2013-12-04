package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit test cases for creating, updating, and deleting entries from the 'job_runner_job' table.
 * @author pcarr
 *
 */
public class TestDbLookup {
    private static final String jobRunnerClassname=LocalQueuingSystem.class.getName();
    private static final String jobRunnerName="LocalQueuingSystem-1";

    @BeforeClass
    public static void beforeClass() throws Exception{
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb();
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    @Test
    public void testCreate() {
        final Integer gpJobNo=0;
        final File workingDir=new File("jobResults/"+gpJobNo);
        
        DbLookup dbLookup = new DbLookup(jobRunnerClassname, jobRunnerName);
        dbLookup.insertDrmRecord(workingDir, gpJobNo);
        
        List<JobRunnerJob> all=dbLookup.getAll();
        Assert.assertEquals("all.size", 1, all.size());
    }
    
    @Test
    public void testQuery() {
        DbLookup dbLookup=new DbLookup(jobRunnerClassname, jobRunnerName);
        List<String> runningJobIds=dbLookup.getRunningDrmJobIds();
        Assert.assertEquals("num running jobs", 1, runningJobIds.size());
    }

}
