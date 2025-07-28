package net.osgiliath.migrator.core.rawelement.jpa;

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

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import net.osgiliath.migrator.core.metamodel.impl.internal.jpa.model.JpaMetamodelVertex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@ExtendWith(MockitoExtension.class)
class JpaEntityProcessorTest {

    private JpaEntityProcessor jpaEntityProcessor;
    @Mock
    private PlatformTransactionManager txMgr;

    @BeforeEach
    void setup() {
        jpaEntityProcessor = new JpaEntityProcessor(txMgr);
    }

    @Test
    void testGetEntityId() {
        TestEntity ent = new TestEntity();
        JpaMetamodelVertex jpaMetamodelVertex = new JpaMetamodelVertex(null, TestEntity.class);
        Optional<Object> ret = jpaEntityProcessor.getId(jpaMetamodelVertex, ent);
        assertThat(ret).contains(1L);
    }

    @Test
    void testIsUniqueWhenClassIsAnnotated() {
        JpaMetamodelVertex jpaMetamodelVertex = new JpaMetamodelVertex(null, TestEntity.class);
        assertThat(jpaEntityProcessor.isUnique(jpaMetamodelVertex, "uniqueColumnThroughClassAnnotation")).isTrue();
    }

    @Test
    void testIsUniqueWhenFieldIsAnnotated() {
        JpaMetamodelVertex jpaMetamodelVertex = new JpaMetamodelVertex(null, TestEntity.class);
        assertThat(jpaEntityProcessor.isUnique(jpaMetamodelVertex, "uniqueColumnThroughFieldAnnotation")).isTrue();
    }

    @Test
    void testIsNotUnique() {
        JpaMetamodelVertex jpaMetamodelVertex = new JpaMetamodelVertex(null, TestEntity.class);
        assertThat(jpaEntityProcessor.isUnique(jpaMetamodelVertex, "nonUniqueColumnThroughFieldAnnotation")).isFalse();
    }

    @Table(name = "country"
            , uniqueConstraints = @UniqueConstraint(columnNames = "unique_column_through_class_annotation")
    )
    class TestEntity {
        private String uniqueColumnThroughClassAnnotation;
        private String uniqueColumnThroughFieldAnnotation;
        private String nonUniqueColumnThroughFieldAnnotation;

        @Id
        public Long getId() {
            return 1L;
        }

        @Column(name = "not_unique")
        public String getNonUniqueColumnThroughFieldAnnotation() {
            return nonUniqueColumnThroughFieldAnnotation;
        }

        public void setNonUniqueColumnThroughFieldAnnotation(String nonUniqueColumnThroughFieldAnnotation) {
            this.nonUniqueColumnThroughFieldAnnotation = nonUniqueColumnThroughFieldAnnotation;
        }


        @Column(name = "unique_column_through_class_annotation")
        public String getUniqueColumnThroughClassAnnotation() {
            return uniqueColumnThroughClassAnnotation;
        }

        public void setUniqueColumnThroughClassAnnotation(String uniqueColumnThroughClassAnnotation) {
            this.uniqueColumnThroughClassAnnotation = uniqueColumnThroughClassAnnotation;
        }

        @Column(name = "unique_column_through_field_annotation", unique = true)
        public String getUniqueColumnThroughFieldAnnotation() {
            return uniqueColumnThroughFieldAnnotation;
        }

        public void setUniqueColumnThroughFieldAnnotation(String uniqueColumnThroughFieldAnnotation) {
            this.uniqueColumnThroughFieldAnnotation = uniqueColumnThroughFieldAnnotation;
        }

    }
}
