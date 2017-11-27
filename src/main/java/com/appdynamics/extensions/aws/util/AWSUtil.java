package com.appdynamics.extensions.aws.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.CredentialsDecryptionConfig;
import com.appdynamics.extensions.aws.config.ProxyConfig;
import com.appdynamics.extensions.crypto.Decryptor;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;

/**
 * @author Florencio Sarmiento
 */
public class AWSUtil {

    public static Long convertToLong(Double metricValue) {
        Long value = null;

        if (metricValue == null || metricValue < 0) {
            value = 0L;

        } else {
            value = Math.round(metricValue);
        }

        return value;
    }

    public static String resolvePath(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }

        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }

    public static AWSCredentials createAWSCredentials(Account account,
                                                      CredentialsDecryptionConfig credentialsDecryptionConfig) {
        String awsAccessKey = account.getAwsAccessKey();
        String awsSecretKey = account.getAwsSecretKey();

        if (credentialsDecryptionConfig != null &&
                credentialsDecryptionConfig.isDecryptionEnabled()) {
            Decryptor decryptor = new Decryptor(credentialsDecryptionConfig.getDecryptionKey());
            awsAccessKey = decryptor.decrypt(awsAccessKey);
            awsSecretKey = decryptor.decrypt(awsSecretKey);
        }

        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        return awsCredentials;
    }

    public static ClientConfiguration createAWSClientConfiguration(int maxErrorRetrySize,
                                                                   ProxyConfig proxyConfig) {
        ClientConfiguration awsClientConfig = new ClientConfiguration();
        awsClientConfig.setMaxErrorRetry(maxErrorRetrySize);

        if (proxyConfig != null && StringUtils.isNotBlank(proxyConfig.getHost()) &&
                proxyConfig.getPort() != null) {
            awsClientConfig.setProxyHost(proxyConfig.getHost());
            awsClientConfig.setProxyPort(proxyConfig.getPort());
            awsClientConfig.setProxyUsername(proxyConfig.getUsername());
            awsClientConfig.setProxyPassword(proxyConfig.getPassword());
        }

        return awsClientConfig;
    }

    //methods to ensure that the aws access keys do not get checked in by a developer

    public static List<String> getAWSSecretKeysFromCfg(File[] configFiles) {
        List<String> secretKeysFromCfg = Lists.newArrayList();
        for (File configFile : configFiles) {
            Map<String, ?> config = YmlReader.readFromFile(configFile);
            List<Map> accounts = (List) config.get("accounts");
            if (accounts != null) {
                for (Map account : accounts) {
                    secretKeysFromCfg.add(account.get("awsSecretKey").toString());
                }
            }
        }
        return secretKeysFromCfg;
    }

    public static List<String> getAWSAccessKeysFromCfg(File[] configFiles) {
        List<String> accessKeysFromCfg = Lists.newArrayList();
        for (File configFile : configFiles) {
            Map<String, ?> config = YmlReader.readFromFile(configFile);
            List<Map> accounts = (List) config.get("accounts");
            if (accounts != null) {
                for (Map account : accounts) {
                    accessKeysFromCfg.add(account.get("awsAccessKey").toString());
                }
            }
        }
        return accessKeysFromCfg;
    }

    public static File[] getConfigFilesFromDir(String dirName) {
        File dir = new File(dirName);
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".yaml") || filename.endsWith(".yml");
            }
        });
    }
}
