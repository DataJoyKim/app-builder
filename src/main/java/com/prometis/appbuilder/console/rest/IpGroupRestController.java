package com.prometis.appbuilder.console.rest;

import com.prometis.appbuilder.app.security.ip.IpAddress;
import com.prometis.appbuilder.app.security.ip.IpAddressMatcher;
import com.prometis.appbuilder.app.security.ip.IpAddressRepository;
import com.prometis.appbuilder.app.security.ip.IpGroup;
import com.prometis.appbuilder.app.security.ip.IpGroupRepository;
import com.prometis.appbuilder.app.util.DataTypeUtil;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroup;
import com.prometis.appbuilder.app.workflow.WorkflowIpGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IP 그룹 관리. 그룹 안의 허용 IP 목록은 한 번에 통째로 저장한다. (화면에서 편집 후 저장 버튼 한 번)
 */
@RestController("console.IpGroupRestController")
@RequestMapping("/console/api/ip-group")
public class IpGroupRestController {
    @Autowired
    private IpGroupRepository repository;
    @Autowired
    private IpAddressRepository ipAddressRepository;
    @Autowired
    private WorkflowIpGroupRepository workflowIpGroupRepository;

    @GetMapping("")
    public ResponseEntity<?> getList() {
        List<IpGroup> ipGroups = repository.findAllByOrderByGroupCodeAsc();

        List<Map<String, Object>> results = new ArrayList<>();
        for(IpGroup ipGroup : ipGroups) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", ipGroup.getId());
            row.put("groupCode", ipGroup.getGroupCode());
            row.put("displayName", ipGroup.getDisplayName());
            row.put("note", ipGroup.getNote());
            // 목록에서 그룹별 IP 개수를 바로 보여주기 위해 함께 내린다.
            row.put("ipCount", ipAddressRepository.findByIpGroupIdOrderByOrderNumAsc(ipGroup.getId()).size());

            results.add(row);
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id) {
        IpGroup ipGroup = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        Map<String, Object> result = new HashMap<>();
        result.put("id", ipGroup.getId());
        result.put("groupCode", ipGroup.getGroupCode());
        result.put("displayName", ipGroup.getDisplayName());
        result.put("note", ipGroup.getNote());
        result.put("ipAddresses", ipAddressRepository.findByIpGroupIdOrderByOrderNumAsc(ipGroup.getId()));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String,Object> params) {
        String groupCode = text(params.get("groupCode"));

        if(repository.findByGroupCode(groupCode).isPresent()) {
            return error("이미 존재하는 IP그룹 코드입니다. [" + groupCode + "]");
        }

        IpGroup createdData = IpGroup.builder()
                .groupCode(groupCode)
                .displayName(text(params.get("displayName")))
                .note(text(params.get("note")))
                .build();

        return new ResponseEntity<>(repository.save(createdData), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        IpGroup savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        String groupCode = text(params.get("groupCode"));

        if(repository.findByGroupCode(groupCode).filter(other -> !other.getId().equals(id)).isPresent()) {
            return error("이미 존재하는 IP그룹 코드입니다. [" + groupCode + "]");
        }

        String beforeGroupCode = savedData.getGroupCode();

        savedData.update(
                groupCode,
                text(params.get("displayName")),
                text(params.get("note"))
        );

        repository.save(savedData);

        // 워크플로우 매핑은 그룹코드로 들고있으므로 코드가 바뀌면 매핑도 따라 바꿔준다.
        if(!beforeGroupCode.equals(groupCode)) {
            for(WorkflowIpGroup mapping : workflowIpGroupRepository.findByIpGroupCode(beforeGroupCode)) {
                mapping.update(groupCode, mapping.getWorkflow());
                workflowIpGroupRepository.save(mapping);
            }
        }

        return new ResponseEntity<>(savedData, HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        IpGroup savedData = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        // 워크플로우에 매핑된 그룹을 지우면 그 워크플로우의 IP 제한이 조용히 풀리므로 막는다.
        List<WorkflowIpGroup> mappings = workflowIpGroupRepository.findByIpGroupCode(savedData.getGroupCode());
        if(!mappings.isEmpty()) {
            String workflowCodes = mappings.stream()
                    .map(mapping -> mapping.getWorkflow() == null ? "" : mapping.getWorkflow().getWorkflowCode())
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            return error("워크플로우에 사용중인 IP그룹은 삭제할 수 없습니다. [" + workflowCodes + "]");
        }

        ipAddressRepository.deleteByIpGroupId(savedData.getId());
        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{id}/address")
    public ResponseEntity<?> getAddresses(@PathVariable("id") Long id) {
        return new ResponseEntity<>(ipAddressRepository.findByIpGroupIdOrderByOrderNumAsc(id), HttpStatus.OK);
    }

    /**
     * 그룹의 허용 IP 목록을 통째로 교체한다. 화면에서 행을 추가/수정/삭제한 결과를 그대로 보낸다.
     */
    @Transactional
    @PutMapping("/{id}/address")
    public ResponseEntity<?> saveAddresses(@PathVariable("id") Long id, @RequestBody List<Map<String,Object>> params) {
        IpGroup ipGroup = repository.findById(id)
                .orElseThrow(RuntimeException::new);

        List<Map<String,Object>> rows = (params == null) ? List.of() : params;

        for(Map<String,Object> param : rows) {
            String ipAddress = text(param.get("ipAddress"));

            if(!IpAddressMatcher.isValidPattern(ipAddress)) {
                return error("IP 형식이 올바르지않습니다. [" + ipAddress + "]");
            }
        }

        ipAddressRepository.deleteByIpGroupId(ipGroup.getId());

        int orderNum = 0;
        List<IpAddress> results = new ArrayList<>();
        for(Map<String,Object> param : rows) {
            IpAddress ipAddress = IpAddress.builder()
                    .ipGroupId(ipGroup.getId())
                    .ipAddress(text(param.get("ipAddress")))
                    .note(text(param.get("note")))
                    .enabled(param.get("enabled") == null || Boolean.TRUE.equals(param.get("enabled")))
                    .orderNum(DataTypeUtil.valueIntegerOf(param.get("orderNum")) == null ? orderNum : DataTypeUtil.valueIntegerOf(param.get("orderNum")))
                    .build();

            results.add(ipAddressRepository.save(ipAddress));

            orderNum++;
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    private static String text(Object value) {
        if(value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();

        return text.isEmpty() ? null : text;
    }

    private static ResponseEntity<?> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
