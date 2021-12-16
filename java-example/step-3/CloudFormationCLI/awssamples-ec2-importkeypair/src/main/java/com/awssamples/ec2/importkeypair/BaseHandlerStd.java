package com.awssamples.ec2.importkeypair;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.LimitExceededException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Ec2Request;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final Logger logger) {
    return handleRequest(
      proxy,
      request,
      callbackContext != null ? callbackContext : new CallbackContext(),
      proxy.newProxy(ClientBuilder::getClient),
      logger
    );
  }

  protected String KEYPAIR_NOT_FOUND_ERROR = "InvalidKeyPair.NotFound";

  protected DescribeKeyPairsResponse getKeyPairsResponse(
            final ProxyClient<Ec2Client> proxyClient) {
        final DescribeKeyPairsRequest describeVpceyPairsRequest = DescribeKeyPairsRequest
                .builder().build();
        return this.getKeyPairsResponse(describeVpceyPairsRequest, proxyClient);
    }

  protected DescribeKeyPairsResponse getKeyPairsResponse(
            final DescribeKeyPairsRequest describeKeyPairsRequest,
            final ProxyClient<Ec2Client> proxyClient) {
        DescribeKeyPairsResponse describeKeyPairsResponse = null;
        
            describeKeyPairsResponse = proxyClient
                    .injectCredentialsAndInvokeV2(describeKeyPairsRequest, proxyClient.client()::describeKeyPairs);
        return describeKeyPairsResponse;
    }

  protected ProgressEvent<ResourceModel, CallbackContext> handleError(
            final Ec2Request request, final Exception e,
            final ProxyClient<Ec2Client> proxyClient, final ResourceModel resourceModel,
            final CallbackContext callbackContext) {
        BaseHandlerException ex;
        if (e instanceof CfnNotFoundException) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        }
        else if (e instanceof LimitExceededException) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceInternalError, e.getMessage());
        } else if (e instanceof Ec2Exception) {
            Ec2Exception ec2e = (Ec2Exception) e;
            if (ec2e.awsErrorDetails() != null && ec2e.awsErrorDetails().errorCode()
                    .equals("InvalidKeyPair.Duplicate"))
            {
                ex = new CfnNotFoundException(ResourceModel.TYPE_NAME, request.toString(), e);
            } else {
                ex = new CfnInternalFailureException(e);
            }
        } else if (e instanceof ResourceNotFoundException) {
            ex = new CfnNotFoundException(e);
        } else if (e instanceof AwsServiceException) {
            ex = new CfnGeneralServiceException(e);
        } else {
            ex = new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
    }

  protected void throwCfnException(final String request, final Exception e) {
        BaseHandlerException ex;
        if ((e instanceof Ec2Exception) && errorMsgIs((Ec2Exception) e, KEYPAIR_NOT_FOUND_ERROR)) {
            ex = new CfnNotFoundException(ResourceModel.TYPE_NAME, request, e);
        }
        else {
            ex = commonHandler(e);
        }
        throw ex;
    }
    protected void throwCfnException(final Exception e) {
        // request string is only used in a special case
        throwCfnException("", e);
    }
  private BaseHandlerException commonHandler(final Exception e) {
        BaseHandlerException ex;
        if (e instanceof LimitExceededException) {
            ex = new CfnServiceLimitExceededException(e);
        } else if (e instanceof Ec2Exception) {
            ex = new CfnInternalFailureException(e);
        // CfnNotFoundException *should* be an instance of ResourceNotFoundException, but is not recognised as such, hence double check
        } else if ((e instanceof ResourceNotFoundException) || (e instanceof CfnNotFoundException)) {
            ex = new CfnNotFoundException(e);
        } else if (e instanceof AwsServiceException) {
            ex = new CfnGeneralServiceException(e);
        } else {
            ex = new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }
        return ex;
    }
    private boolean errorMsgIs(Ec2Exception e, String msg) {
      return e.awsErrorDetails() != null && e.awsErrorDetails().errorCode().equals(msg);
  }

  public RuntimeException handleException(final Exception error, final Ec2Request request) {
    if (error instanceof CfnNotFoundException) {
        return new CfnNotFoundException(ResourceModel.TYPE_NAME, "Resource not found");
    } if (error instanceof LimitExceededException) {
        return new CfnServiceLimitExceededException(error);
    } if (error instanceof Ec2Exception) {
        return new CfnInternalFailureException(error);
    } if (error instanceof AwsServiceException) {
        return new CfnGeneralServiceException(error);
    } else {
        return new CfnGeneralServiceException(ResourceModel.TYPE_NAME, error);
    }
}

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          AmazonWebServicesClientProxy proxy,
          ResourceHandlerRequest<ResourceModel> request,
          CallbackContext callbackContext,
          ProxyClient<Ec2Client> proxyClient,
          Logger logger);
}
