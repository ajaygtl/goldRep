<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="java.io.*"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
  Created by vadim.gulyakin, Jul 5, 2005
--%>
<%!
public String getContents(File aFile) {

    if (aFile==null) return "ERROR: Invalid file name";
    StringBuffer contents = new StringBuffer();

    BufferedReader input = null;
    try {
      input = new BufferedReader( new FileReader(aFile) );
      String line = null;
      while (( line = input.readLine()) != null){
        contents.append(line);
        contents.append("<br>");
      }
    }
    catch (FileNotFoundException ex) {
      return "ERROR: An error occured during reading log file. File not found.";
    }
    catch (IOException ex){
      return "ERROR: An error occured during reading log file";
    }
    finally {
      try {
        if (input!= null) input.close();
      }
      catch (IOException ex) {ex.getMessage(); }
    }
    return contents.toString();
  }
%>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html;CHARSET=iso-8859-1">
    <title>
        <%
            String logFileName = (String) session.getAttribute("latestDragonLogFileName");
            File logFile = null;
            if (StringUtils.isNotEmpty(logFileName)) {
               logFile = new File(logFileName);
               out.println(logFile.getName());
            }
        %>
    </title>
    <link href="style.css" rel="stylesheet" type="text/css">
  </head>
  <body>
  <font color="#000000">
    <%
        if (logFile == null)
            out.println("File not found.");
        else if (logFile.canRead())
            out.println(getContents(logFile));
        else
            out.println("Can`t read log file.");
    %>
  </font>
  </body>
</html>