package com.appdynamics.extensions.aws.predicate;

import com.amazonaws.services.resourcegroupstaggingapi.model.Tag;
import com.appdynamics.extensions.aws.config.Tags;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

/**
 * @author: {Vishaka Sekar} on {11/20/18}
 */

@RunWith(PowerMockRunner.class)
public class TagsPredicateTest {
    @Mock
    Tag awsMetricTag;

    @Test
    public void whenEmptyTagValuesThenReturnFalse() {
        List<Tags> tags = Lists.newArrayList();

        Tags tag = new Tags();
        tag.setTagName("testTag");
        Set<String> tagValues = Sets.newHashSet();
        tag.setTagValue(tagValues);
        tags.add(tag);

        TagsPredicate tagsPredicate = new TagsPredicate(tags);
        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("testValue");
        assertFalse(tagsPredicate.apply(awsMetricTag));
    }

    @Test
    public void whenEmptyStringTagValuesThenReturnFalse() {
        List<Tags> tags = Lists.newArrayList();

        Tags tag = new Tags();
        tag.setTagName("testTag");
        Set<String> tagValues = Sets.newHashSet();
        tagValues.add("");
        tag.setTagValue(tagValues);
        tags.add(tag);

        TagsPredicate tagsPredicate = new TagsPredicate(tags);
        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("testValue");
        assertFalse(tagsPredicate.apply(awsMetricTag));
    }

    @Test
    public void whenTagValueIsWildCardThenReturnTrue() {
        List<Tags> tags = Lists.newArrayList();
        Set<String> tagValues = Sets.newHashSet();
        tagValues.add(".*");
        Tags tag = new Tags();
        tag.setTagName("testTag");
        tag.setTagValue(tagValues);
        tags.add(tag);

        TagsPredicate tagsPredicate = new TagsPredicate(tags);
        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("testValue");
        assertTrue(tagsPredicate.apply(awsMetricTag));

        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("testValue1");
        assertTrue(tagsPredicate.apply(awsMetricTag));

        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("");
        assertTrue(tagsPredicate.apply(awsMetricTag));
    }

    @Test
    public void whenTagValueIsRegexThenReturnMatch() {
        List<Tags> tags = Lists.newArrayList();
        Set<String> tagValues = Sets.newHashSet();
        tagValues.add(".*est.*al");
        Tags tag = new Tags();
        tag.setTagName("testTag");
        tag.setTagValue(tagValues);
        tags.add(tag);

        TagsPredicate tagsPredicate = new TagsPredicate(tags);
        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("testValue");
        assertTrue(tagsPredicate.apply(awsMetricTag));

        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("testValue1");
        assertTrue(tagsPredicate.apply(awsMetricTag));

        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("AnotherTagWhichDoesNotMatch");
        assertFalse(tagsPredicate.apply(awsMetricTag));

        when(awsMetricTag.getKey()).thenReturn("testTag");
        when(awsMetricTag.getValue()).thenReturn("");
        assertFalse(tagsPredicate.apply(awsMetricTag));
    }

}
