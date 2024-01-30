package org.example;

import config.AppConfig;
import config.AppConfigUtils;
import ftp.FtpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static final int SLEEP_INTERVAL = 5;

    public static void main( String[] args )
    {
        boolean keepDoing=true;

        while (keepDoing){

        logger.trace("Starting");
        if(args.length!=1){
            logger.error("No Configuration Information is passed via argument");
            return;
        }
        Optional<String> optionalConfigFilePath= Optional.of(args[0]);
        String configPath=optionalConfigFilePath.orElseThrow(()->new IllegalArgumentException(String.format("Configuration Path is not Invalid [%s]",optionalConfigFilePath.get())));

        try {
            Path configFilePath = Paths.get(configPath);
            if (Files.isReadable(configFilePath)) {

                AppConfig appConfig=AppConfigUtils.getAppConfig(String.valueOf(configFilePath));
                logger.trace(appConfig.toString());
                FtpUtils ftpUtils=new FtpUtils();
                ftpUtils.uploadFileToSFTP(appConfig);

            } else {
                logger.error(String.format("Error While Reading the file [%s]",(configFilePath)));
            }
        } catch (InvalidPathException e) {
            logger.warn("Invalid file path: " + e.getMessage());
        }catch (IOException ioException){
            ioException.printStackTrace();
            logger.warn("Error Reading Configuration : " + ioException.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.warn("An unexpected error occurred: " + e.getMessage());
        }
            try {
                TimeUnit.SECONDS.sleep(SLEEP_INTERVAL);
            } catch (InterruptedException e) {
               logger.warn(String.format(" Issue While Sleeping %s",e.getMessage()));
            }
        }
    }
}
