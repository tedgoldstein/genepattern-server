<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		 java.io.File,
		 java.net.MalformedURLException,
		 java.text.DateFormat,
		 java.text.NumberFormat,
		 java.text.ParseException,
		 java.util.Arrays,
		 java.util.Comparator,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.Properties,
		 java.util.Map,
		 java.util.StringTokenizer,
		 java.util.TreeSet,
		 java.util.List,
		 java.util.ArrayList,
		 java.util.Vector"
   session="false" language="Java" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
	LocalAdminClient adminClient = new LocalAdminClient("GenePattern");


%>
	<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
<title>Installable Suites</title>
<style>
td { font-size: 8pt }
</style>

<script language="Javascript">
var ie4 = (document.all) ? true : false;
var ns4 = (document.layers) ? true : false;
var ns6 = (document.getElementById && !document.all) ? true : false;

function writeToLayer(lay,txt) {
	if (ns6) {
		over = document.getElementById([lay]);
		range = document.createRange();
		range.setStartBefore(over);
		domfrag = range.createContextualFragment(txt);
		while (over.hasChildNodes()) {
			over.removeChild(over.lastChild);
		}
		over.appendChild(domfrag);
	} else if (ns4) {
		var l = document['id'+lay];
		l.document.write(txt);
		l.document.close();
	} else if (ie4) {
		document.all(lay).innerHTML = txt;
	}

}

function checkAll(frm, bChecked) {
	frm = document.forms['install'];
	for (i = 0; i < frm.elements.length; i++) {
		if (frm.elements[i].type != "checkbox") continue;
		frm.elements[i].checked = bChecked;
	}
}


</script>
	</head>
	<body>
	<jsp:include page="navbar.jsp"></jsp:include>
	<span id="fetching">
		Fetching suite catalog from <a href="<%= System.getProperty("SuiteRepositoryURL") %>" target="_new"><%= System.getProperty("SuiteRepositoryURL") %></a>...
	</span>
<%
	out.flush();
	
SuiteRepository sr = null;
HashMap suites = new HashMap();
	try {
		sr = new SuiteRepository();
		suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));
	} catch (Exception e) {
%>
		Sorry, the <%=messages.get("ApplicationName")%> <a href="<%= System.getProperty("SuiteRepositoryURL") %>" target="_new">suite repository</a> is not currently available.<br>
		<p>Reason: <code><%= e.getMessage() %></code><br>
		<p>
		<b>Try to correct this problem</b> by changing <a href="adminServer.jsp">web proxy settings</a> or <a href="adminServer.jsp">Suite Repository URL.</a>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
		e.printStackTrace();
		return;
	} finally {
		// erase the Fetching... message
%>
		<script language="Javascript">
			document.getElementById("fetching").innerHTML = "";
		</script>
<%
		out.flush();
	}
	String motd = sr.getMOTD_message();
	if (motd.length() > 0) {
%>
		<%= motd %><br>
		<font size="1">updated <%= DateFormat.getDateInstance().format(sr.getMOTD_timestamp()) %>.  
		<a href="<%= sr.getMOTD_url() %>" target="_blank">More information</a>.</font><br>
		<%= newerServerAvailable(sr.getMOTD_latestServerVersion(), System.getProperty("GenePatternVersion")) ? 
			("<a href=\"http://www.broad.mit.edu/cancer/software/genepattern/download\">Download updated "+messages.get("ApplicationName")+" version " + 
				sr.getMOTD_latestServerVersion() + "</a> (currently running version " + 
				System.getProperty("GenePatternVersion") + ")<br>") :
			 "" %>
		<br>
		<hr>
<%
		for (int ii = 0; ii < 8*1024; ii++) out.print(" ");
		out.println();
		out.flush();
	}
	
%>

	
Select from the following suites from the <%=messages.get("ApplicationName")%> public access website to download and install:<br><br>
 
