package com.awssamples.ec2.importkeypair;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;
import software.amazon.cloudformation.proxy.*;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<Ec2Client> proxyClient, final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> createKeyPair(proxy, proxyClient, model, callbackContext));
    }

    private ProgressEvent<ResourceModel, CallbackContext> createKeyPair(final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient, final ResourceModel model, final CallbackContext context) {
        return proxy.initiate("AWS-EC2-KeyPair::Create", proxyClient, model, context)
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((importKeyPairRequest, client) -> {
                    ImportKeyPairResponse importKeyPairResponse = null;
                
                        importKeyPairResponse = proxyClient.injectCredentialsAndInvokeV2(importKeyPairRequest,
                                proxyClient.client()::importKeyPair);
                    logger.log(String.format("%s request successfully created. Awaiting accept.",
                            ResourceModel.TYPE_NAME));
                    return importKeyPairResponse;
                })
                .stabilize((importKeyPairRequest, importKeyPairResponse, cbProxyClient, resourceModel,
                        pcontext) -> stabilizeOnCreate(logger, importKeyPairResponse, cbProxyClient, resourceModel))
                .handleError(this::handleError)

                .success();
    }

    private Boolean stabilizeOnCreate(Logger logger, ImportKeyPairResponse importKeyPairResponse,
            ProxyClient<Ec2Client> cbProxyClient, ResourceModel model) {
        model.setKeyPairId(importKeyPairResponse.keyPairId());
        model.setKeyFingerprint(importKeyPairResponse.keyFingerprint());
        return true;
    }
}

