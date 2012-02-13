<link href="../css/swfupload.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="../js/swfupload/swfupload.js"></script>
<script type="text/javascript" src="../js/swfupload/swfupload-handlers.js"></script>
<script type="text/javascript">
		var upload1, upload2;
	   	
	   var uploadCompleteHandler = 
	   	function (file, server_data) 
	   	{ 
	   		try {
					var progress = new FileProgress(file, this.customSettings.progressTarget);
					progress.SetComplete();
					progress.SetStatus("Complete.");
					progress.ToggleCancel(false,upload1);
					
					<c:if test="${singleUpload}">
						enableRemoveButton(true);
						enableCancelButton(false);
					</c:if>
			
				} catch (ex) { this.debug(ex); }
				
				try
				{
					if(localUploadCompleteHandler)
					{
						localUploadCompleteHandler(file);
					}
				}
				catch(e){}
	   	};
	   	
		function uploader() {
		
			upload1 = new SWFUpload({
				// Backend Settings
				upload_url: "../servlet/upload;jsessionid=<c:out value="${pageContext.session.id}"/><c:if test="${singleUpload}">?single=true</c:if>",	// Relative to the SWF file (or you can use absolute paths)

				// File Upload Settings
				file_types : "*.*",
				file_types_description : "All Files",
				file_upload_limit : "<c:if test="${singleUpload}">1</c:if><c:if test="${!singleUpload}">1000</c:if> ",
				file_queue_limit : "0",

				// Event Handler Settings (all my handlers are in the Handler.js file)
				file_dialog_start_handler : fileDialogStart,
				file_queued_handler : fileQueued,
				file_queue_error_handler : fileQueueError,
				file_dialog_complete_handler : fileDialogComplete,
				upload_start_handler : uploadStart,
				upload_progress_handler : uploadProgress,
				upload_error_handler : uploadError,
				upload_complete_handler : uploadCompleteHandler,
				file_complete_handler : fileComplete,

				// Flash Settings
				flash_url : "../flash/swfupload.swf",	// Relative to this file (or you can use absolute paths)

				// UI Settings
				ui_container_id : "flashUI1",
				degraded_container_id : "degradedUI1",

				// Debug Settings
				debug: false
			});
			upload1.customSettings.progressTarget = "fsUploadProgress1";	// Add an additional setting that will later be used by the handler.
			upload1.customSettings.cancelButtonId = "btnCancel1";			// Add an additional setting that will later be used by the handler.
	     }
	     
	function enableRemoveButton(enabled)
	{
		if(document.getElementById("btnDelete"))
		{
			document.getElementById("btnDelete").disabled=!enabled;
		}
	}
	
	function enableCancelButton(enabled)
	{
		if(document.getElementById("btnCancel1"))
		{
			document.getElementById("btnCancel1").disabled=!enabled;
		}
	}
</script>