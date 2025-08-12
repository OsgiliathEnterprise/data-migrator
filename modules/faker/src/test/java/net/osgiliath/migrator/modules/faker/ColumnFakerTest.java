package net.osgiliath.migrator.modules.faker;

/*-
 * #%L
 * datamigrator-modules-faker
 * %%
 * Copyright (C) 2024 - 2025 Osgiliath Inc.
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

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import net.datafaker.Faker;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import net.osgiliath.migrator.core.api.model.ModelElement;
import net.osgiliath.migrator.core.configuration.ColumnTransformationDefinition;
import net.osgiliath.migrator.core.graph.ModelElementProcessor;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import net.osgiliath.migrator.core.rawelement.RawElementProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColumnFakerTest {

    private ColumnFaker columnFaker;

    @Mock
    private ModelElementProcessor modelElementProcessor;
    @Mock
    private ColumnTransformationDefinition columnTransformationDefinition;
    @Mock
    private RawElementProcessor rawElementProcessor;

    private MetamodelVertex metamodelVertex;

    @BeforeEach
    void setup() {

        metamodelVertex = new JpaMetamodelVertex(null, TestEntity.class);

    }

    @Test
    void testFieldIsFaked() {
        when(columnTransformationDefinition.getColumnName()).thenReturn("columnToFake");
        columnFaker = new ColumnFaker(modelElementProcessor, metamodelVertex, columnTransformationDefinition, rawElementProcessor, new Faker().getFaker());
        String fieldValue = "originalValue";
        ModelElement modelElement = createModelElement(fieldValue, false);

        when(modelElementProcessor.getFieldRawValue(modelElement, "columnToFake")).thenReturn(fieldValue);
        doAnswer(invocation -> {
                    String newValue = invocation.getArgument(2);
                    ((TestEntity) ((ModelElement) invocation.getArgument(0)).rawElement()).setColumnToFake(newValue);
                    return null;
                }
        ).when(modelElementProcessor).setFieldRawValue(eq(modelElement), eq("columnToFake"), anyString());
        assertThat(((TestEntity) columnFaker.evaluate(modelElement).rawElement()).getColumnToFake()).isNotEqualTo(fieldValue);
    }

    @Test
    void testFieldIsFakedConsistently() {
        when(columnTransformationDefinition.getColumnName()).thenReturn("columnToFake");
        when(columnTransformationDefinition.getConsistentKey()).thenReturn(true);
        columnFaker = new ColumnFaker(modelElementProcessor, metamodelVertex, columnTransformationDefinition, rawElementProcessor, new Faker().getFaker());

        String fieldValue = "originalValue";
        ModelElement modelElement = createModelElement(fieldValue, false);
        ModelElement modelElement2 = createModelElement(fieldValue, false);

        Collection<String> fakedValue = new ArrayList<>();
        when(modelElementProcessor.getFieldRawValue(any(), eq("columnToFake"))).thenReturn(fieldValue);
        doAnswer(invocation -> {
                    String faked = invocation.getArgument(2);
                    fakedValue.add(faked);
                    ((TestEntity) ((ModelElement) invocation.getArgument(0)).rawElement()).setColumnToFake(faked);
                    return invocation;
                }
        ).when(modelElementProcessor).setFieldRawValue(any(), eq("columnToFake"), anyString());
        assertThat(((TestEntity) columnFaker.evaluate(modelElement).rawElement()).getColumnToFake()).isNotEqualTo(fieldValue);
        assertThat(((TestEntity) columnFaker.evaluate(modelElement2).rawElement()).getColumnToFake()).isEqualTo(fakedValue.iterator().next());
    }

    @Test
    void testFieldIsFakedUnique() {
        when(columnTransformationDefinition.getColumnName()).thenReturn("columnToFake");
        when(columnTransformationDefinition.getConsistentKey()).thenReturn(false);
        columnFaker = new ColumnFaker(modelElementProcessor, metamodelVertex, columnTransformationDefinition, rawElementProcessor, new Faker().getFaker());

        String fieldValue = "originalValue";
        List<ModelElement> modelElements = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            modelElements.add(createModelElement(fieldValue, true));
        }
        Collection<String> fakedValue = new ArrayList<>();
        when(modelElementProcessor.getFieldRawValue(any(), eq("columnToFake"))).thenReturn(fieldValue);
        when(rawElementProcessor.isUnique(any(), eq("columnToFake"))).thenReturn(true);
        doAnswer(invocation -> {
                    String faked = invocation.getArgument(2);
                    System.out.println(faked);
                    fakedValue.add(faked);
                    ((TestEntity) ((ModelElement) invocation.getArgument(0)).rawElement()).setUniqueColumnToFake(faked);
                    return invocation;
                }
        ).when(modelElementProcessor).setFieldRawValue(any(), eq("columnToFake"), anyString());
        for (int i = 0; i < 100; i++) {
            String ret = ((TestEntity) columnFaker.evaluate(modelElements.get(i)).rawElement()).getUniqueColumnToFake();
            assertThat(ret).isNotEqualTo(fieldValue);
            assertThat(fakedValue.stream().filter(faked -> faked.equals(ret)).count()).isEqualTo(1);
        }
    }

    private ModelElement createModelElement(String fieldValue, boolean unique) {
        TestEntity entity = new TestEntity();
        if (unique) {
            entity.setUniqueColumnToFake(fieldValue);
        } else {
            entity.setColumnToFake(fieldValue);
        }
        return new ModelElement(metamodelVertex, entity);
    }

    class TestEntity {
        private String columnToFake;
        private String uniqueColumnToFake;

        @Id
        public Long getId() {
            return 1L;
        }

        @Column(name = "column_to_fake")
        public String getColumnToFake() {
            return columnToFake;
        }

        public void setColumnToFake(String columnToFake) {
            this.columnToFake = columnToFake;
        }

        @Column(name = "unique_column_to_fake", unique = true)
        public String getUniqueColumnToFake() {
            return uniqueColumnToFake;
        }

        public void setUniqueColumnToFake(String uniqueColumnToFake) {
            this.uniqueColumnToFake = uniqueColumnToFake;
        }
    }
}
