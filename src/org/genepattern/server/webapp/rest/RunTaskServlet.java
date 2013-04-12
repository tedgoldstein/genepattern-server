package org.genepattern.server.webapp.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.data.pipeline.PipelineDependencyHelper;
import org.genepattern.modules.ModuleJSON;
import org.genepattern.modules.ParametersJSON;
import org.genepattern.modules.ResponseJSON;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.domain.Lsid;
import org.genepattern.server.job.input.BatchInputHelper;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Jan 10, 2013
 * Time: 9:41:34 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/RunTask")
public class RunTaskServlet extends HttpServlet
{
    public static Logger log = Logger.getLogger(RunTaskServlet.class);
    /*public static final String UPLOAD = "/upload";
    public static final String RUN = "/run";
    */

    /**
	 * Inject details about the URI for this request
	 */
	@Context
    UriInfo uriInfo;
	
    @GET
    @Path("/load")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadModule(
            @QueryParam("lsid") String lsid, 
            @QueryParam("reloadJob") String reloadJobId, 
            @QueryParam("_file") String sendFromFile,
            @QueryParam("_format") String sendFromFormat,
            @Context HttpServletRequest request)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");

            if (username == null)
            {
                throw new Exception("User not logged in");
            }

            ServerConfiguration.Context context = ServerConfiguration.Context.getContextForUser(username);
            JobInput reloadJobInput = null;

            if (lsid == null && reloadJobId == null)
            {
                throw new Exception ("No lsid or job number to reload received");
            }

            if(reloadJobId != null && !reloadJobId.equals(""))
            {
                //This is a reloaded job
                reloadJobInput= ParamListHelper.getInputValues(context, reloadJobId);

                String reloadedLsidString = reloadJobInput.getLsid();

                //check if lsid is null
                if(lsid == null)
                {
                    lsid = reloadedLsidString;
                }
                else
                {
                    //warn the user if the reloaded job lsid and given lsid do not match
                    //but continue execution
                    Lsid reloadLsid = new Lsid(reloadedLsidString);
                    Lsid givenLsid = new Lsid(lsid);
                    if(reloadLsid.getLsidNoVersion().equals(givenLsid.getLsidNoVersion()))
                    {
                        log.warn("The given lsid " + givenLsid.getLsidNoVersion() + " does not match " +
                                "the lsid of the reloaded job " + reloadLsid.getLsidNoVersion());
                    }
                }

            }

            //check if lsid is still null
            if(lsid == null)
            {
                throw new Exception ("No lsid  received");
            }

            TaskInfo taskInfo = getTaskInfo(lsid, username);

            if(taskInfo == null)
            {
                throw new Exception("No task with task id: " + lsid + " found " +
                        "for user " + username);
            }

            ModuleJSON moduleObject = new ModuleJSON(taskInfo, null);
            moduleObject.put("lsidVersions", new JSONArray(getModuleVersions(taskInfo)));

            //check if user is allowed to edit the module
            boolean createModuleAllowed = AuthorizationHelper.createModule(username);
            boolean editable = createModuleAllowed && taskInfo.getUserId().equals(username)
                    && LSIDUtil.getInstance().isAuthorityMine(taskInfo.getLsid());
            moduleObject.put("editable", editable);

            //check if the user is allowed to view the module
            boolean isViewable = true;

            //check if the module has documentation
            boolean hasDoc = true;

            File[] docFiles = null;
            try {
                LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);
                docFiles = taskIntegratorClient.getDocFiles(taskInfo);

