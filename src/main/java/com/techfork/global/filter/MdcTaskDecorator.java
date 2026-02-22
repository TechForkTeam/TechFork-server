package com.techfork.global.filter;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 부모 스레드의 MDC 컨텍스트를 자식 스레드로 복사하는 TaskDecorator.
 * ThreadPoolTaskExecutor에 등록하여 @Async, AsyncItemProcessor 등에서 MDC가 전파되도록 한다.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}