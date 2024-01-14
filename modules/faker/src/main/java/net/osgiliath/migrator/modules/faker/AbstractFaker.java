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
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.transformers.JpaEntityColumnTransformer;
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

public abstract class AbstractFaker<COLUMN_TYPE> extends JpaEntityColumnTransformer<COLUMN_TYPE> {

    private final ColumnTransformationDefinition columnTransformationDefinition;

    public AbstractFaker(MetamodelVertex metamodel, ColumnTransformationDefinition columnTransformationDefinition) {
        super(metamodel, columnTransformationDefinition.getColumnName());
        this.columnTransformationDefinition = columnTransformationDefinition;
    }

    protected String fake(String value) {
        return getRandomInteger(value)
                .orElseGet(() ->
                        getRandomLocalDate(value)
                                .orElseGet(() ->
                                        getRandomString()));
    }

    private String getRandomString() {
        Faker faker = new Faker().getFaker();
        if (columnTransformationDefinition.getOptions().containsKey("faker")) {
            String fakerAlg = columnTransformationDefinition.getOptions().get("faker");
            return faker.resolve(fakerAlg);
        }
        return new StringBuilder()
                .append(faker.dragonBall().character())
                .toString();
    }

    private Optional<String> getRandomInteger(String value) {
        if (value.length() > 0) {
            try {
                Integer inputAsInteger = Integer.parseInt(value); // i.e. 10
                Random random = new Random();
                Integer randomizerFactor = (inputAsInteger * 2) * 10; // i.e. 1110
                Integer randomResult = random.ints(randomizerFactor - inputAsInteger, randomizerFactor * inputAsInteger)
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
                // go to next step
            }
        }
        return Optional.empty();
    }

    private Optional<String> getRandomLocalDate(String value) {
        for (SupportedLocalDateFormat supportedLocalDateFormat : SupportedLocalDateFormat.values()) {
            try {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern(supportedLocalDateFormat.format));
                Random random = new Random();
                Integer bound = 200;
                Integer randomResult = random.ints(100, bound)
                        .findFirst()
                        .getAsInt();
                return Optional.of(date.minus(randomResult, ChronoUnit.DAYS).format(DateTimeFormatter.ofPattern(supportedLocalDateFormat.format)));
            } catch (DateTimeParseException dtpe) {

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
