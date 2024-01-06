package net.osgiliath.migrator.core.db.inject;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VertexPersister {

    @PersistenceContext(unitName = "sink")
    private EntityManager entityManager;

    @Transactional(transactionManager = "sinkTransactionManager")
    public void persistVertex(Object entity) {
        entityManager.persist(entity);
    }
}
