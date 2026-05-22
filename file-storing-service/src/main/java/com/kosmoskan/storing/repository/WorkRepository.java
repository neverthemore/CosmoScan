package com.kosmoskan.storing.repository;

import com.kosmoskan.storing.model.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {

}