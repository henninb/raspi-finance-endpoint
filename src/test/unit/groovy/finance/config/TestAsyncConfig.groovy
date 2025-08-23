package finance.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor

@Configuration
class TestAsyncConfig {

    @Bean(name = "taskExecutor")
    TaskExecutor taskExecutor() {
        new SyncTaskExecutor()
    }
}