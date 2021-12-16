package com.awssamples.ec2.importkeypair;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  protected final String keyName = "foo";
    protected final String keyFingerprint = "654321";
    protected final String publicKeyMaterial = "123456";
    protected final String keyPairId = "key-07a1547691c4a3a22";
    protected final String peerOwnerId = "123456789012";
    protected final String peerRoleArn = MessageFormat.format("arn:aws:iam::{0}:user/user-name-with-path", peerOwnerId);
    protected List<Tag> tags = new ArrayList<Tag>() {
        {
            add(Tag.builder().key("key1").value("value1").build());
            add(Tag.builder().key("key2").value("value2").build());
        }
    };

  protected KeyPairInfo getKeyPairInfo(ResourceModel model) {
        KeyPairInfo info = KeyPairInfo.builder()
                .keyName(model.getKeyName())
                .keyPairId(model.getKeyPairId())
                .keyFingerprint(model.getKeyFingerprint())
                .tags(TagHelper.translateTagsFromSdk(model.getTags()))
                .build();
      return info;
    }

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();
  }
  static ProxyClient<Ec2Client> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final Ec2Client sdkClient) {
    return new ProxyClient<Ec2Client>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Ec2Client client() {
        return sdkClient;
      }
    };
  }
}

