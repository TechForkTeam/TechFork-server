package com.techfork.activity.readpost.infrastructure;

import java.time.LocalDateTime;

public interface FirstReadPostRepositoryCustom {

    boolean markFirstRead(Long userId, Long postId, LocalDateTime firstReadAt);
}
