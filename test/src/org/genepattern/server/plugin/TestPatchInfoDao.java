package org.genepattern.server.plugin;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.DbUtil.DbType;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.server.plugin.dao.PatchInfoDao;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestPatchInfoDao {
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
    }
    
    @Ignore // <---- just for manually testing MySQL db initialization
    @Test
    public void initMysqlDb() throws Throwable {
        DbUtil.initDb(DbType.MYSQL);
        File gpResourcesDir=new File("resources");
        String dbSchemaPrefix="analysis_mysql-";
        String gpVersion="3.9.2";
        HsqlDbUtil.updateSchema(gpResourcesDir, dbSchemaPrefix, gpVersion);
        
        List<PatchInfo> expected = TestPluginRegistrySystemProps.initDefaultInstalledPatchInfos();
        assertEquals("before record patch, expecting default entries",
                expected,
                new PatchInfoDao().getInstalledPatches());
    }
        
    @Test
    public void recordPatch() throws DbException, MalformedURLException {
        // test one, default entries 
        List<PatchInfo> defaultEntries = TestPluginRegistrySystemProps.initDefaultInstalledPatchInfos();
        assertEquals("before recordPatch, expecting default list", defaultEntries, new PatchInfoDao().getInstalledPatches());

        // test two, record patch
        final String BWA="urn:lsid:broadinstitute.org:plugin:BWA_0_7_4:2";
        new PatchInfoDao().recordPatch(new PatchInfo(BWA));
        List<PatchInfo> expected = new ArrayList<PatchInfo>(defaultEntries);
        expected.add(new PatchInfo(BWA));
        assertEquals(
            "expecting a new entry after recording patch", 
            // expected
            expected, 
            // actual
            new PatchInfoDao().getInstalledPatches());
        
        // test three, duplicate lsid, update the entry
        new PatchInfoDao().recordPatch(new PatchInfo(BWA));
        assertEquals(
            "expecting no new entry after recording a patch update", 
            // expected
            expected, 
            // actual
            new PatchInfoDao().getInstalledPatches());
        
        // test four, delete record
        boolean success=new PatchInfoDao().removePatch(new PatchInfo(BWA));
        assertEquals("Expecting successful remove patch", true, success);
        assertEquals(
            "after removePatch, expecting the default list",
            // expected
            defaultEntries, 
            // actual
            new PatchInfoDao().getInstalledPatches());
    }

}