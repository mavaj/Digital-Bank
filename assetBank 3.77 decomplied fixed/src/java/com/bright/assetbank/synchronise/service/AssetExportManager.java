/*     */ package com.bright.assetbank.synchronise.service;
/*     */ 
/*     */ import com.bn2web.common.exception.Bn2Exception;
/*     */ import com.bright.assetbank.application.bean.Asset;
/*     */ import com.bright.assetbank.application.bean.ImageConversionInfo;
/*     */ import com.bright.assetbank.application.bean.LightweightAsset;
/*     */ import com.bright.assetbank.application.constant.AssetBankSettings;
/*     */ import com.bright.assetbank.application.service.AssetManager;
/*     */ import com.bright.assetbank.application.util.AssetUtil;
/*     */ import com.bright.assetbank.attribute.service.AttributeManager;
/*     */ import com.bright.assetbank.attribute.service.AttributeValueManager;
/*     */ import com.bright.assetbank.batch.service.BatchQueueManager;
/*     */ import com.bright.assetbank.synchronise.bean.AssetBeanWriter;
/*     */ import com.bright.assetbank.synchronise.bean.DeleteResult;
/*     */ import com.bright.assetbank.synchronise.bean.ExportInfo;
/*     */ import com.bright.assetbank.synchronise.bean.ExportResult;
/*     */ import com.bright.assetbank.synchronise.bean.ExternalAssetImpl;
/*     */ import com.bright.assetbank.synchronise.bean.ExternalExportAssetImpl;
/*     */ import com.bright.assetbank.synchronise.bean.ExternalSynchAssetImpl;
/*     */ import com.bright.assetbank.synchronise.bean.SynchAssetBeanWriter;
/*     */ import com.bright.assetbank.synchronise.constant.ExportConstants;
/*     */ import com.bright.assetbank.synchronise.util.SynchUtil;
/*     */ import com.bright.framework.common.bean.FileBean;
/*     */ import com.bright.framework.constant.FrameworkConstants;
/*     */ import com.bright.framework.database.bean.DBTransaction;
/*     */ import com.bright.framework.database.service.DBTransactionManager;
/*     */ import com.bright.framework.file.BeanWrapper;
/*     */ import com.bright.framework.file.DefaultBeanReader;
/*     */ import com.bright.framework.file.DefaultBeanReader.BeanPopulationException;
/*     */ import com.bright.framework.file.DefaultBeanReader.TooManyColumnsException;
/*     */ import com.bright.framework.file.ExcelFormat;
/*     */ import com.bright.framework.file.FileFormat;
/*     */ import com.bright.framework.language.constant.LanguageConstants;
/*     */ import com.bright.framework.queue.bean.QueuedItem;
/*     */ import com.bright.framework.service.FileStoreManager;
/*     */ import com.bright.framework.storage.bean.StorageDevice;
/*     */ import com.bright.framework.storage.constant.StorageDeviceType;
/*     */ import com.bright.framework.storage.constant.StoredFileType;
/*     */ import com.bright.framework.storage.service.StorageDeviceManager;
/*     */ import com.bright.framework.user.bean.User;
/*     */ import com.bright.framework.util.FileUtil;
/*     */ import com.bright.framework.util.StringUtil;
/*     */ import java.io.BufferedReader;
/*     */ import java.io.BufferedWriter;
/*     */ import java.io.File;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.FileOutputStream;
/*     */ import java.io.FileReader;
/*     */ import java.io.IOException;
/*     */ import java.io.OutputStreamWriter;
/*     */ import java.sql.SQLException;
/*     */ import java.util.Collection;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Vector;
/*     */ import org.apache.commons.io.FileUtils;
/*     */ import org.apache.commons.io.FilenameUtils;
/*     */ import org.apache.commons.lang.StringUtils;
/*     */ import org.apache.commons.logging.Log;
/*     */ 
/*     */ public class AssetExportManager extends BatchQueueManager
/*     */   implements FrameworkConstants, ExportConstants
/*     */ {
/*  97 */   private static final String k_sExportFilenameFieldDelimiter = AssetBankSettings.getExportedAssetFilenameFieldDelimiter();
/*  98 */   private static final String k_sExportFilenameDelimiterReplacement = AssetBankSettings.getExportedAssetFilenameDelimiterReplacement();
/*     */   private static final int k_iNumAssetsPerFileRead = 100;
/*     */   private static final int k_iMaxAssetsInCache = 1000;
/*     */   public static final String k_sFileIdentifier = "Generated by AssetExportManager";
/*     */   public static final String k_sFilenameFormatPrefix = "filenameFormat:";
/* 106 */   private DBTransactionManager m_transactionManager = null;
/* 107 */   private AssetManager m_assetManager = null;
/* 108 */   private AttributeManager m_attributeManager = null;
/* 109 */   private AttributeValueManager m_attributeValueManager = null;
/* 110 */   private FileStoreManager m_fileStoreManager = null;
/* 111 */   private StorageDeviceManager m_storageDeviceManager = null;
/*     */ 
/* 113 */   private HashMap m_hmExportResults = new HashMap();
/* 114 */   private HashMap m_hmDeleteResults = new HashMap();
/*     */ 
/*     */   public void startup()
/*     */     throws Bn2Exception
/*     */   {
/* 124 */     super.startup();
/*     */ 
/* 126 */     if (k_sExportFilenameFieldDelimiter.equals(k_sExportFilenameDelimiterReplacement))
/*     */     {
/* 128 */       String sMessage = "Illegal application setting: AssetBankSettings.getExportedAssetFilenameFieldDelimiter() must not equal AssetBankSettings.getExportedAssetFilenameDelimiterReplacement()";
/*     */ 
/* 130 */       this.m_logger.error(sMessage);
/* 131 */       throw new RuntimeException(sMessage);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void processQueueItem(QueuedItem a_queuedItem)
/*     */     throws Bn2Exception
/*     */   {
/* 152 */     ExportInfo exportInfo = null;
/*     */     try
/*     */     {
/* 158 */       exportInfo = (ExportInfo)a_queuedItem;
/* 159 */       addMessage(exportInfo.getUser().getId(), "Starting export...");
/*     */ 
/* 161 */       ExportResult result = exportAssets(exportInfo.getAssetIds(), exportInfo.getName(), exportInfo.getDescription(), exportInfo.getExportFiles(), exportInfo.getDataOnly(), exportInfo.getExportFilesInZips(), exportInfo.isAllLanguages(), exportInfo.getUser().getId(), false);
/*     */ 
/* 171 */       this.m_hmExportResults.put(new Long(exportInfo.getUser().getId()), result);
/* 172 */       addMessage(exportInfo.getUser().getId(), "In total, " + result.getExportCount() + " items were successfully exported.");
/*     */     }
/*     */     catch (Bn2Exception bn2e)
/*     */     {
/* 176 */       this.m_logger.error("AssetExportManager.processQueueItem - exception:", bn2e);
/* 177 */       throw bn2e;
/*     */     }
/*     */     finally
/*     */     {
/* 181 */       if (exportInfo != null)
/*     */       {
/* 183 */         endBatchProcess(exportInfo.getUser().getId());
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   public int queueImport(ExportInfo a_exportInfo)
/*     */   {
/* 203 */     startBatchProcess(a_exportInfo.getUser().getId());
/*     */ 
/* 206 */     return queueItem(a_exportInfo);
/*     */   }
/*     */ 
/*     */   public ExportResult getExportResult(long a_lUserId)
/*     */   {
/* 218 */     return (ExportResult)this.m_hmExportResults.get(new Long(a_lUserId));
/*     */   }
/*     */ 
/*     */   public ExportResult exportAssets(Vector vAssetIds, String a_sName, String a_sDescription, boolean[] a_abExportFiles, boolean a_bDataOnly, boolean a_bFilesInZips, boolean a_bAllLanguages, long a_lUserId, boolean a_bSynch)
/*     */     throws Bn2Exception
/*     */   {
/* 246 */     ExportResult result = null;
/* 247 */     DBTransaction dbTransaction = null;
/* 248 */     int iExportCount = 0;
/* 249 */     BufferedWriter writer = null;
/*     */     try
/*     */     {
/* 252 */       String sFilename = a_sName;
/*     */ 
/* 254 */       if (StringUtils.isEmpty(sFilename))
/*     */       {
/* 256 */         sFilename = "export.xls";
/*     */       }
/*     */ 
/* 259 */       FileFormat format = new ExcelFormat();
/* 260 */       String extension = format.getFilenameExtension();
/*     */ 
/* 262 */       if (StringUtils.isNotEmpty(AssetBankSettings.getExportFileExtension()))
/*     */       {
/* 264 */         extension = AssetBankSettings.getExportFileExtension().trim().replace(".", "");
/*     */       }
/*     */ 
/* 267 */       String sDataFilepath = this.m_fileStoreManager.getUniqueFilepath(sFilename + "." + extension, StoredFileType.EXPORT);
/*     */ 
/* 269 */       File exportFile = new File(this.m_fileStoreManager.getAbsolutePath(sDataFilepath));
/* 270 */       writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportFile), "UTF-8"));
/*     */ 
/* 273 */       dbTransaction = this.m_transactionManager.getNewTransaction();
/* 274 */       Vector vAttributeTypes = this.m_attributeManager.getFlexibleAttributeList(dbTransaction);
/* 275 */       dbTransaction.commit();
/*     */ 
/* 277 */       Vector vSourceFilenames = new Vector(vAssetIds.size());
/*     */ 
/* 280 */       Map hmDestFilenames = new LinkedHashMap((int)(vAssetIds.size() * 1.5D));
/*     */ 
/* 282 */       AssetBeanWriter fileWriter = null;
/* 283 */       BeanWrapper wrapper = null;
/*     */ 
/* 285 */       if (a_bSynch)
/*     */       {
/* 287 */         fileWriter = new SynchAssetBeanWriter(writer, format, vAttributeTypes);
/* 288 */         wrapper = new ExternalSynchAssetImpl();
/*     */       }
/*     */       else
/*     */       {
/* 292 */         fileWriter = new AssetBeanWriter(writer, format, vAttributeTypes);
/* 293 */         wrapper = new ExternalExportAssetImpl();
/*     */       }
/*     */ 
/* 297 */       fileWriter.setWriteAllLanguages(a_bAllLanguages);
/*     */ 
/* 299 */       String sImportIdFormat = getIdFormat(a_bDataOnly, a_bSynch);
/*     */ 
/* 301 */       fileWriter.writeHeader(writer, "Generated by AssetExportManager", a_sName, a_sDescription, "filenameFormat:" + sImportIdFormat, format);
/*     */ 
/* 308 */       if (a_lUserId > 0L)
/*     */       {
/* 310 */         addMessage(a_lUserId, "Wrote export file header");
/*     */       }
/*     */ 
/* 322 */       int iNumDocumentsAtATime = 1000;
/* 323 */       long iStartId = 1L;
/* 324 */       boolean bIsFirst = true;
/* 325 */       int iSize = 0;
/* 326 */       String sSubstitutePrefix = AssetBankSettings.getSubstituteFilePrefix();
/* 327 */       int iNumFiles = 0;
/*     */ 
/* 330 */       Map mAssetCache = new LinkedHashMap(1500, 0.75F, true)
/*     */       {
/*     */         protected boolean removeEldestEntry(Map.Entry eldest) {
/* 333 */           return size() > 1000;
/*     */         }
/*     */       };
/*     */       do
/*     */       {
/* 339 */         boolean bGetRelated = (AssetBankSettings.getExportRelationshipData()) || (AssetBankSettings.getIncludeParentMetadataForExport());
/*     */ 
/* 342 */         dbTransaction = this.m_transactionManager.getNewTransaction();
/* 343 */         Vector vAssets = this.m_assetManager.getAssetsByIdAndBatchSize(dbTransaction, vAssetIds, null, iStartId, iNumDocumentsAtATime, bGetRelated, false, true);
/*     */ 
/* 345 */         iSize = 0;
/* 346 */         if (vAssets != null)
/*     */         {
/* 348 */           iSize = vAssets.size();
/*     */ 
/* 350 */           if (iSize > 0)
/*     */           {
/* 353 */             long iMaxId = ((Asset)vAssets.get(iSize - 1)).getId();
/* 354 */             iStartId = iMaxId + 1L;
/*     */ 
/* 358 */             if (a_bSynch)
/*     */             {
/* 360 */               Collections.reverse(vAssets);
/* 361 */               boolean[] a_abReversedExportFiles = new boolean[a_abExportFiles.length];
/* 362 */               for (int i = 0; i < a_abReversedExportFiles.length; i++)
/*     */               {
/* 364 */                 a_abReversedExportFiles[i] = a_abExportFiles[(a_abExportFiles.length - i - 1)];
/*     */               }
/* 366 */               a_abExportFiles = a_abReversedExportFiles;
/*     */             }
/*     */ 
/* 370 */             if (AssetBankSettings.getIncludeParentMetadataForExport())
/*     */             {
/* 372 */               for (int i = 0; i < vAssets.size(); i++)
/*     */               {
/* 374 */                 AssetUtil.mergeParentAssetData(dbTransaction, (Asset)vAssets.get(i), this.m_attributeValueManager, AssetBankSettings.getExportDataMargedFieldDelimiter(), true, true, false, LanguageConstants.k_defaultLanguage);
/*     */               }
/*     */             }
/*     */ 
/* 378 */             dbTransaction.commit();
/*     */ 
/* 380 */             String sSourceFilename = null;
/*     */ 
/* 382 */             for (int i = 0; i < vAssets.size(); i++)
/*     */             {
/* 384 */               Asset asset = (Asset)vAssets.get(i);
/*     */ 
/* 386 */               if ((!a_bDataOnly) && (a_abExportFiles[i] != false)) //a_abExportFiles[i]!=0
/*     */               {
/* 388 */                 boolean bFoundFile = false;
/*     */ 
/* 390 */                 if (StringUtil.stringIsPopulated(asset.getOriginalFileLocation()))
/*     */                 {
/* 392 */                   sSourceFilename = asset.getOriginalFileLocation();
/* 393 */                   bFoundFile = true;
/*     */                 }
/* 396 */                 else if (StringUtil.stringIsPopulated(asset.getFileLocation()))
/*     */                 {
/* 398 */                   sSourceFilename = asset.getFileLocation();
/* 399 */                   bFoundFile = true;
/*     */                 }
/*     */ 
/* 403 */                 if (bFoundFile)
/*     */                 {
/* 407 */                   String sFormat = AssetBankSettings.getPublishOrignalFileDestinationFormat();
/* 408 */                   if ((a_bSynch) && (asset.getIsImage()) && (StringUtil.stringIsPopulated(sFormat)))
/*     */                   {
/* 411 */                     int iMaxHeight = AssetBankSettings.getPublishOrignalFileDestinationMaxHeight();
/* 412 */                     int iMaxWidth = AssetBankSettings.getPublishOrignalFileDestinationMaxWidth();
/*     */ 
/* 414 */                     ImageConversionInfo conversionInfo = new ImageConversionInfo();
/* 415 */                     conversionInfo.setMaxHeight(iMaxHeight);
/* 416 */                     conversionInfo.setMaxWidth(iMaxWidth);
/* 417 */                     conversionInfo.setMaintainAspectRatio(true);
/*     */                     try
/*     */                     {
/* 422 */                       sSourceFilename = this.m_assetManager.getDownloadableAssetPath(asset, sFormat, conversionInfo);
/*     */                     }
/*     */                     catch (Bn2Exception e)
/*     */                     {
/* 427 */                       this.m_logger.error("AssetExportManager.exportAssets: Unable to use converted version of asset " + asset.getId() + " as master for synch: " + e.getMessage());
/*     */                     }
/*     */                   }
/*     */ 
/* 431 */                   vSourceFilenames.add(sSourceFilename);
/*     */ 
/* 433 */                   String sDestFilename = getExportFilename(asset, sImportIdFormat, a_bSynch, true, AssetBankSettings.getPublishOrignalFileDestinationFormat());
/*     */ 
/* 435 */                   iNumFiles++;
/*     */ 
/* 439 */                   if (sImportIdFormat.indexOf('i') < 0)
/*     */                   {
/* 441 */                     if (hmDestFilenames.containsKey(sDestFilename))
/*     */                     {
/* 443 */                       sDestFilename = getUniqueFilename(sDestFilename, hmDestFilenames);
/* 444 */                       asset.setOriginalFilename(sDestFilename);
/*     */                     }
/*     */                   }
/* 447 */                   hmDestFilenames.put(sDestFilename, sDestFilename);
/*     */ 
/* 450 */                   if ((!a_bSynch) && (AssetBankSettings.getAllowImageSubstitutes()) && (StringUtil.stringIsPopulated(asset.getOriginalFileLocation())) && (StringUtil.stringIsPopulated(asset.getFileLocation())))
/*     */                   {
/* 455 */                     vSourceFilenames.add(asset.getFileLocation());
/*     */ 
/* 457 */                     String sDestSubstitute = sSubstitutePrefix + sDestFilename;
/* 458 */                     hmDestFilenames.put(sDestSubstitute, sDestSubstitute);
/*     */ 
/* 460 */                     iNumFiles++;
/*     */                   }
/*     */                 }
/*     */ 
/*     */               }
/*     */ 
/* 466 */               if ((a_bDataOnly) || (!AssetBankSettings.getExportRelationshipData()))
/*     */                 continue;
/* 468 */               String sIds = null;
/*     */ 
/* 470 */               sIds = getExportedAssetIds(null, asset.getParentAssetIdsAsString(), mAssetCache, sImportIdFormat, a_bSynch);
/* 471 */               asset.setParentAssetIdsAsString(sIds);
/*     */ 
/* 473 */               sIds = getExportedAssetIds(null, asset.getPeerAssetIdsAsString(), mAssetCache, sImportIdFormat, a_bSynch);
/* 474 */               asset.setPeerAssetIdsAsString(sIds);
/*     */ 
/* 476 */               sIds = getExportedAssetIds(null, asset.getChildAssetIdsAsString(), mAssetCache, sImportIdFormat, a_bSynch);
/* 477 */               asset.setChildAssetIdsAsString(sIds);
/*     */ 
/* 479 */               mAssetCache.put(Long.valueOf(asset.getId()), asset);
/*     */             }
/*     */ 
/* 485 */             fileWriter.writeBeans(vAssets, wrapper, bIsFirst);
/*     */ 
/* 487 */             if (a_lUserId > 0L)
/*     */             {
/* 489 */               for (int i = 0; i < vAssets.size(); i++)
/*     */               {
/* 491 */                 long lId = ((LightweightAsset)vAssets.elementAt(i)).getId();
/* 492 */                 addMessage(a_lUserId, "Exported asset with id: " + lId);
/* 493 */                 iExportCount++;
/*     */               }
/*     */             }
/*     */           }
/*     */         }
/*     */ 
/* 499 */         bIsFirst = false;
/*     */       }
/* 501 */       while (iSize > 0);
/*     */ 
/* 504 */       result = new ExportResult();
/* 505 */       result.setDataFile(new FileBean(sFilename + "." + extension, sDataFilepath));
/* 506 */       result.setExportCount(iExportCount);
/* 507 */       result.setTotalFileCount(iNumFiles);
/*     */ 
/* 509 */       if (a_bFilesInZips)
/*     */       {
/* 511 */         String[] saZipFilePaths = this.m_fileStoreManager.createZipFilesForDownload(sFilename, vSourceFilenames, new Vector(hmDestFilenames.values()), AssetBankSettings.getDownloadZipMaxSizeInMBs() * 1024 * 1024, StoredFileType.EXPORT);
/*     */ 
/* 520 */         if (saZipFilePaths.length > 1)
/*     */         {
/* 522 */           for (int i = 0; i < saZipFilePaths.length; i++)
/*     */           {
/* 524 */             result.getZipFiles().add(new FileBean(sFilename = sFilename + "_" + i + ".zip", saZipFilePaths[i]));
/*     */           }
/*     */         }
/* 527 */         else if (saZipFilePaths.length == 1)
/*     */         {
/* 529 */           result.getZipFiles().add(new FileBean(sFilename += ".zip", saZipFilePaths[0]));
/*     */         }
/*     */       }
/*     */       else
/*     */       {
/* 534 */         Collection<String> destinationFiles = hmDestFilenames.values();
/* 535 */         int iFile = 0;
/*     */ 
/* 538 */         StorageDevice device = this.m_storageDeviceManager.getDeviceForNewFile(null, StorageDeviceType.getTypeFor(StoredFileType.EXPORT));
/* 539 */         String sDestBasePath = device.getFullLocalBasePath(StoredFileType.EXPORT) + '/' + sFilename + '/';
/*     */ 
/* 541 */         FileUtils.forceMkdir(new File(sDestBasePath));
/*     */ 
/* 543 */         for (String sDestFilename : destinationFiles)
/*     */         {
/* 545 */           String sFullSourcePath = this.m_fileStoreManager.getAbsolutePath((String)vSourceFilenames.get(iFile++));
/* 546 */           String sFullDestPath = sDestBasePath + sDestFilename;
/*     */ 
/* 548 */           FileUtils.copyFile(new File(sFullSourcePath), new File(sFullDestPath));
/*     */         }
/*     */ 
/* 551 */         result.setFileExportLocation(FilenameUtils.separatorsToSystem(sDestBasePath));
/*     */       }
/*     */     }
/*     */     catch (IOException ioe)
/*     */     {
/* 556 */       throw new Bn2Exception("AssetExportManager.exportAssets() : Unable to create file for output", ioe);
/*     */     }
/*     */     catch (SQLException se)
/*     */     {
/* 560 */       throw new Bn2Exception("AssetExportManager.exportAssets() : SQL Exception: ", se);
/*     */     }
/*     */     finally
/*     */     {
/* 565 */       if (dbTransaction != null)
/*     */       {
/*     */         try
/*     */         {
/* 569 */           dbTransaction.commit();
/*     */         }
/*     */         catch (SQLException sqle)
/*     */         {
/* 573 */           this.m_logger.error("SQL Exception whilst trying to close connection " + sqle.getMessage());
/*     */         }
/*     */       }
/*     */ 
/* 577 */       if (writer != null)
/*     */       {
/*     */         try
/*     */         {
/* 581 */           writer.close();
/*     */         }
/*     */         catch (IOException ioe)
/*     */         {
/*     */         }
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 590 */     return result;
/*     */   }
/*     */ 
/*     */   private String getIdFormat(boolean a_bDataOnly, boolean a_bIsSync)
/*     */   {
/* 596 */     if (a_bDataOnly)
/*     */     {
/* 598 */       return "none";
/*     */     }
/* 600 */     if (a_bIsSync)
/*     */     {
/* 602 */       return String.valueOf('i');
/*     */     }
/* 604 */     return AssetBankSettings.getExportedAssetFilenameFormat();
/*     */   }
/*     */ 
/*     */   private String getExportedAssetIds(DBTransaction a_dbTransaction, String a_sIds, Map<Long, Asset> a_mAssetCache, String a_sIdFormat, boolean a_bSynch)
/*     */     throws Bn2Exception
/*     */   {
/* 622 */     StringBuffer sbNewIdString = new StringBuffer();
/*     */ 
/* 624 */     if (StringUtils.isNotEmpty(a_sIds))
/*     */     {
/* 626 */       List<Long> ids = StringUtil.convertToListOfLongs(a_sIds.replaceAll(" ", ""), ",");
/*     */ 
/* 628 */       for (Long lId : ids)
/*     */       {
/* 630 */         Asset related = null;
/* 631 */         if (a_mAssetCache.containsKey(lId))
/*     */         {
/* 633 */           related = (Asset)a_mAssetCache.get(lId);
/*     */         }
/*     */         else
/*     */         {
/* 637 */           related = this.m_assetManager.getAsset(a_dbTransaction, lId.longValue(), null, false, false);
/* 638 */           a_mAssetCache.put(Long.valueOf(related.getId()), related);
/*     */         }
/*     */ 
/* 641 */         if (sbNewIdString.length() > 0)
/*     */         {
/* 643 */           sbNewIdString.append(",");
/*     */         }
/* 645 */         sbNewIdString.append(getExportFilename(related, a_sIdFormat, a_bSynch, false, AssetBankSettings.getPublishOrignalFileDestinationFormat()));
/*     */       }
/*     */     }
/* 648 */     return sbNewIdString.toString();
/*     */   }
/*     */ 
/*     */   private static String getExportFilename(Asset a_asset, String a_sFilenameFormat, boolean a_bSynch, boolean a_bAddFileExtension, String a_sSuffix)
/*     */   {
/* 665 */     String sFilename = "";
/* 666 */     if (StringUtils.isEmpty(a_sFilenameFormat))
/*     */     {
/* 669 */       sFilename = a_asset.getId() + k_sExportFilenameFieldDelimiter + a_asset.getFileName();
/*     */     }
/*     */     else
/*     */     {
/* 673 */       for (int i = 0; i < a_sFilenameFormat.length(); i++)
/*     */       {
/* 675 */         switch (a_sFilenameFormat.charAt(i))
/*     */         {
/*     */         case 'i':
/* 678 */           sFilename = sFilename + (sFilename.length() > 0 ? AssetBankSettings.getExportedAssetFilenameFieldDelimiter() : "");
/* 679 */           sFilename = sFilename + String.valueOf(a_asset.getId());
/* 680 */           break;
/*     */         case 't':
/* 682 */           sFilename = sFilename + (sFilename.length() > 0 ? AssetBankSettings.getExportedAssetFilenameFieldDelimiter() : "");
/* 683 */           sFilename = sFilename + replaceFilenameFieldSeparator(a_asset.getName());
/* 684 */           break;
/*     */         case 'f':
/* 686 */           sFilename = sFilename + (sFilename.length() > 0 ? AssetBankSettings.getExportedAssetFilenameFieldDelimiter() : "");
/* 687 */           sFilename = sFilename + SynchUtil.getExportFilenameNoExtension(a_asset);
/*     */         }
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 693 */     if (a_bSynch)
/*     */     {
/* 695 */       sFilename = sFilename + "_synch";
/*     */     }
/*     */ 
/* 698 */     sFilename = FileUtil.getSafeFilename(sFilename, false);
/*     */ 
/* 700 */     if (a_bAddFileExtension)
/*     */     {
/* 702 */       if (!StringUtil.stringIsPopulated(a_sSuffix))
/*     */       {
/* 704 */         a_sSuffix = FileUtil.getSuffix(a_asset.getFileName());
/*     */       }
/*     */ 
/* 707 */       sFilename = sFilename + "." + a_sSuffix;
/*     */     }
/* 709 */     return sFilename;
/*     */   }
/*     */ 
/*     */   private static String getUniqueFilename(String a_sFilename, Map<String, String> a_filenames) {
/* 715 */     int iUniquenessSuffix = 0;
/*     */     String sNewFilename;
/*     */     do {
/* 719 */       sNewFilename = FileUtil.getSafeFilename(new StringBuilder().append(FilenameUtils.getBaseName(a_sFilename)).append(iUniquenessSuffix > 0 ? "_" + iUniquenessSuffix : "").toString(), false) + "." + FilenameUtils.getExtension(a_sFilename);
/*     */ 
/* 721 */       iUniquenessSuffix++;
/*     */     }
/* 723 */     while (a_filenames.containsKey(sNewFilename));
/* 724 */     return sNewFilename;
/*     */   }
/*     */ 
/*     */   public void deleteExportedAssetsAsynchronously(final String a_sFileLocation)
/*     */     throws Bn2Exception
/*     */   {
/* 741 */     this.m_hmDeleteResults.remove(a_sFileLocation);
/*     */ 
/* 743 */     Thread thread = new Thread()
/*     */     {
/*     */       public void run()
/*     */       {
/*     */         try
/*     */         {
/* 750 */           AssetExportManager.this.deleteExportedAssets(a_sFileLocation);
/*     */         }
/*     */         catch (Bn2Exception bn2e)
/*     */         {
/* 754 */           AssetExportManager.this.m_logger.error("deleteExportedAssetsAsynchronously : " + bn2e);
/*     */         }
/*     */       }
/*     */     };
/* 759 */     thread.start();
/*     */   }
/*     */ 
/*     */   public DeleteResult getDeletionResult(String a_sFileLocation)
/*     */     throws Bn2Exception
/*     */   {
/* 776 */     DeleteResult result = (DeleteResult)this.m_hmDeleteResults.get(a_sFileLocation);
/* 777 */     if (result != null)
/*     */     {
/* 779 */       this.m_hmDeleteResults.remove(a_sFileLocation);
/*     */     }
/* 781 */     return result;
/*     */   }
/*     */ 
/*     */   public void deleteExportedAssets(String a_sFileLocation)
/*     */     throws Bn2Exception
/*     */   {
/* 792 */     String sFileLocation = null;
/* 793 */     BufferedReader reader = null;
/* 794 */     DeleteResult result = new DeleteResult();
/*     */ 
/* 796 */     sFileLocation = this.m_fileStoreManager.getAbsolutePath(a_sFileLocation);
/*     */     try
/*     */     {
/* 799 */       reader = new BufferedReader(new FileReader(new File(sFileLocation)));
/* 800 */       FileFormat format = new ExcelFormat();
/*     */ 
/* 803 */       CategoryHandler categoryHandler = new CategoryIgnoreHandler();
/* 804 */       BeanWrapper wrapper = new ExternalAssetImpl(categoryHandler);
/* 805 */       DefaultBeanReader assetReader = new DefaultBeanReader(reader, format, Asset.class, wrapper, 1);
/*     */ 
/* 811 */       if ((assetReader.getMissingHeaders() != null) && (assetReader.getMissingHeaders().size() > 0))
/*     */       {
/* 813 */         throw new Bn2Exception("AssetImportManager.importAssetData() : NoMatchForColumnHeaderException due to unexpected column");
/*     */       }
/*     */ 
/* 816 */       Vector vAssets = null;
/* 817 */       boolean bFinished = false;
/* 818 */       int iLineNo = 1;
/* 819 */       int iNumAssetsToRead = 1;
/* 820 */       Vector vUpdatedAssets = new Vector(100);
/*     */       do
/*     */       {
/*     */         try
/*     */         {
/* 826 */           vAssets = assetReader.readBeans(iNumAssetsToRead);
/*     */ 
/* 828 */           if (vAssets.size() > 0)
/*     */           {
/* 830 */             vUpdatedAssets.clear();
/* 831 */             int iStackTraceCount = 0;
/*     */ 
/* 834 */             for (int i = 0; i < vAssets.size(); i++)
/*     */             {
/* 836 */               iLineNo++;
/* 837 */               Asset asset = (Asset)vAssets.get(i);
/*     */               try
/*     */               {
/* 841 */                 asset.setId(Long.parseLong(asset.getImportedAssetId()));
/* 842 */                 this.m_assetManager.deleteAsset(null, asset.getId());
/* 843 */                 result.setNumDeleted(result.getNumDeleted() + 1);
/*     */               }
/*     */               catch (NumberFormatException nfe)
/*     */               {
/* 847 */                 this.m_logger.error("AssetExportManager.deleteExportedAssets() : id exported to file " + sFileLocation + " is not numeric: " + asset.getImportedAssetId());
/*     */ 
/* 849 */                 result.setNumErrors(result.getNumErrors() + 1);
/*     */               }
/*     */               catch (Throwable t)
/*     */               {
/* 853 */                 if (iStackTraceCount++ < 5)
/*     */                 {
/* 855 */                   this.m_logger.error("AssetExportManager.deleteExportedAssets() : unexpected exception caught while reading line " + iLineNo + " of file " + sFileLocation, t);
/*     */                 }
/*     */                 else
/*     */                 {
/* 860 */                   this.m_logger.error("AssetExportManager.deleteExportedAssets() : unexpected exception caught while reading line " + iLineNo + " of file " + sFileLocation + " (skipping stack trace due to high error count)");
/*     */                 }
/*     */ 
/* 863 */                 result.setNumErrors(result.getNumErrors() + 1);
/*     */               }
/*     */             }
/*     */           }
/*     */           else
/*     */           {
/* 869 */             bFinished = true;
/*     */           }
/*     */         }
/*     */         catch (DefaultBeanReader.TooManyColumnsException e)
/*     */         {
/* 874 */           this.m_logger.error("AssetImportManager.importAssetData() : TooManyColumnsException reading line " + (iLineNo + 1) + ": " + e.getMessage());
/* 875 */           iLineNo += iNumAssetsToRead;
/* 876 */           result.setNumErrors(result.getNumErrors() + 1);
/*     */         }
/*     */         catch (DefaultBeanReader.BeanPopulationException e)
/*     */         {
/* 880 */           this.m_logger.error("AssetImportManager.importAssetData() : BeanPopulationException reading line " + (iLineNo + 1) + ": " + e.getMessage());
/* 881 */           iLineNo += iNumAssetsToRead;
/* 882 */           result.setNumErrors(result.getNumErrors() + 1);
/*     */         }
/*     */       }
/* 885 */       while (!bFinished);
/*     */     }
/*     */     catch (FileNotFoundException fnfe)
/*     */     {
/* 889 */       throw new Bn2Exception("AssetImportManager.importAssetData() : input data file not found: " + sFileLocation, fnfe);
/*     */     }
/*     */     catch (IOException ioe)
/*     */     {
/* 893 */       throw new Bn2Exception("AssetImportManager.importAssetData() : IO problem with data file : " + sFileLocation, ioe);
/*     */     }
/*     */ 
/* 897 */     if (result.getNumDeleted() > 0)
/*     */     {
/* 899 */       this.m_assetManager.clearAssetCaches();
/*     */     }
/*     */ 
/* 902 */     this.m_hmDeleteResults.put(a_sFileLocation, result);
/*     */   }
/*     */ 
/*     */   public static String replaceFilenameFieldSeparator(String source)
/*     */   {
/* 916 */     if (source != null)
/*     */     {
/* 918 */       return source.replace(k_sExportFilenameFieldDelimiter, k_sExportFilenameDelimiterReplacement);
/*     */     }
/* 920 */     return "null";
/*     */   }
/*     */ 
/*     */   public void setTransactionManager(DBTransactionManager a_sTransactionManager)
/*     */   {
/* 925 */     this.m_transactionManager = a_sTransactionManager;
/*     */   }
/*     */ 
/*     */   public void setFileStoreManager(FileStoreManager a_sFileStoreManager)
/*     */   {
/* 930 */     this.m_fileStoreManager = a_sFileStoreManager;
/*     */   }
/*     */ 
/*     */   public void setAssetManager(AssetManager a_sAssetManager)
/*     */   {
/* 935 */     this.m_assetManager = a_sAssetManager;
/*     */   }
/*     */ 
/*     */   public void setAttributeManager(AttributeManager a_sAttributeManager)
/*     */   {
/* 940 */     this.m_attributeManager = a_sAttributeManager;
/*     */   }
/*     */ 
/*     */   public void setAttributeValueManager(AttributeValueManager a_attributeValueManager)
/*     */   {
/* 945 */     this.m_attributeValueManager = a_attributeValueManager;
/*     */   }
/*     */ 
/*     */   public void setStorageDeviceManager(StorageDeviceManager a_deviceManager)
/*     */   {
/* 951 */     this.m_storageDeviceManager = a_deviceManager;
/*     */   }
/*     */ }

/* Location:           C:\Users\mamatha\Desktop\com.zip
 * Qualified Name:     com.bright.assetbank.synchronise.service.AssetExportManager
 * JD-Core Version:    0.6.0
 */