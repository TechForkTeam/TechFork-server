package com.techfork.activity.readpost.infrastructure;

import com.techfork.activity.readpost.domain.FirstReadPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirstReadPostRepository extends JpaRepository<FirstReadPost, Long>, FirstReadPostRepositoryCustom {
}