                if(docFiles == null || docFiles.length == 0)
                {
                    hasDoc = false;
                }
            }
            catch (WebServiceException e) {
                log.error("Error getting doc files.", e);
            }
            moduleObject.put("hasDoc", hasDoc);

            //if this is a pipeline check if there are any missing dependencies
            TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
            String taskType = tia.get(GPConstants.TASK_TYPE);
            boolean isPipeline = "pipeline".equalsIgnoreCase(taskType);
            if(isPipeline && PipelineDependencyHelper.instance().getMissingDependenciesRecursive(taskInfo).size() != 0)
            {
                moduleObject.put("missing_tasks", true);
            }
            else
            {
                moduleObject.put("missing_tasks", false);                        
            }
            JSONObject responseObject = new JSONObject();
            responseObject.put(ModuleJSON.KEY, moduleObject);

            JSONArray parametersObject = getParameterList(taskInfo.getParameterInfoArray());
            responseObject.put(ParametersJSON.KEY, parametersObject);


            //set initial values for the parameters for the following cases:
            //   1) a reloaded job
            //   2) values set in request parameters, when linking from the protocols page
            //   3) send to module, from the context menu for a file
            String _fileParam=null;
            String _formatParam=null;
            final Map<String,String[]> parameterMap=request.getParameterMap();
            if (parameterMap.containsKey("_file")) {
                _fileParam=parameterMap.get("_file")[0];
                if (parameterMap.containsKey("_format")) {
                    _formatParam=parameterMap.get("_format")[0];
                }
            } 
            JSONObject initialValues=ParamListHelper.getInitialValuesJson(
                    taskInfo.getParameterInfoArray(), 
                    reloadJobInput, 
                    _fileParam, 
                    _formatParam, 
                    parameterMap);

            responseObject.put("initialValues", initialValues);

            return Response.ok().entity(responseObject.toString()).build();
        }
        catch(Exception e)
        {
            String message = "An error occurred while loading the module with lsid: \"" + lsid + "\"";
            if(e.getMessage() != null)
            {
                message = e.getMessage();
            }
            log.error(message);

            if(message.contains("You do not have the required permissions"))
            {
                throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(message)
                    .build()
                );
            }
            else
            {
                throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .build()
                );
            }
        }
	}

    @POST
    @Path("/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
        @FormDataParam("ifile") InputStream uploadedInputStream,
        @FormDataParam("ifile") FormDataContentDisposition fileDetail,
        @FormDataParam("paramName") final String paramName,
        @FormDataParam("index") final int index,        
        @Context HttpServletRequest request)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");
            if (username == null)
            {         
                throw new Exception("User not logged in");
            }

            ServerConfiguration.Context jobContext=ServerConfiguration.Context.getContextForUser(username);

            JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
            GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, paramName, fileDetail.getFileName());

            // save it
            writeToFile(uploadedInputStream, gpFilePath.getServerFile().getCanonicalPath());
            fileUtil.updateUploadsDb(gpFilePath);

            String output = "File uploaded to : " + gpFilePath.getServerFile().getCanonicalPath();
            log.error(output);

            log.error(gpFilePath.getUrl().toExternalForm());
            ResponseJSON result = new ResponseJSON();
            result.addChild("location",  gpFilePath.getUrl().toExternalForm());
            return Response.ok().entity(result.toString()).build();
        }
        catch(Exception e)
        {
            String message = "An error occurred while uploading the file \"" + fileDetail.getFileName() + "\"";
            if(e.getMessage() != null)
            {
                message = message + ": " + e.getMessage();
            }
            log.error(message);

            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .build()
            );
        }
    }

    @POST
    @Path("/addJob")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addJob(
        JobSubmitInfo jobSubmitInfo,
        @Context HttpServletRequest request)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");
            if (username == null)
            {
                throw new Exception("User not logged in");
            }

            final ServerConfiguration.Context userContext=ServerConfiguration.Context.getContextForUser(username);
            final BatchInputHelper jobInputHelper=new BatchInputHelper(userContext, jobSubmitInfo.getLsid());

            JSONObject parameters = new JSONObject(jobSubmitInfo.getParameters());
            Iterator<String> paramNames = parameters.keys();
            while(paramNames.hasNext())
            {
                String parameterName = paramNames.next();
                boolean isBatch = isBatchParam(jobSubmitInfo, parameterName);
                //JSONArray valueList = new JSONArray((String)parameters.get(parameterName));
                JSONArray valueList;
                Object val=parameters.get(parameterName);
                if (val instanceof JSONArray) {
                    valueList=(JSONArray) val;
                }
                else {
                    valueList = new JSONArray((String)parameters.get(parameterName));
                }
                for(int v=0; v<valueList.length();v++)
                {
                    if (isBatch) {
                        jobInputHelper.addBatchDirectory(parameterName, valueList.getString(v));
                    }
                    else {
                        jobInputHelper.addValue(parameterName, valueList.getString(v));
                    }
                }
            }

            //
            // experimental, when inferBatch is true, it means ignore the 'Single' or 'Batch' selection from the end user
            //    instead infer batch inputs when the input value is a directory (instead of a file)
            //
            final List<JobInput> batchInputs;
            //final boolean inferBatch=false;
            //jobInputHelper.setInferBatchParams(inferBatch);
            //if (inferBatch) {
            //    batchInputs=jobInputHelper.inferBatch();
            //}
            //else {
                batchInputs=jobInputHelper.prepareBatch();
            //}
            final JobReceipt receipt=jobInputHelper.submitBatch(batchInputs);

            
            //TODO: if necessary, add batch details to the JSON representation
            String jobId="-1";
            if (receipt.getJobIds().size()>0) {
                jobId=receipt.getJobIds().get(0);
            }
            ResponseJSON result = new ResponseJSON();
            result.addChild("jobId", receipt.getJobIds().get(0));
            if (receipt.getBatchId() != null && receipt.getBatchId().length()>0) {
                result.addChild("batchId", receipt.getBatchId());
                request.getSession().setAttribute(JobBean.DISPLAY_BATCH, receipt.getBatchId());
            }
            return Response.ok(result.toString()).build();
        }
        catch(Exception e)
        {
            String message = "An error occurred while submitting the job";
            if(e.getMessage() != null)
            {
                message = message + ": " + e.getMessage();
            }
            log.error(message);

            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build()
            );
        }
    }
    
    /**
     * Given the submitted job info and a param name, determine if the provided parameter is a batch parameter
     * @param jobSubmitInfo
     * @param name
     * @return
     */
    private boolean isBatchParam(JobSubmitInfo jobSubmitInfo, String name) {
        List<String> batches = jobSubmitInfo.getBatchParams();
        return batches.contains(name);
    }

    /**
     * Get the GP client code for the given task, copied from JobBean#getTaskCode().
     * Requires a logged in user, and valid 'lsid' query parameter or a valid 'reloadJob' query parameter.
     * The lsid can be the full lsid or the name of a module.
     * 
     * To test from curl,
     * <pre>
       curl -u <username:password> <GenePatternURL>/rest/RunTask/viewCode?
           lsid=<lsid>,
           reloadJob=<reloadJobId>,
           language=[ 'Java' | 'R' | 'MATLAB' ], if not set, default to 'Java',
           <pname>=<pvalue>
     * </pre>
     * 
     * Example 1: get Java code for ComparativeMarkerSelection (v.9)
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&lsid=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9" 
     * </pre>
     * Example 2: by taskName
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&lsid=ComparativeMarkerSelection" 
     * </pre>
     * Example 3: initialize the input.filename
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&lsid=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9&input.filename=ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct" 
     * </pre>
     * Example 4: from a reloaded job
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&reloadJob=9948" 
     * </pre> 
     * 
     * Note: I had to wrap the uri in double-quotes to deal with the '&' character.
     * 
     * Note: If you prefer to use use cookie-based authentication. This command logs in and
     * saves the session cookie to the file 'cookies.txt'
     * <pre>
       curl -c cookies.txt "<GenePatternURL>/login?username=<username>&password=<password>"
       </pre>
     *
     * Use the '-b cookies.txt' on subsequent calls.       
     * 
     * @param lsid, the full lsid or taskName of the module or pipeline
     * @param language, the programming language client, e.g. 'Java', 'R', or 'MATLAB'
     * @return
     */
    @GET
    @Path("/viewCode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response viewCode(
            @QueryParam("language") String language,
            @QueryParam("lsid") String lsid,
            final @QueryParam("reloadJob") String reloadJob, 
            final @QueryParam("_file") String _fileParam,
            final @QueryParam("_format") String _formatParam,
            final @Context HttpServletRequest request
    ) {

        String userId = (String) request.getSession().getAttribute("userid");
        final ServerConfiguration.Context userContext=ServerConfiguration.Context.getContextForUser(userId);
        JobInput reloadJobInput=null;
        if (reloadJob != null && !reloadJob.equals("")) {
            //This is a reloaded job
            try {
                reloadJobInput=ParamListHelper.getInputValues(userContext, reloadJob);
            }
            catch (Exception e) {
                log.error("Error initializing from reloadJob="+reloadJob, e);
                return Response.serverError().entity(e.getLocalizedMessage()).build();
            }
        }
        if (lsid==null || lsid.length()==0) {
            if (reloadJobInput != null) {
                lsid=reloadJobInput.getLsid();
            }
        }
        if (lsid==null || lsid.length()==0) { 
            //400, Bad Request
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing required request parameter, 'lsid'").build();
        }
        if (language==null || language.length()==0) {
            log.debug("Missing request parameter, setting 'language=Java'");
            language="Java";
        }
        JSONObject content=new JSONObject();
        try {
            IAdminClient adminClient = new LocalAdminClient(userId);
            TaskInfo taskInfo = adminClient.getTask(lsid);
            if (taskInfo == null) { 
                return Response.status(Response.Status.NOT_FOUND).entity("Module not found, lsid="+lsid).build();
            }
            
            ParameterInfo[] parameters = taskInfo.getParameterInfoArray();
            
            
            ParameterInfo[] jobParameters=null;
            if (parameters != null) {
                JobInput initialValues=ParamListHelper.getInitialValues(
                        parameters, reloadJobInput, _fileParam, _formatParam, request.getParameterMap());
                
                jobParameters = new ParameterInfo[parameters.length];
                int i=0;
                for(ParameterInfo pinfo : parameters) {
                    final String id=pinfo.getName();
                    String value=null;
                    if (initialValues.hasValue(id)) {
                        Param p = initialValues.getParam(pinfo.getName());
                        int numValues=p.getNumValues();
                        if (numValues==0) {
                        }
                        else if (numValues==1) {
                            value=p.getValues().get(0).getValue();
                        }
                        else {
                            //TODO: can't initialize from a list of values
                            log.error("can't initialize from a list of values, lsid="+lsid+
                                    ", pname="+id+", numValues="+numValues);
                        }
                    }
                    jobParameters[i++] = new ParameterInfo(id, value, "");
                }
            }

            JobInfo jobInfo = new JobInfo(-1, -1, null, null, null, jobParameters, userContext.getUserId(), lsid, taskInfo.getName());
            boolean isVisualizer = TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes());
            AnalysisJob job = new AnalysisJob(UIBeanHelper.getServer(), jobInfo, isVisualizer);
            String code=CodeGeneratorUtil.getCode(language, job, taskInfo, adminClient);
            content.put("code", code);
            return Response.ok().entity(content.toString()).build();
        }
        catch (Throwable t) {
            //String errorMessage=;
            log.error("Error getting code.", t);
            try {
                content.put("error", "Error getting code: "+t.getLocalizedMessage());
                return Response.serverError().entity(content).build();
            }
            catch (JSONException e) {
                log.error(e);
            }
            return Response.serverError().build();
        }
    }

    // save uploaded file to new location
    private void writeToFile(InputStream uploadedInputStream,
        String uploadedFileLocation) {

        try {
            OutputStream out = new FileOutputStream(new File(
                    uploadedFileLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    private JSONArray getParameterList(ParameterInfo[] pArray)
    {
        JSONArray parametersObject = new JSONArray();

        for(int i =0;i < pArray.length;i++)
        {
            ParametersJSON parameter = new ParametersJSON(pArray[i]);
            parametersObject.put(parameter);
        }

        return parametersObject;
    }

    private ArrayList getModuleVersions(TaskInfo taskInfo) throws Exception
    {
        LSID taskLSID = new LSID(taskInfo.getLsid());
        String taskNoLSIDVersion = taskLSID.toStringNoVersion();

        ArrayList moduleVersions = new ArrayList();
        TaskInfo[] tasks = TaskInfoCache.instance().getAllTasks();
        for(int i=0;i<tasks.length;i++)
        {
            TaskInfoAttributes tia = tasks[i].giveTaskInfoAttributes();
            String lsidString = tia.get(GPConstants.LSID);
            LSID lsid = new LSID(lsidString);
            String lsidNoVersion = lsid.toStringNoVersion();
            if(taskNoLSIDVersion.equals(lsidNoVersion))
            {
                moduleVersions.add(lsidString);
            }
        }

        return moduleVersions;
    }

    private TaskInfo getTaskInfo(String taskLSID, String username) throws WebServiceException
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }
}
