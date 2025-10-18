package com.techfork.domain.source.repository;

import com.techfork.domain.source.entity.TechBlog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechBlogRepository extends JpaRepository<TechBlog, Long> {
}
