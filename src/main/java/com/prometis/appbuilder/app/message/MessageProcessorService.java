package com.prometis.appbuilder.app.message;

import com.prometis.appbuilder.app.executor.script.ScriptEngine;
import com.prometis.appbuilder.app.executor.script.ScriptEngineExecuteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessorService {
    private final MessageProcessorRepository messageProcessorRepository;
    private final ScriptEngine scriptEngine;

    public MessageProcessorResult execute(String processorName, MessageProcessorRequest params) {
        try {
            Optional<MessageProcessor> processorOptional = messageProcessorRepository.findByProcessorName(processorName);

            if (processorOptional.isEmpty()) {
                throw new RuntimeException("요청한 리소스가 존재하지않습니다. [code:" + processorName + "]");
            }

            MessageProcessor messageProcessor = processorOptional.get();

            Object result = scriptEngine.execute(messageProcessor.getScript(), params.getContents());

            return MessageProcessorResult.createSuccessMessage(result);
        } catch (ScriptEngineExecuteException e) {
            log.error("Script Execution Error", e);
            return MessageProcessorResult.createErrorMessage("E-CCD-001", e.getMessage());
        }
    }
}
