package com.secpro.platform.monitoring.agent.utils.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.core.utils.Constants;
import com.secpro.platform.log.utils.PlatformLogger;

/**
 * @author baiyanwei Sep 17, 2013
 * 
 * 
 *         store sample into local file system.
 */
final public class FileSystemStorageUtil {
	final private static PlatformLogger theLogger = PlatformLogger.getLogger(FileSystemStorageUtil.class);

	/**
	 * store a sample into local file system.
	 * 
	 * @param path
	 * @param fileContent
	 * @return
	 */
	final public static String storeSampleDateToFile(String path, String fileContent) throws Exception {
		if (Assert.isEmptyString(path) == true || Assert.isEmptyString(fileContent) == true) {
			return null;
		}
		PrintWriter filePrinter = null;
		try {
			File taskFile = new File(path);
			if (taskFile.getParentFile().exists() == false) {
				taskFile.getParentFile().mkdirs();
			}
			if (taskFile.exists() == false) {
				taskFile.createNewFile();
			}
			filePrinter = new PrintWriter(taskFile, Constants.DEFAULT_ENCODING);
			filePrinter.print(fileContent);
			filePrinter.flush();
		} catch (Exception e) {
			theLogger.exception("Create task file error", e);
			throw e;
		} finally {
			if (filePrinter != null) {
				filePrinter.close();
				filePrinter = null;
			}
		}
		return path;
	}

	/**
	 * read a file content . and remove is or not.
	 * 
	 * @param pathName
	 * @param isRemove
	 * @return
	 * @throws Exception
	 */
	final public static String readSampleDateToFile(String pathName, boolean isRemove) throws Exception {
		if (Assert.isEmptyString(pathName) == true) {
			throw new Exception("parameter pathName is invalid");
		}
		File targetFile = new File(pathName);
		if (targetFile.exists() == false) {
			throw new Exception(pathName + " doesn't exist");
		}
		if (targetFile.isFile() == false) {
			throw new Exception(pathName + " is not a file");
		}
		FileReader fileReader = null;
		StringBuffer contentBuff = new StringBuffer();
		try {
			fileReader = new FileReader(targetFile);
			int fileIndex;
			// Read characters
			while ((fileIndex = fileReader.read()) != -1) {
				contentBuff.append((char) fileIndex);
			}
		} catch (Exception e) {
			theLogger.exception(e);
		} finally {
			if (fileReader != null) {
				// Close file reader
				try {
					fileReader.close();
					fileReader = null;
				} catch (IOException e) {
				}
			}
		}
		if (isRemove) {
			targetFile.delete();
		}
		return contentBuff.toString();
	}
}
