package com.awssamples.ec2.importkeypair;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.*;


/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static ImportKeyPairRequest translateToCreateRequest(final ResourceModel model) {
    ImportKeyPairRequest.Builder builder = ImportKeyPairRequest.builder()
            .keyName(model.getKeyName())
            .publicKeyMaterial(SdkBytes.fromUtf8String(model.getPublicKeyMaterial()));
    //  tag specs hasTags() returns true even with zero length tag list
    if (isNotEmpty(model.getTags())) {
      TagSpecification tagSpec = TagSpecification.builder()
              .tags(TagHelper.translateTagsFromSdk(model.getTags()))
              .resourceType(ResourceType.KEY_PAIR).build();
      builder.tagSpecifications(tagSpec);
    }
    return builder.build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeKeyPairsRequest translateToReadRequest(final ResourceModel model) {
    DescribeKeyPairsRequest builder = DescribeKeyPairsRequest.builder()
            .filters(Filter.builder()
                      .name("key-pair-id")
                      .values(model.getKeyPairId())
                      .build())
            .build();
    return builder;
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param response the aws service describe resource response
   * @param resourceModel 
   * @param callbackContext 
   * @return model resource model
   */
  static ProgressEvent<ResourceModel, CallbackContext> translateFromReadResponse(final DescribeKeyPairsResponse response, ResourceModel resourceModel) {
    if (response.keyPairs().size() == 0) {
      return ProgressEvent.defaultFailureHandler(new CfnNotFoundException(resourceModel.getKeyPairId(), resourceModel.getKeyName()), HandlerErrorCode.NotFound);
    }
    ResourceModel builder = ResourceModel.builder()
          .keyName(response.keyPairs().get(0).keyName())
          .keyPairId(response.keyPairs().get(0).keyPairId())
          .keyFingerprint(response.keyPairs().get(0).keyFingerprint())
          .build();

      if (response.keyPairs().get(0).hasTags()){
          builder.setTags(TagHelper.translateTagsToSdk(response.keyPairs().get(0).tags()));
      }
     return ProgressEvent.defaultSuccessHandler(builder);
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteKeyPairRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteKeyPairRequest.builder()
        .keyName(model.getKeyName())
        .keyPairId(model.getKeyPairId())
        .build();
  }

  static CreateTagsRequest translateToAddTagsRequest(String keyPairId, List<Tag> tags) {
    return CreateTagsRequest.builder()
            .resources(keyPairId)
            .tags(TagHelper.translateTagsFromSdk(tags))
            .build();
}

static CreateTagsRequest translateToAddTagsRequest(final ResourceModel model) {
  return CreateTagsRequest.builder()
          .resources(model.getKeyPairId())
          .tags(TagHelper.translateTagsFromSdk(model.getTags()))
          .build();
}

static DeleteTagsRequest translateToRemoveTagsRequest(final ResourceModel model) {
    return DeleteTagsRequest.builder()
            .resources(model.getKeyPairId())
            .tags(TagHelper.translateTagsFromSdk(model.getTags()))
            .build();
}

static ResourceModel translateFromDescribeResponse(final ResourceModel model,
            final DescribeKeyPairsResponse describeKeyPairsResponse) {
        KeyPairInfo first = describeKeyPairsResponse.keyPairs().get(0);

        ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
                .keyFingerprint(first.keyFingerprint())
                .keyName(first.keyFingerprint())
                .keyPairId(first.keyPairId());
                
        //  tag specs hasTags() returns true even with zero length tag list
        if (isNotEmpty(first.tags())) {
            builder.tags(TagHelper.translateTagsToSdk(first.tags()));
        }
        return builder.build();
    }

    static List<ResourceModel> translateFromListRequest(
            final DescribeKeyPairsResponse describeKeyPairsResponse) {
        return TagHelper.streamOfOrEmpty(describeKeyPairsResponse.keyPairs())
                .map(keyPairInfo -> ResourceModel.builder()
                        .keyPairId(keyPairInfo.keyPairId())
                        .keyFingerprint(keyPairInfo.keyFingerprint())
                        .keyName(keyPairInfo.keyName())
                        .build())
                .collect(Collectors.toList());
    }
}
