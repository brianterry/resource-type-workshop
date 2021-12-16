package com.awssamples.ec2.importkeypair;


import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.cloudformation.proxy.*;


import java.util.List;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        AmazonWebServicesClientProxy proxy,
          ResourceHandlerRequest<ResourceModel> request,
          CallbackContext callbackContext,
          ProxyClient<Ec2Client> proxyClient,
          Logger logger){


        final DescribeKeyPairsRequest describeKeyPairRequest = DescribeKeyPairsRequest
                .builder()
                .build();
        final DescribeKeyPairsResponse describeKeyPairsResponse = proxy
                .injectCredentialsAndInvokeV2(describeKeyPairRequest,
                        proxyClient.client()::describeKeyPairs);

        final List<ResourceModel> models = Translator
                .translateFromListRequest(describeKeyPairsResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
