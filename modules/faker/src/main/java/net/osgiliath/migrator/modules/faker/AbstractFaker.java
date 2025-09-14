package net.osgiliath.migrator.modules.faker;

/*-
 * #%L
 * faker
 * %%
 * Copyright (C) 2021 Osgiliath
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import net.datafaker.Faker;
import net.datafaker.providers.base.Text;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.transformers.ModelElementColumnTransformer;
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static net.datafaker.providers.base.Text.DIGITS;
import static net.datafaker.providers.base.Text.EN_UPPERCASE;


/**
 * Column faker superclass
 *
 * @param <T> the Type of the column
 */
public abstract class AbstractFaker<T> extends ModelElementColumnTransformer<T> {

    public static final String FAKER = "faker";
    private final ColumnTransformationDefinition columnTransformationDefinition;
    private final RawElementProcessor rawElementProcessor;
    private static final Logger log = LoggerFactory.getLogger(AbstractFaker.class);

    private static final Random RANDOM = new Random();
    private static Map<String, String> fakedKeys = new HashMap<>();
    private static final Collection<String> alreadyFakedValues = new ArrayList<>();
    private final Faker faker;

    protected AbstractFaker(ModelElementProcessor modelElementProcessor, MetamodelVertex metamodel, ColumnTransformationDefinition columnTransformationDefinition, RawElementProcessor rawElementProcessor, Faker faker) {
        super(modelElementProcessor, metamodel, columnTransformationDefinition.getColumnName());
        this.columnTransformationDefinition = columnTransformationDefinition;
        this.rawElementProcessor = rawElementProcessor;
        this.faker = faker;
    }

    protected String fake(String value) {
        return getCachedKey(value).orElseGet(() -> getRandomInteger(value)
                .orElseGet(() ->
                        getRandomLocalDate(value)
                                .orElseGet(() -> {
                                    String res = this.getRandomString();
                                    if (columnTransformationDefinition.getConsistentKey()) {
                                        fakedKeys.put(value, res);
                                    }
                                    return res;
                                })));
    }

    private Optional<String> getCachedKey(String originalValue) {
        if (columnTransformationDefinition.getConsistentKey() && fakedKeys.containsKey(originalValue)) {
            return Optional.of(fakedKeys.get(originalValue));
        }
        return Optional.empty();
    }

    private String getRandomString() {
        String fakerAlg = columnTransformationDefinition.getOptions().getOrDefault(FAKER, "dragon_ball.characters");
        if (rawElementProcessor.isUnique(getMetamodel(), columnTransformationDefinition.getColumnName())) {
            log.debug("Using unique faker algorithm: {}", fakerAlg);
            String uniqueFromPreferredFaker = null;
            try {
                uniqueFromPreferredFaker = faker.unique().fetchFromYaml(fakerAlg);
                return uniqueFromPreferredFaker;
            } catch (NoSuchElementException nsee) {
                log.warn("No unique value found for algorithm: {}. Will generate random values.", fakerAlg);
                String randomValue = faker.text().text(Text.TextSymbolsBuilder.builder()
                        .len(8)
                        .with(EN_UPPERCASE, 2)
                        .with(DIGITS, 3).build());
                if (!alreadyFakedValues.contains(randomValue)) {
                    alreadyFakedValues.add(randomValue);
                    return randomValue;
                } else {
                    log.warn("Random value {} already faked. Will generate another one.", randomValue);
                    return getRandomString(); // Recursion to find a unique value
                }
            }
        }
        return faker.resolve(fakerAlg);
    }

    private Optional<String> getRandomInteger(String value) {
        if (!value.isBlank()) {
            try {
                Integer inputAsInteger = Integer.parseInt(value); // i.e. 10
                Integer randomizerFactor = ((inputAsInteger + 1) * 2) * 10; // i.e. 1110
                Integer randomResult = RANDOM.ints(randomizerFactor - inputAsInteger, randomizerFactor * inputAsInteger)
                        .findFirst()
                        .getAsInt();
                if (inputAsInteger > 0) {
                    if (randomResult < 0) {
                        randomResult = -randomResult;
                    }
                } else {
                    if (randomResult > 0) {
                        randomResult = -randomResult;
                    }
                }
                return Optional.of(String.valueOf(randomResult));
            } catch (NumberFormatException nfe) {
                log.trace("Not a number while faking: {}", value);
            }
        }
        return Optional.empty();
    }

    private Optional<String> getRandomLocalDate(String value) {
        for (SupportedLocalDateFormat supportedLocalDateFormat : SupportedLocalDateFormat.values()) {
            try {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern(supportedLocalDateFormat.format));
                Integer bound = 200;
                Integer randomResult = RANDOM.ints(100, bound)
                        .findFirst()
                        .getAsInt();
                return Optional.of(date.minus(randomResult, ChronoUnit.DAYS).format(DateTimeFormatter.ofPattern(supportedLocalDateFormat.format)));
            } catch (DateTimeParseException dtpe) {
                log.trace("Not a date while faking: {}", value);
            }
        }
        return Optional.empty();
    }

    private enum SupportedLocalDateFormat {
        ISO("yyyy-MM-dd"),
        DDMMYYYSLASH("dd/MM/yyyy"),
        DDMMYYYDASH("dd-MM-yyyy"),
        FULLTEXT("E, MMM dd yyyy");

        private final String format;

        SupportedLocalDateFormat(String format) {
            this.format = format;
        }
    }
}
