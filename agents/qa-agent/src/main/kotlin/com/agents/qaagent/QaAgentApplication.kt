package com.agents.qaagent

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QaAgentApplication

fun main(args: Array<String>) {
    runApplication<QaAgentApplication>(*args)
}
