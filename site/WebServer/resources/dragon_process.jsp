<%@ page import="com.equant.csi.interfaces.cis.DragonMigration"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.List"%>
<%@ page import="org.apache.commons.fileupload.FileItem"%>
<%@ page import="org.apache.commons.fileupload.DiskFileUpload"%>
<%@ page import="org.apache.commons.fileupload.FileUpload"%>
<%@ page import="org.apache.commons.fileupload.FileUploadException"%>
<%@ page import="java.io.File"%>
<%@ page import="java.security.MessageDigest"%>
<%@ page import="java.security.NoSuchAlgorithmException"%><%@ page import="org.apache.commons.lang.StringUtils"%>
<%--
  Created by vadim.gulyakin, Jun 30, 2005
--%>
<%!
   public String getMD5Hash(String aString) {
    MessageDigest md5 = null;
    try {
        md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
        return null;
    }
    byte[] digestBytes = md5.digest(aString.getBytes());

   StringBuffer buf = new StringBuffer(digestBytes.length * 2);
    for (int i = 0; i < digestBytes.length; i++) {
        if (((int) digestBytes[i] & 0xff) < 0x10)
            buf.append("0");
        buf.append(Long.toString((int) digestBytes[i] & 0xff, 16));
    }
    return buf.toString();
  }
%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html;CHARSET=iso-8859-1">
<title>Dragon migration data loader</title>
<link href="style.css" rel="stylesheet" type="text/css">
</head>
<body>
<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> <!-- page title -->
    <td height="70px" class="titlebar"> <span class="title">CSI</span> <br><span class="subtitle">administrative
      interface</span> </td>
  </tr>
  <tr> <!-- location bar-->
    <td height="15px" class="nav"><a href="index.html">Administrative interface</a> / <a href="dragon_submit.html">Dragon migration</a> / Migration result</td>
  </tr>
  <tr>
    <td height="*" align="center">&nbsp;
<%
    String loginId = "dragon";
    String hashPass = "46f94c8de14fb36680850768ff1b7f2a";//123qwe
    //TODO:Implement better way
    String login = null;
    String password = null;
    String file1 = null;
    String file2 = null;
    try {
        if (!FileUpload.isMultipartContent(request))
            throw new Exception("Invalid request");

        DiskFileUpload fu = new DiskFileUpload();
        // maximum size before a FileUploadException will be thrown
        fu.setSizeMax(1000000);
        // maximum size that will be stored in memory
        fu.setSizeThreshold(4096);
        List fileItems = null;
        DragonMigration dragonMigration = new DragonMigration();
        String importPath = dragonMigration.getImportPath();
        fileItems = fu.parseRequest(request);
        Iterator i = fileItems.iterator();
        FileItem fi = null;
        fi = (FileItem) i.next(); //login name
        login = fi.getString();
        fi = (FileItem) i.next();  //password
        password = fi.getString();

        String passMD5  =  getMD5Hash(password);
        if (StringUtils.isEmpty(passMD5)) {
            out.println("An error occured while calculating password MD5 hash.");
            out.println("<BR>Please contact to administrator.");
        } else if (loginId.equals(login) && hashPass.equals(passMD5)) {
            fi = (FileItem) i.next();
            if (fi.getSize() == 0)
                throw new FileUploadException("Source file not found");
            file1 = "sc.csv_" + dragonMigration.getCurrentTimeString();
            fi.write(new File(importPath + file1));

            fi = (FileItem) i.next();
            if (fi.getSize() != 0) {
                file2 = "pvc.csv_" + dragonMigration.getCurrentTimeString();
                fi.write(new File(importPath + file2));
            }

            dragonMigration.setFileNames(file1, file2);
            if (dragonMigration.run() == true)
                out.println("...Dragon migration completed. ");
            else
                out.println("...Dragon migration failed. ");
            session.setAttribute("latestDragonLogFileName", dragonMigration.getFileAppenderName());
            out.println("See <a href='viewlog.jsp' target='_blank'>latest log</a> for details.");
        } else { %>
            Incorrect user name or password. Please try again.
            <br><a href='javascript:history.back(1)'>Go back</a>
            <%
        }
    } catch (Exception ex) {
        out.println("<br>File upload failed. Reason: " + ex.getMessage());
        out.println("<br>Please try again. <a href='javascript:history.back(1)'>Go back</a>");
    }
    %>
	</td>
  </tr>
  <tr class="footer">
    <td height="15px"></td>
  </tr>
</table>
</body>
</html>
