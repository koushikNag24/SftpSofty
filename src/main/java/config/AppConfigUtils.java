package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfigUtils {
    private static Properties loadProperties(String fileName) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(fileName)) {
            properties.load(input);
        }
        return properties;
    }
    public static AppConfig getAppConfig(String configFilePath) throws IOException {
        Properties properties = loadProperties(configFilePath);
        String sftpHost=properties.getProperty("sftp_host");
        String sftpPort=properties.getProperty("sftp_port");
        String sftpUsername=properties.getProperty("sftp_username");
        String sftpPassword=properties.getProperty("sftp_password");
        String remoteDirectory=properties.getProperty("remote_directory");
        String localDirectory=properties.getProperty("local_directory");
        String zipFile=properties.getProperty("zip_file");
        Boolean showRemoteContent=properties.getProperty("do_show_remote").equalsIgnoreCase("Yes");
        String remoteOsSeparator=properties.getProperty("remote_os_separator");
        Boolean doZip= properties.getProperty("do_zip").equalsIgnoreCase("Yes");
        Boolean doDelete=properties.getProperty("do_delete").equalsIgnoreCase("Yes");
        AppConfig appConfig=new AppConfig();
        appConfig.setSftpHost(sftpHost);
        appConfig.setShowRemoteContent(showRemoteContent);
        appConfig.setDoDelete(doDelete);
        appConfig.setDoZip(doZip);
        appConfig.setRemoteOsSeparator(remoteOsSeparator);
        appConfig.setSftpPassword(sftpPassword);
        appConfig.setLocalDirectory(localDirectory);
        appConfig.setSftpPort(sftpPort);
        appConfig.setRemoteDirectory(remoteDirectory);
        appConfig.setSftpUsername(sftpUsername);
        appConfig.setZipFile(zipFile);
        return appConfig;
    }
}