<br>
<table width=100% cellspacing=0>
<tr>
<td colspan="5" width="80%">
<font size="+1"><b><%= "YYY"  %> of <%=suites.size()%> suites are new or updated</b></font>
</td>
<td>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
</td>
<tr><td>&nbsp;</td></tr>
</tr>
<%  
	for (Iterator iter = suites.keySet().iterator(); iter.hasNext(); ){
		HashMap suite = (HashMap)suites.get(iter.next());		
%>
<tr class='paleBackground'>

<td><font size=+1><b><%=suite.get("name")%></b></font></td>
<td>Author: <%=suite.get("author")%><br>Owner: <%=suite.get("owner")%></td>
</tr>
<tr class='paleBackground'>
<td colspan=2><%=suite.get("description")%></td>

</tr>
<tr class='paleBackground'>
<td colspan=2>Suite Documentation: <%  ArrayList docs = (ArrayList)suite.get("docFiles");
	for (Iterator iter2 = docs .iterator(); iter2.hasNext(); ){
		String doc = (String)iter2.next();
		int idx = doc.lastIndexOf("/");

%>
		<a href='<%=doc%>'><%= doc.substring(idx+1)%></a>

<% } %>
</td>
</tr>
<tr>
<td colspan=2>
<table width=100%>
<% 
ArrayList modules = (ArrayList)suite.get("modules");
int count = 0;
for (Iterator iter2 = modules.iterator(); iter2.hasNext(); ){
	HashMap mod = (HashMap)iter2.next();
	LSID modLsid = new LSID((String)mod.get("lsid"));
	String docName = (String)(mod.get("docFile"));
	int idx = docName.lastIndexOf("/");

	TaskInfo ti = adminClient.getTask(modLsid.toString());
	boolean installed = ti != null;
	
	if ( (count%2) == 0) out.println("<tr>");

	if (installed){
%>
		<td><input type="checkbox" checked="true" disabled="true"/></td>
<td width=50%><%=mod.get("name")%> (<%=modLsid.getVersion()%>) <a href='<%= docName %>'><img src="skin/pdf.jpg" border="0" alt="doc" align="texttop"></a> <a href="addTask.jsp?view=1&name=<%=modLsid.toString()%>"><img src="skin/view.jpg" border="0" alt="view" align="texttop"></a> </td>

<% 	} else { %>
		<td><input type="checkbox"/></td>
<td width=50%><%=mod.get("name")%> (<%=modLsid.getVersion()%>) <a href='<%= docName %>'><img src="skin/pdf.jpg" border="0" alt="doc" align="texttop"></a> </td>

<% 	
	} 

	if ( (count%2) == 1) out.println("</tr>");

	count++;
} 
%>

</table>
</td>
</tr>
<%
	}
%>
</table>


<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
<%! public boolean newerServerAvailable(String versionAtBroad, String localVersion) {
	boolean ret = false;
	StringTokenizer stBroadVersion = new StringTokenizer(versionAtBroad, LSID.VERSION_DELIMITER);
	StringTokenizer stLocalVersion = new StringTokenizer(localVersion, LSID.VERSION_DELIMITER);
	String broadVersionMinor;
	String localVersionMinor;
	int broadMinor;
	int localMinor;
	NumberFormat df = NumberFormat.getIntegerInstance();
	while (stBroadVersion.hasMoreTokens()) {
		broadVersionMinor = stBroadVersion.nextToken();
		if (!stLocalVersion.hasMoreTokens()) {
			// Broad version has more parts than local, but was equal until now
			// That means that it has an extra minor level and is therefore later
			ret = true;
			break;
		}
		localVersionMinor = stLocalVersion.nextToken();
		try {
			broadMinor = df.parse(broadVersionMinor).intValue();
		} catch (ParseException nfe) {
			// what to do?
			continue;
		}
		try {
			localMinor = df.parse(localVersionMinor).intValue();
		} catch (ParseException pe) {
			// what to do?
			continue;
		}
		if (broadMinor > localMinor) {
			ret = true;
			break;
		} else if (broadMinor < localMinor) {
			//System.out.println("You're running a greater version than downloadable");
			break;
		}
	}
	// don't worry about the case where the local version has more levels than the Broad version
	return ret;
    }
%>