package com.prometis.appbuilder.console.app.rest;

import com.prometis.appbuilder.app.view.MenuRepository;
import com.prometis.appbuilder.app.view.ViewObjectRepository;
import com.prometis.appbuilder.app.view.domain.Menu;
import com.prometis.appbuilder.app.view.domain.ViewObject;
import com.prometis.appbuilder.app.view.dto.MenuDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController("console.MenuRestController")
@RequestMapping("/{applicationId}/console/api/menu")
public class MenuRestController {
    @Autowired
    private MenuRepository repository;
    @Autowired
    private ViewObjectRepository viewObjectRepository;

    @GetMapping("/tree")
    public ResponseEntity<?> getList(@PathVariable("applicationId") String applicationId) {
        List<MenuDto> results = MenuDto.of(repository.findAllTree(applicationId), applicationId);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Menu results = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable("applicationId") String applicationId, @RequestBody Map<String,Object> params) {
        String parentMenuCd = (String) params.get("parentMenuCd");

        Optional<Menu> parentMenuOptional = repository.findByApplicationIdAndMenuCd(applicationId, parentMenuCd);
        Menu parentMenu = null;
        if(parentMenuOptional.isPresent()) {
            parentMenu = parentMenuOptional.get();
        }

        String objectCode = (String) params.get("objectCode");

        ViewObject viewObject = null;
        Optional<ViewObject> viewObjectOptional = viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
        if(viewObjectOptional.isPresent()) {
            viewObject = viewObjectOptional.get();
        }

        Menu createdData = Menu.builder()
                .applicationId(applicationId)
                .menuCd((String) params.get("menuCd"))
                .menuNm((String) params.get("menuNm"))
                .orderNum(Integer.valueOf((String) params.get("orderNum")))
                .icon((String) params.get("icon"))
                .parentMenu(parentMenu)
                .viewObject(viewObject)
                .build();

        repository.save(createdData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id, @RequestBody Map<String,Object> params) {
        String parentMenuCd = (String) params.get("parentMenuCd");

        Optional<Menu> parentMenuOptional = repository.findByApplicationIdAndMenuCd(applicationId, parentMenuCd);
        Menu parentMenu = null;
        if(parentMenuOptional.isPresent()) {
            parentMenu = parentMenuOptional.get();
        }

        String objectCode = (String) params.get("objectCode");

        ViewObject viewObject = null;
        Optional<ViewObject> viewObjectOptional = viewObjectRepository.findByApplicationIdAndObjectCode(applicationId, objectCode);
        if(viewObjectOptional.isPresent()) {
            viewObject = viewObjectOptional.get();
        }

        Menu savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        savedData.update(
                (String) params.get("menuCd"),
                (String) params.get("menuNm"),
                Integer.valueOf((String) params.get("orderNum")),
                (String) params.get("icon"),
                parentMenu,
                viewObject
        );

        repository.save(savedData);

        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("applicationId") String applicationId, @PathVariable("id") Long id) {
        Menu savedData = repository.findById(id)
                .filter(owned -> applicationId.equals(owned.getApplicationId()))
                .orElseThrow(RuntimeException::new);

        repository.deleteById(savedData.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
