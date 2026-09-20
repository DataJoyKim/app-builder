package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.console.dto.WorkflowNodeResponse;
import com.prometis.appbuilder.message.MessageProcessor;
import com.prometis.appbuilder.message.MessageProcessorRepository;
import com.prometis.appbuilder.entity.Entity;
import com.prometis.appbuilder.entity.EntityRepository;
import com.prometis.appbuilder.file.FileHandler;
import com.prometis.appbuilder.file.FileHandlerRepository;
import com.prometis.appbuilder.node.WorkflowNode;
import com.prometis.appbuilder.node.WorkflowNodeRepository;
import com.prometis.appbuilder.node.code.FunctionType;
import com.prometis.appbuilder.notification.Notification;
import com.prometis.appbuilder.notification.NotificationRepository;
import com.prometis.appbuilder.query.Query;
import com.prometis.appbuilder.query.QueryRepository;
import com.prometis.appbuilder.restclient.RestClient;
import com.prometis.appbuilder.restclient.RestClientRepository;
import com.prometis.appbuilder.util.DataTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController("console.WorkflowNodeRestController")
@RequestMapping("/console/api/workflow-node")
public class WorkflowNodeRestController {
    private static final String CONDITION_DISPLAY_NAME = "조건분기";
    private static final String ERROR_MESSAGE_DISPLAY_NAME = "에러메시지";

    @Autowired
    private WorkflowNodeRepository repository;
    @Autowired
    private QueryRepository queryRepository;
    @Autowired
    private EntityRepository entityRepository;
    @Autowired
    private RestClientRepository restClientRepository;
    @Autowired
    private MessageProcessorRepository messageProcessorRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private FileHandlerRepository fileHandlerRepository;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody List<Map<String,Object>> params) {

        for(Map<String,Object> param : params) {
            WorkflowNode workflowNode;
            Object idObj = param.get("id");
            if(idObj == null) {
                workflowNode = createWorkflowNode(param);
            }
            else {
                workflowNode = repository.findById(DataTypeUtil.valueLongOf(idObj))
                        .orElseThrow(RuntimeException::new);

                updateWorkflowNode(param, workflowNode);
            }

            repository.save(workflowNode);
        }

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        WorkflowNode workflowNode = createWorkflowNode(params);

        return new ResponseEntity<>(repository.save(workflowNode), HttpStatus.OK);
    }

    private static WorkflowNode createWorkflowNode(Map<String, Object> params) {
        return WorkflowNode.builder()
                .workflowId(DataTypeUtil.valueLongOf(params.get("workflowId")))
                // 흐름이 nodeId 로만 이어지므로 비어있는 채로 저장되면 안 된다.
                .nodeId(resolveNodeId((String) params.get("nodeId")))
                .functionName((String) params.get("functionName"))
                .functionType(FunctionType.valueOf((String) params.get("functionType")))
                .orderNum((Integer) params.get("orderNum"))
                .isLogging((Boolean) params.get("isLogging"))
                .requestMessageId((String) params.get("requestMessageId"))
                .responseMessageId((String) params.get("responseMessageId"))
                .positionX(DataTypeUtil.valueIntegerOf(params.get("positionX")))
                .positionY(DataTypeUtil.valueIntegerOf(params.get("positionY")))
                .build();
    }

    private static void updateWorkflowNode(Map<String, Object> params, WorkflowNode workflowNode) {
        workflowNode.update(
                DataTypeUtil.valueLongOf(params.get("workflowId")),
                resolveNodeId((String) params.get("nodeId")),
                (String) params.get("functionName"),
                FunctionType.valueOf((String) params.get("functionType")),
                (Integer) params.get("orderNum"),
                (Boolean) params.get("isLogging"),
                (String) params.get("requestMessageId"),
                (String) params.get("responseMessageId"),
                DataTypeUtil.valueIntegerOf(params.get("positionX")),
                DataTypeUtil.valueIntegerOf(params.get("positionY"))
        );
    }

    private static String resolveNodeId(String nodeId) {
        return (nodeId == null || nodeId.isBlank()) ? "node-" + UUID.randomUUID() : nodeId;
    }

    @GetMapping("")
    public ResponseEntity<?> get(@RequestParam Map<String,Object> params) {
        Long workflowId = Long.valueOf((String) params.get("workflowId"));
        List<WorkflowNode> results = repository.findByWorkflowIdOrderByOrderNum(workflowId);

        List<WorkflowNodeResponse> response = new ArrayList<>();
        for(WorkflowNode w : results) {
            String displayName = createDisplayName(w);

            response.add(WorkflowNodeResponse.builder()
                    .id(w.getId())
                    .workflowId(w.getWorkflowId())
                    .nodeId(w.getNodeId())
                    .functionName(w.getFunctionName())
                    .displayName(displayName)
                    .functionType(w.getFunctionType())
                    .errorResolveType(w.getErrorResolveType())
                    .orderNum(w.getOrderNum())
                    .isLogging(w.getIsLogging())
                    .requestMessageId(w.getRequestMessageId())
                    .responseMessageId(w.getResponseMessageId())
                    .positionX(w.getPositionX())
                    .positionY(w.getPositionY())
                    .build());
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String createDisplayName(WorkflowNode w) {
        String displayName = "(기능생성필요)";
        if(FunctionType.SQL.equals(w.getFunctionType())) {
            Optional<Query> func = queryRepository.findByQueryName(w.getFunctionName());
            if(func.isPresent()){
                displayName = func.get().getDisplayName();
            }
        }
        else if(FunctionType.ENTITY.equals(w.getFunctionType())) {
            Optional<Entity> func = entityRepository.findByEntityName(w.getFunctionName());
            if(func.isPresent()){
                displayName = func.get().getDisplayName();
            }
        }
        else if(FunctionType.REST_CLIENT.equals(w.getFunctionType())) {
            Optional<RestClient> func = restClientRepository.findByClientName(w.getFunctionName());
            if(func.isPresent()){
                displayName = func.get().getDisplayName();
            }
        }
        else if(FunctionType.MESSAGE_PROCESSOR.equals(w.getFunctionType())) {
            Optional<MessageProcessor> func = messageProcessorRepository.findByProcessorName(w.getFunctionName());
            if(func.isPresent()){
                displayName = func.get().getDisplayName();
            }
        }
        else if(FunctionType.NOTIFICATION.equals(w.getFunctionType())) {
            Optional<Notification> func = notificationRepository.findByNotificationName(w.getFunctionName());
            if(func.isPresent()){
                displayName = func.get().getDisplayName();
            }
        }
        else if(FunctionType.FILE.equals(w.getFunctionType())) {
            Optional<FileHandler> func = fileHandlerRepository.findByHandlerName(w.getFunctionName());
            if(func.isPresent()){
                displayName = func.get().getDisplayName();
            }
        }
        else if(FunctionType.CONDITION.equals(w.getFunctionType())) {
            // 조건분기는 따로 등록해둔 기능이 없고 판정식 자체가 내용이라 고정 이름을 쓴다.
            displayName = CONDITION_DISPLAY_NAME;
        }
        else if(FunctionType.ERROR_MESSAGE.equals(w.getFunctionType())) {
            displayName = ERROR_MESSAGE_DISPLAY_NAME;
        }
        return displayName;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        WorkflowNode workflowNode = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        updateWorkflowNode(params, workflowNode);

        return new ResponseEntity<>(repository.save(workflowNode), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        WorkflowNode workflowNode = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        repository.deleteById(workflowNode.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
