package com.awssamples.ec2.importkeypair;
import com.google.common.collect.Lists;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TagHelper {

    static List<com.awssamples.ec2.importkeypair.Tag> translateTagsToSdk(List<Tag> tags) {
        return streamOfOrEmpty(tags)
                .map(tag -> com.awssamples.ec2.importkeypair.Tag.builder().key(tag.key()).value(tag.value())
                        .build())
                .collect(Collectors.toList());
    }

    static List<Tag> translateTagsFromSdk(final Collection<com.awssamples.ec2.importkeypair.Tag> tags) {
        return streamOfOrEmpty(tags)
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toList());
    }

    static List<Tag> convertResourceTagsToList(Map<String, String> resourceTags) {
        List<Tag> tags = Lists.newArrayList();
        if (resourceTags != null) {
            resourceTags.forEach((key, value) -> tags.add(Tag.builder().key(key).value(value).build()));
        }
        return tags;
    }

    static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
