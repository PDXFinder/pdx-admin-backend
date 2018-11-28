package org.pdxfinder.admin.controllers;

import org.pdxfinder.admin.pojos.MappingEntity;
import org.pdxfinder.admin.zooma.ZoomaEntity;
import org.pdxfinder.services.MappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;


/*
 * Created by csaba on 09/07/2018.
 */
@RestController
@RequestMapping("/api")
public class AjaxController {

    private MappingService mappingService;
    private final static Logger log = LoggerFactory.getLogger(AjaxController.class);

    private RestTemplate restTemplate = new RestTemplate();
    private final String ZOOMA_URL = "http://scrappy.ebi.ac.uk:8080/annotations";



    @Autowired
    public AjaxController(MappingService mappingService, RestTemplateBuilder restTemplateBuilder) {
        this.mappingService = mappingService;
        this.restTemplate = restTemplateBuilder.build();
    }



                        /****************************************************************
                         *          INTERACTIONS WITH PDX FINDER KNOWLEDGE BASE         *
                         ****************************************************************/


    @RequestMapping(value = "/missingmapping/diagnosis")
    @ResponseBody
    public Map<String, List<MappingEntity>> getMissingMappings(@RequestParam("ds") Optional<String> dataSource){

        String ds = null;
        if(dataSource.isPresent() && !dataSource.get().isEmpty()){
            ds = dataSource.get();
        }

        return mappingService.getMissingDiagnosisMappings(ds).getEntityList();
    }


    @RequestMapping(value = "/mapping/diagnosis")
    @ResponseBody
    public Map<String, List<MappingEntity>>  getDiagnosisMappings(@RequestParam("ds") Optional<String> dataSource){

        String ds = null;
        if(dataSource.isPresent() && !dataSource.get().isEmpty()){
            ds = dataSource.get();
        }

        return mappingService.getSavedDiagnosisMappings(ds).getEntityList();
    }


    @PostMapping("/diagnosis")
    public ResponseEntity<?> createDiagnosisMappings(@RequestBody List<MappingEntity> newMappings){

        log.info(newMappings.toString());
        return ResponseEntity.noContent().build();
    }






                        /****************************************************************
                         *                   INTERACTIONS WITH ZOOMA                    *
                         ****************************************************************/


    @GetMapping("/zooma/transform")
    public List<ZoomaEntity> transformAnnotationForZooma(){

        List<ZoomaEntity> zoomaEntities = mappingService.transformMappingsForZooma();
        return zoomaEntities; //new ResponseEntity<>(result, HttpStatus.OK);
    }



    @GetMapping("/zooma")
    public ResponseEntity<?> writeAllAnnotationsToZooma(){

        List<ZoomaEntity> zoomaEntities = mappingService.transformMappingsForZooma();

        ZoomaEntity zoomaEntity = zoomaEntities.get(0);
        HttpEntity<String> entity = BuildHttpHeader();
        HttpEntity<Object> req = new HttpEntity<>(zoomaEntity, entity.getHeaders());

        ResponseEntity<ZoomaEntity> result =  null;

        try{
            result = restTemplate.postForObject(ZOOMA_URL, req, ResponseEntity.class);

        }catch (Exception e){

            return new ResponseEntity<>(
                    new HashMap(){{put("message", e.getCause().getMessage());}}, HttpStatus.NOT_FOUND
            );
        }

        return new ResponseEntity<>(result, HttpStatus.OK);

    }



    public HttpEntity<String> BuildHttpHeader(){

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        return  entity;
    }



}
