<%@ taglib uri="/WEB-INF/c.tld" prefix="c" %>
<%-- Referring to OriginalFileLocation to get the path to source FLV --%>

<c:set scope="request" var="fileUrl2" value="../../servlet/display/${assetForm.encryptedOriginalFilePath}"/>
<script src="../flash/custom/AC_OETags.js" language="javascript"></script>
<script language="JavaScript" type="text/javascript">
<!--
// -----------------------------------------------------------------------------
// Globals
// Major version of Flash required
var requiredMajorVersion = 9;
// Minor version of Flash required
var requiredMinorVersion = 0;
// Minor version of Flash required
var requiredRevision = 28;
// -----------------------------------------------------------------------------
// -->
<!--
// Version check for the Flash Player that has the ability to start Player Product Install (6.0r65)
var hasProductInstall = DetectFlashVer(6, 0, 65);

// Version check based upon the values defined in globals
var hasRequestedVersion = DetectFlashVer(requiredMajorVersion, requiredMinorVersion, requiredRevision);

if ( hasProductInstall && !hasRequestedVersion ) {
	// DO NOT MODIFY THE FOLLOWING FOUR LINES
	// Location visited after installation is complete if installation is required
	var MMPlayerType = (isIE == true) ? "ActiveX" : "PlugIn";
	var MMredirectURL = window.location;
    document.title = document.title.slice(0, 47) + " - Flash Player Installation";
    var MMdoctitle = document.title;
	AC_FL_RunContent(
		"src", "playerProductInstall","FlashVars","MMredirectURL="+MMredirectURL+'&MMplayerType='+MMPlayerType+'&MMdoctitle='+MMdoctitle+'&myVideo=<c:out value="${fileUrl2}"/>',
		"width", "500",
		"height", "400",
		"align", "middle",
		"id", "Video3D",
		"quality", "high",
		"bgcolor", "#869ca7",
		"name", "Video3D",
		"allowScriptAccess","sameDomain",
		"type", "application/x-shockwave-flash",
		"pluginspage", "http://www.adobe.com/go/getflashplayer"
	);
} else if (hasRequestedVersion) {
	// if we've detected an acceptable version
	// embed the Flash Content SWF when all Video3Ds are passed
       
	AC_FL_RunContent(
			"src", "../flash/custom/Video3D",
			"width", "500",
			"height", "400",
			"flashVars", "myVideo=<c:out value='${fileUrl2}'/>",
			"align", "middle",
			"id", "Video3D",
			"quality", "high",
			"bgcolor", "#869ca7",
			"name", "Video3D",
			"allowScriptAccess","sameDomain",
			"type", "application/x-shockwave-flash",
			"pluginspage", "http://www.adobe.com/go/getflashplayer"
	);
  } else {  // flash is too old or we can't detect the plugin
    var alternateContent = 'Alternate HTML content should be placed here. '
  	+ 'This content requires the Adobe Flash Player. '
   	+ '<a href=http://www.adobe.com/go/getflash/>Get Flash</a>';
    document.write(alternateContent);  // insert non-flash content
  }
// -->
</script>
<noscript>
  	<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"
			id="Video3D" width="100%" height="100%"
			codebase="http://fpdownload.macromedia.com/get/flashplayer/current/swflash.cab">
			<param name="movie" value="../flash/custom/Video3D.swf" />
			<param name="quality" value="high" />
			<param name='flashVars' value='myVideo=<c:out value="${fileUrl2}"/>'/>
			<param name="bgcolor" value="#869ca7" />
			<param name="allowScriptAccess" value="sameDomain" />
			<embed src="../flash/custom/Video3D.swf" quality="high" bgcolor="#869ca7"
				width="500" height="400" name="Video3D" align="middle"
				play="true"
				loop="false"
				quality="high"
				allowScriptAccess="sameDomain"
				type="application/x-shockwave-flash"
				pluginspage="http://www.adobe.com/go/getflashplayer">
			</embed>
	</object>
</noscript>