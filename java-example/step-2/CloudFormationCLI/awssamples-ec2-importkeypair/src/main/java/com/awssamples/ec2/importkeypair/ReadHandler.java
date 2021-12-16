package com.awssamples.ec2.importkeypair;

import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
            final ProxyClient<Ec2Client> proxyClient, final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> proxy
                        .initiate("AWSSamples-EC2-ImportKeyPair::Read", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall(this::getResource)
                        .done(describeKeyPairsResponse -> {    
                                model.setKeyFingerprint(describeKeyPairsResponse.keyPairs().get(0).keyFingerprint());
                            return ProgressEvent.progress(model, callbackContext);
                        }))
                .then(progress -> {
                    return ProgressEvent.defaultSuccessHandler(model);
                });
    }

    private DescribeKeyPairsResponse getResource(DescribeKeyPairsRequest getRequest,
            final ProxyClient<Ec2Client> proxyClient) {
        DescribeKeyPairsResponse response = null;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(getRequest, proxyClient.client()::describeKeyPairs);
            
            if (response.keyPairs().size() == 0) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, "Resource not found");
            }
        } catch (final Exception e) {
            throw handleException(e, getRequest);
        }
        return response;
    }
}
