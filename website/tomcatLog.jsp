<%@ page import="java.io.*,
		 java.text.SimpleDateFormat,
		 java.util.Calendar,
		 java.util.Properties"
	session="false" contentType="text/html" language="Java" %><%

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	Calendar cal = Calendar.getInstance();
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	boolean doGP = request.getParameter("tomcat") == null;
	String what = doGP ? "GenePattern" : "web server";
	File f = null;

	// if the date has rolled over but there is not yet an entry in today's log, look backward in time
	if (doGP) {
		Properties props = new Properties();
		props.load(new FileInputStream(System.getProperty("log4j.configuration")));
		f = new File(props.getProperty("log4j.appender.R.File"));
	} else {
		if (System.getProperty("serverInfo").indexOf("Apache Tomcat") != -1) {
			for (int i = 0; i < 10; i++) {
				String filename = "localhost_log." + df.format(cal.getTime()) + ".txt";
				f = new File("logs", filename);
				if (f.exists()) break;
				f = null;
				cal.add(Calendar.DATE, -1); // backup up one day
			}
		}
	}
	cal = Calendar.getInstance();
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title><%= what %> log file from <%= request.getServerName() %> on <%= cal.getTime() %></title>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<pre>
<% if (f == null || !f.exists()) { %>
	No logs exist.
<% } else { %>
<%= what %> log file from <%= request.getServerName() %> on <%= cal.getTime() %>
<br><hr><br>
<%

	Reader logFile = new BufferedReader(new FileReader(f));
	char[] buf = new char[100000];
	int i;
	while ((i = logFile.read(buf, 0, buf.length)) > 0) {
		out.print(new String(buf, 0, i));
	}
	logFile.close();
%>
</pre>
<hr>
<br>
<form>
	<center>
	<input type="button" value="refresh" onclick="window.location.reload()">
	</center>
</form>
<% } %>
<br>
<a href="tomcatLog.jsp<%= doGP ? "?tomcat=1" : "" %>">view <%= doGP ? "web server" : "GenePattern" %> log</a><br>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
