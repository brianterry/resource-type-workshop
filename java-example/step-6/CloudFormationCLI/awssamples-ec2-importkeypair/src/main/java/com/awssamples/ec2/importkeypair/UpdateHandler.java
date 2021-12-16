package com.awssamples.ec2.importkeypair;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteTagsResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.cloudformation.proxy.*;


public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress->new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger))
            .onSuccess(progress -> deleteTags(proxy, proxyClient, model, callbackContext)
            )
            .then(progress -> addTags(proxy, proxyClient, model, callbackContext)
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(model));
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteTags(
            final AmazonWebServicesClientProxy proxy, final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model, final CallbackContext context) {
        return proxy
                .initiate("AAWSSamples-EC2-ImportKeyPair::Update::DeleteTag", proxyClient, model, context)
                .translateToServiceRequest(Translator::translateToRemoveTagsRequest)
                .makeServiceCall((deleteTagRequest, client) -> {
                    DeleteTagsResponse deleteTagsResponse  = null;
                    try {
                        deleteTagsResponse = proxyClient
                                .injectCredentialsAndInvokeV2(deleteTagRequest,
                                        proxyClient.client()::deleteTags);
                    } catch (final Exception e) {
                        throwCfnException(e);
                    }
                    logger.log(String.format("%s tags removed",
                            ResourceModel.TYPE_NAME));
                    return deleteTagsResponse;
                })
                .handleError(this::handleError)
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> addTags(
            final AmazonWebServicesClientProxy proxy, final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model, final CallbackContext context) {
        return proxy
                .initiate("AAWSSamples-EC2-ImportKeyPair::Update::AddTag", proxyClient, model, context)
                .translateToServiceRequest(Translator::translateToAddTagsRequest)
                .makeServiceCall((createTagsRequest, client) -> {
                    CreateTagsResponse createTagsResponse = null;
                    try {
                        createTagsResponse = proxyClient
                                .injectCredentialsAndInvokeV2(createTagsRequest,
                                        proxyClient.client()::createTags);
                    } catch (final Exception e) {
                        throwCfnException(e);
                    }
                    logger.log(String.format("%s tags added",
                            ResourceModel.TYPE_NAME));
                    return createTagsResponse;
                })
                .progress();
    }
}

