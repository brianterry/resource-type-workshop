package com.awssamples.ec2.importkeypair;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.ec2.Ec2Client;


import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<Ec2Client> proxyClient;

    @Mock
    Ec2Client ec2Client;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        ec2Client = mock(Ec2Client.class);
        proxyClient = MOCK_PROXY(proxy, ec2Client);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .keyName(this.keyName)
                .keyFingerprint(this.keyFingerprint)
                .tags(this.tags)
                .keyPairId(this.keyPairId)
                .publicKeyMaterial(this.publicKeyMaterial)
                .build();
                

        final ImportKeyPairResponse importKeyPairResponse = ImportKeyPairResponse
                .builder()
                .keyName(this.keyName)
                .keyFingerprint(this.keyFingerprint)
                .keyPairId(this.keyPairId)
                .tags(TagHelper.translateTagsFromSdk(this.tags))
                .build();
        when(proxyClient.client().importKeyPair(any(ImportKeyPairRequest.class)))
                .thenReturn(importKeyPairResponse);      

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(ec2Client).importKeyPair(any(ImportKeyPairRequest.class));
        verify(ec2Client, atLeastOnce()).serviceName();
    }
    
    @Test
    public void handleRequest_SimpleSuccess_NoTags() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .keyName(this.keyName)
                .keyFingerprint(this.keyFingerprint)
                .keyPairId(this.keyPairId)
                .publicKeyMaterial(this.publicKeyMaterial)
                .build();
                
        final ImportKeyPairResponse importKeyPairResponse = ImportKeyPairResponse
                .builder()
                .keyName(this.keyName)
                .keyFingerprint(this.keyFingerprint)
                .keyPairId(this.keyPairId)
                .build();
        when(proxyClient.client().importKeyPair(any(ImportKeyPairRequest.class)))
                .thenReturn(importKeyPairResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler
                .handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(ec2Client).importKeyPair(any(ImportKeyPairRequest.class));
        verify(ec2Client, atLeastOnce()).serviceName();
    }
}

