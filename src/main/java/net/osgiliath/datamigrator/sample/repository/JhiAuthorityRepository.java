package net.osgiliath.datamigrator.sample.repository;

import net.osgiliath.datamigrator.sample.domain.JhiAuthority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

/**
* Generated by Spring Data Generator on 07/01/2024
*/
@Repository
public interface JhiAuthorityRepository extends JpaRepository<JhiAuthority, String>, JpaSpecificationExecutor<JhiAuthority>, QuerydslPredicateExecutor<JhiAuthority> {

}
