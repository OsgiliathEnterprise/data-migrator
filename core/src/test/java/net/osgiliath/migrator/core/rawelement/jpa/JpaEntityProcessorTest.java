package net.osgiliath.migrator.core.rawelement.jpa;

import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JpaEntityProcessorTest {

    private JpaEntityProcessor jpaEntityProcessor;

    @BeforeEach
    public void setup() {
        jpaEntityProcessor = new JpaEntityProcessor();
    }

    @Test
    void testGetEntityId() {
        TestEntity ent = new TestEntity();

        Optional<Object> ret = jpaEntityProcessor.getId(TestEntity.class, ent);
        assertThat(ret.get()).isEqualTo(1L);
    }


    class TestEntity {
        @Id
        public Long getId() {
            return 1L;
        }
    }
}
