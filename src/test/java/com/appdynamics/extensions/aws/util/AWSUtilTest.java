package com.appdynamics.extensions.aws.util;

import com.amazonaws.auth.AWSCredentials;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.CredentialsDecryptionConfig;
import com.appdynamics.extensions.crypto.Encryptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Satish Muddam
 */
@RunWith(MockitoJUnitRunner.class)
public class AWSUtilTest {

    @Mock
    private Account account;

    @Mock
    private CredentialsDecryptionConfig credentialsDecryptionConfig;

    @Test
    public void testCreateAWSCredentialsWithoutEncryption() {

        Mockito.when(account.getAwsAccessKey()).thenReturn("accessKey1");
        Mockito.when(account.getAwsSecretKey()).thenReturn("secretKey1");

        AWSCredentials awsCredentials = AWSUtil.createAWSCredentials(account, null);

        Assert.assertEquals("accessKey1", awsCredentials.getAWSAccessKeyId());
        Assert.assertEquals("secretKey1", awsCredentials.getAWSSecretKey());
    }

    @Test
    public void testCreateAWSCredentialsWithEncryption() {


        Encryptor encryptor = new Encryptor("test");

        Mockito.when(account.getAwsAccessKey()).thenReturn(encryptor.encrypt("accessKey1"));
        Mockito.when(account.getAwsSecretKey()).thenReturn(encryptor.encrypt("secretKey1"));


        Mockito.when(credentialsDecryptionConfig.isDecryptionEnabled()).thenReturn(true);
        Mockito.when(credentialsDecryptionConfig.getEncryptionKey()).thenReturn("test");

        AWSCredentials awsCredentials = AWSUtil.createAWSCredentials(account, credentialsDecryptionConfig);

        Assert.assertEquals("accessKey1", awsCredentials.getAWSAccessKeyId());
        Assert.assertEquals("secretKey1", awsCredentials.getAWSSecretKey());
    }
}
