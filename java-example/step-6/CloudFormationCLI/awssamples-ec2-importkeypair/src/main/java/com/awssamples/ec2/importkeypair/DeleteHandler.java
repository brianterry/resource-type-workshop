package com.awssamples.ec2.importkeypair;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getKeyPairId())) {
            return ProgressEvent
                    .failed(model, callbackContext, HandlerErrorCode.NotFound, "Keypair not found");
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress->new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger))
                .onSuccess(progress -> proxy.initiate("AWSSamples-EC2-ImportKeyPair::Delete", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall(this::deleteResource)
                    .progress()
                )
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
    
    private DeleteKeyPairResponse deleteResource(
            DeleteKeyPairRequest getRequest,
            final ProxyClient<Ec2Client> proxyClient) {
        DeleteKeyPairResponse response = null;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(getRequest, proxyClient.client()::deleteKeyPair);
        } catch (final Exception e) {
            throw handleException(e, getRequest);
        }
        return response;
    }
}
