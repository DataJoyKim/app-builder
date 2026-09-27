package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.MessageProcessorRestController")
@RequestMapping("/{applicationId}/console/api/message-processor")
public class MessageProcessorRestController {
    @Autowired
    private MessageProcessorRepository repository;
    @Autowired
    private MessageProcessorService messageProcessorService;

    @GetMapping("")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @RequestParam(name = "processorName") String processorName) {
        Optional<MessageProcessor> results = repository.findByApplicationIdAndProcessorName(applicationId, processorName);
        if(results.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(results.get(), HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        Object idObj = params.get("id");

        MessageProcessor data;
        if(idObj == null) {
            data = MessageProcessor.builder()
                    .applicationId(applicationId)
                    .processorName((String) params.get("processorName"))
                    .displayName((String) params.get("displayName"))
                    .script((String) params.get("script"))
                    .build();
        }
        else {
            data = repository.findById(Long.valueOf((String) idObj))
                    .filter(owned -> applicationId.equals(owned.getApplicationId()))
                    .orElseThrow();

            data.update(
                (String) params.get("displayName"),
                (String) params.get("script")
            );
        }

        MessageProcessor results = repository.save(data);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        MessageProcessor query = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow();

        repository.deleteById(query.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{processorName}/execute")
    public ResponseEntity<?> execute(@PathVariable("applicationId") String applicationId, @PathVariable("processorName") String processorName,
            @RequestBody List<Map<String, Object>> params
    ) {
        MessageProcessorRequest request = MessageProcessorRequest.builder()
                .contents(params)
                .build();

        MessageProcessorResult results = messageProcessorService.execute(applicationId, processorName, request);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
