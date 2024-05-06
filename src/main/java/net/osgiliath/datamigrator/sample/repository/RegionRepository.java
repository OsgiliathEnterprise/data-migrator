package net.osgiliath.datamigrator.sample.repository;

import net.osgiliath.datamigrator.sample.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

/**
* Generated by Spring Data Generator on 06/05/2024
*/
@Repository
public interface RegionRepository extends JpaRepository<Region, Long>, JpaSpecificationExecutor<Region>, QuerydslPredicateExecutor<Region> {

}
