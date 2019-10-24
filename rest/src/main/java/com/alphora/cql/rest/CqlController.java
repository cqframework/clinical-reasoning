package com.alphora.cql.rest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CqlController {
    @RequestMapping(path="/cql", method=RequestMethod.POST)
    public String cql(@RequestBody String cql) {
        return "hobos";
    }
}