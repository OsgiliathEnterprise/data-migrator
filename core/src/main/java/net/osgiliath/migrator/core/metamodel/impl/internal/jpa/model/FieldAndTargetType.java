package net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model;

/*-
 * #%L
 * data-migrator-core
 * %%
 * Copyright (C) 2024 Osgiliath Inc.
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

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Entity field and target type.
 */
public class FieldAndTargetType {
    /**
     * Entity field.
     */
    private final Field field;
    /**
     * Target type of the field.
     */
    private final Type targetType;

    /**
     * Constructor.
     * @param field the field.
     * @param targetType the target type.
     */
    public FieldAndTargetType(Field field, Type targetType) {
        this.field = field;
        this.targetType = targetType;
    }

    /**
     * Gets the field.
     * @return the field.
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets the target type.
     * @return the target type.
     */
    public Type getTargetType() {
        return targetType;
    }
}
