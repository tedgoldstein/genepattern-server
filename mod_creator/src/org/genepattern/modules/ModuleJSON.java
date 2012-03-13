package org.genepattern.modules;

import org.json.JSONObject;
import org.json.JSONException;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Mar 12, 2012
 * Time: 11:08:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ModuleJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ModuleJSON.class);

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String AUTHOR = "author";
    public static final String PRIVACY = "privacy";
    public static final String QUALITY = "quality";
    public static final String VERSION_COMMENT = "version";
    public static final String LSID = "lsid";
    public static final String CATEGORY = "taskType";
    public static final String CPU = "cpuType";
    public static final String OS = "os";
    public static final String COMMAND_LINE = "commandLine";    
    public static final String FILES = "supportFiles";

    public static final String KEY = "module";

    public ModuleJSON(JSONObject object) {
        try {
            this.put(NAME, object.get(NAME));
            this.put(DESCRIPTION, object.get(DESCRIPTION));
            this.put(AUTHOR, object.get(AUTHOR));
            this.put(PRIVACY, object.get(PRIVACY));
            this.put(VERSION_COMMENT, object.get(VERSION_COMMENT));
            this.put(CATEGORY, object.get(CATEGORY));                        
            this.put(CPU, object.get(CPU));


            this.put(OS, object.get(OS));
            this.put(COMMAND_LINE, object.get(COMMAND_LINE));

            //this.put(LSID, object.get(LSID));
            //this.put(FILES, object.get(FILES));
        }
        catch (JSONException e) {
            log.error(e);
            log.error("Unable to create ModuleJSON from generic JSONObject");
        }
    }

    public static JSONObject parseBundle(String bundle)
    {
        JSONObject pipelineJSON = null;
        try {
            pipelineJSON = new JSONObject(bundle);
        }
        catch (JSONException e) {
            log.error("Error parsing JSON in the saved bundle");
        }
        return pipelineJSON;
    }

    public static ModuleJSON extract(JSONObject json) {
        try {
            JSONObject object = (JSONObject) json.get(ModuleJSON.KEY);
            return new ModuleJSON(object);
        }
        catch (JSONException e) {
            log.error("Unable to extract ModuleJSON from saved bundle");
            return null;
        }
    }

    public String getLsid() throws JSONException {
        return this.getString(LSID);
    }

    public void setLsid(String lsid) throws JSONException {
        this.put(LSID, lsid);
    } 

    public String getDescription() throws JSONException {
        return this.getString(DESCRIPTION);
    }

    public void setDescription(String description) throws JSONException {
        this.put(DESCRIPTION, description);
    }

    public String getName() throws JSONException {
        return this.getString(NAME);
    }

    public void setName(String name) throws JSONException {
        this.put(NAME, name);
    }
}