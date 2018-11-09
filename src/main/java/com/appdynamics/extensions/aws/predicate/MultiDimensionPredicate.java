package com.appdynamics.extensions.aws.predicate;

import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.Dimension;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link Predicate} to filter data from multiple AWS dimensions. This create a separate filter for each dimension and applies them
 * to the corresponding dimension values.
 *
 * @author Satish Muddam
 */
public class MultiDimensionPredicate implements Predicate<Metric> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MultiDimensionPredicate.class);


    private List<Dimension> dimensions;
    private Map<String, Predicate<CharSequence>> allPredicates = new HashMap<>();

    public MultiDimensionPredicate(List<Dimension> dimensions) {
        this.dimensions = dimensions;
        build();
    }

    private void build() {
        if (dimensions != null && !dimensions.isEmpty()) {
            Predicate<CharSequence> patternPredicate = null;

            for (Dimension dimension : dimensions) {
                String dimensionName = dimension.getName();
                Set<String> dimensionValues = dimension.getValues();

                if(dimensionValues.size() > 0) {

                        for (String pattern : dimensionValues) {
                            if (!pattern.equals("")) {

                                Predicate<CharSequence> charSequencePredicate = Predicates.containsPattern(pattern);
                                if (patternPredicate == null) {
                                    patternPredicate = charSequencePredicate;
                                } else {
                                    patternPredicate = Predicates.or(patternPredicate, charSequencePredicate);
                                }
                                allPredicates.put(dimensionName, patternPredicate);

                            }

                            else {
                                LOGGER.warn(" Dimension Value for Dimension {} is blank. Not collecting metrics that require " +
                                        "this dimension", dimension.getName());
                            }

//                            allPredicates.put(dimensionName, patternPredicate); - didnt work
                        }
//                        allPredicates.put(dimensionName, patternPredicate);

//                    allPredicates.put(dimensionName, patternPredicate);
                }
                LOGGER.warn("Empty Dimension Value for {}", dimension.getName());
            }
        } else {
            LOGGER.warn("dimensions in config.yml not configured, hence not monitoring anything");
        }
    }

    public boolean apply(Metric metric) {

        List<com.amazonaws.services.cloudwatch.model.Dimension> dimensions = metric.getDimensions();

        boolean result = false;
        for (com.amazonaws.services.cloudwatch.model.Dimension dimension : dimensions) {
            String name = dimension.getName();
            String value = dimension.getValue();
            Predicate<CharSequence> predicate = allPredicates.get(name);
            if (predicate != null) {
                result = predicate.apply(value);
            }

            if (predicate == null || !result) {
                return false;
            }
        }
        return true;
    }
}