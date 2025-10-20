package com.techfork.domain.post.repository;

import com.techfork.domain.post.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByName(String name);

    List<Keyword> findAllByNameIn(List<String> names);
}
