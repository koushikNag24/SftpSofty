package config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.PrimitiveIterator;

@Getter
@Setter
@ToString
public class AppConfig {
    private String remoteOsSeparator;
    private String sftpHost;
    private String sftpPort;
    private String sftpUsername;
    private String sftpPassword;
    private String remoteDirectory;
    private String localDirectory;
    private String zipFile;
    private Boolean doZip;
    private Boolean doDelete;
    private Boolean showRemoteContent;
}
