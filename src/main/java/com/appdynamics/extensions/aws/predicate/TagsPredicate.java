package com.appdynamics.extensions.aws.predicate;

import com.amazonaws.services.resourcegroupstaggingapi.model.Tag;
import com.appdynamics.extensions.aws.config.Tags;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: {Vishaka Sekar} on {11/14/18}
 */
public class TagsPredicate implements com.google.common.base.Predicate<Tag> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TagsPredicate.class);
    private Map<String, Predicate<CharSequence>> allPredicates = new HashMap<>();
    private List<Tags> tags;

    public TagsPredicate(List<Tags> tags) {
        this.tags = tags;
        build();
    }

    private void build() {
        if (tags != null && !tags.isEmpty()) {
            Predicate<CharSequence> patternPredicate = null;

            for (Tags tag : tags) {
                String tagName = tag.getTagName();
                Set<String> tagValues = tag.getTagValue();

                if(tagValues.size() > 0) {

                    for (String pattern : tagValues) {
                        if (!pattern.equals("")) {

                            Predicate<CharSequence> charSequencePredicate = Predicates.containsPattern(pattern);
                            if (patternPredicate == null) {
                                patternPredicate = charSequencePredicate;
                            } else {
                                patternPredicate = Predicates.or(patternPredicate, charSequencePredicate);
                            }
                            allPredicates.put(tagName, patternPredicate);

                        }

                        else {
                            LOGGER.warn(" tag Value for tag {} is blank. Not collecting metrics that require " +
                                    "this tag");
                        }

//                            allPredicates.put(dimensionName, patternPredicate); - didnt work
                    }
//                        allPredicates.put(dimensionName, patternPredicate);

//                    allPredicates.put(dimensionName, patternPredicate);
                }
                LOGGER.warn("Empty tag Value for {}");
            }
        } else {
            LOGGER.warn("tags in config.yml not configured, hence not monitoring anything");
        }
    }




    @Override
    public boolean apply(Tag tag) {


                boolean result = false;
                String tagName = tag.getKey();
                String tagValue = tag.getValue();

                Predicate<CharSequence> predicate = allPredicates.get(tagName);
                if (predicate != null) {
                    result = predicate.apply(tagValue);
                }

                if (predicate == null || !result) {
                    return false;
                }




        return true;

    }




}
