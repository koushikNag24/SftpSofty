package ftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import config.AppConfig;
import org.apache.commons.net.ftp.FTPClient;
import org.example.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class FtpUtils {
    public static String LOCAL_OS_FILE_SEPARATOR = System.getProperty("file.separator");

    private static final Logger logger = LoggerFactory.getLogger(FtpUtils.class);

    private static void zipFile(String sourceFilePath, String zipFilePath) throws Exception {
        // Implementation to zip the file goes here
        // You can use java.util.zip.ZipOutputStream or any library like Apache Commons Compress
        // For simplicity, let's assume you've already zipped the file.
        // Make sure to handle exceptions appropriately.
    }

    public   void uploadFileToSFTP(AppConfig appConfig) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession(appConfig.getSftpUsername(), appConfig.getSftpHost(), Integer.parseInt(appConfig.getSftpPort()));
        session.setPassword(appConfig.getSftpPassword());
        long timeOutSeconds = 10;

        // Convert seconds to milliseconds using TimeUnit
        int timeOut = (int) TimeUnit.SECONDS.toMillis(timeOutSeconds);
        session.setTimeout(timeOut);
        // Avoid checking the key when connecting
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();

        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            File localDirectory = new File(appConfig.getLocalDirectory());
            if(!localDirectory.exists() || !localDirectory.isDirectory() || !localDirectory.canRead()){
//                logger.warning("Local Sftp Directory [%s] can not be read ".formatted(localDirectory));
                logger.warn(String.format("Local Sftp Directory [%s] can not be read", localDirectory));
                return;
            }
            File[] filesToBeSftp= localDirectory.listFiles();
            if(filesToBeSftp==null || filesToBeSftp.length==0){
                logger.warn("No files to SFTP");
                return;
            }
            recursivelyUploadDirectories(channelSftp, appConfig.getLocalDirectory(), appConfig.getRemoteDirectory(),appConfig);


            if(appConfig.getShowRemoteContent()){
                Vector<ChannelSftp.LsEntry> files = channelSftp.ls(appConfig.getRemoteDirectory());

                logger.info(String.format("============================ Remote Directory [%s] ============================", appConfig.getRemoteDirectory()));
                for (ChannelSftp.LsEntry file : files) {
                    if(file.getFilename().startsWith("."))continue;
                    logger.info("Remote File: " + file.getFilename());
                }
            }


            channelSftp.disconnect();
        }catch (SftpException sftpException){
            sftpException.printStackTrace();
            logger.warn(" Destination may have the same directory");
        }
        finally {
            session.disconnect();
        }
    }
    private static void  recursivelyUploadDirectories(ChannelSftp channelSftp, String localDirectory, String remoteDir,AppConfig appConfig)  {
//        logger.info(" Will attempt to upload files from %s".formatted(localDirectory));
        File localDir = new File(localDirectory);

        File[] localDirectoryFiles=localDir.listFiles();

        if(localDirectoryFiles==null || localDirectoryFiles.length<1){
            logger.warn(String.format("No files to Transfer in %s", localDir));
            return;
        }
        for(File aFile : localDirectoryFiles){

            String remoteDirectory=remoteDir+ appConfig.getRemoteOsSeparator()+aFile.getName();
//            logger.info("Attempt to create %s".formatted(remoteDirectory));
            if(isFileModifiedWithinLast24Hours(aFile.toPath())){
                logger.warn(String.format("[%s] is not 24 hours stale",aFile.getAbsolutePath()));
                continue;
            }
            if (aFile.isDirectory()) {
                try {
                    channelSftp.mkdir(remoteDirectory);
                } catch (SftpException e) {
                    logger.warn(String.format("Remote location may have a directory with the name %s",remoteDirectory));
                }
                try {
                    channelSftp.cd(remoteDirectory);
                } catch (SftpException e) {
                    logger.warn(String.format(" Remote location may not have a directory with the name %s",(remoteDirectory)));
                }
                for (File file : aFile.listFiles()) {
//                    logger.info("  Input File %s".formatted(file.getAbsolutePath()));
                    if (file.isFile()) {
                        String remotePath=remoteDirectory + appConfig.getRemoteOsSeparator()+ file.getName();
//                        logger.info(" %s will upload to %s".formatted(file.getAbsolutePath(),remotePath));

                        try {
                            logger.info(String.format("Attempting upload from %s to %s", file.getAbsolutePath(), remotePath));
                            channelSftp.put(file.getAbsolutePath(),remotePath );
                            logger.info(String.format("%s uploaded to %s", file.getAbsolutePath(), remotePath));
                            if(appConfig.getDoDelete()){
                                deleteFile(Paths.get(file.getAbsolutePath()));
                            }

                        } catch (SftpException e) {
                            logger.warn(String.format("Error while uploading file source [%s] remote [%s]", file.getAbsolutePath(), remotePath));
                        }

                    } else if (file.isDirectory()) {
                        recursivelyUploadDirectories(channelSftp, file.getAbsolutePath(), remoteDirectory,appConfig );
                    }
                }

                deleteEmptyDirectories(aFile);
            }else{
                try {
                    logger.info(String.format("Attempting upload from %s to %s", aFile.getAbsolutePath(), remoteDirectory));
                    channelSftp.put(aFile.getAbsolutePath(),remoteDirectory );
                    logger.info(String.format("%s uploaded to %s", aFile.getAbsolutePath(), remoteDirectory));
                    if(appConfig.getDoDelete()){
                        deleteFile(Paths.get(aFile.getAbsolutePath()));
                    }

                } catch (SftpException e) {
                    logger.warn(String.format("Error while uploading file source [%s] remote [%s]", aFile.getAbsolutePath(), remoteDirectory));
//                    logger.warning(" Error while uploading file source [%s] remote [%s]".formatted(aFile.getAbsolutePath(),remoteDirectory));
                }

//                logger.info(" %s uploaded to %s".formatted(aFile.getAbsolutePath(),remoteDirectory));
            }
        }

    }
    private static void deleteFile(Path file){
        try {
            Files.delete(file);
            logger.info(String.format("%s is deleted successfully %s",file,!Files.exists(file)));
        } catch (IOException e) {
            logger.info(String.format("Error encountered while deleting %s",file));
        }

    }
    public static void deleteEmptyDirectories(File directory) {
        if(isFileModifiedWithinLast24Hours(directory.toPath())){
            logger.warn(String.format("[%s] is not 24 hours stale",directory.getAbsolutePath()));
            return;
        }
        // Check if the directory exists
        if (directory.exists()) {
            // Get the list of files and subdirectories in the current directory
            File[] files = directory.listFiles();

            // Check if the directory is empty
            if (files != null && files.length == 0) {
                // Delete the empty directory
                if (directory.delete()) {
                    logger.info("Deleted empty directory: " + directory.getAbsolutePath());
                } else {
                    logger.info("Failed to delete directory: " + directory.getAbsolutePath());
                }
            } else if (files != null) {
                // If the directory is not empty, recursively delete empty directories in its subdirectories
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteEmptyDirectories(file);
                    }
                }
            }
        } else {
            logger.info("Directory does not exist: " + directory.getAbsolutePath());
        }
    }
    private static boolean isFileModifiedWithinLast24Hours(Path file) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

            return lastModifiedTime.toInstant().isAfter(twentyFourHoursAgo);
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Handle the exception according to your needs
        }
    }
//            Files.delete(localDir.toPath());
//            logger.info(" %s was deleted successfully ? %s".formatted(localDir.getAbsolutePath(),!Files.exists(Path.of(localDir.getAbsolutePath()))));





}
