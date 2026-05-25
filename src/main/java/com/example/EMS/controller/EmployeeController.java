package com.example.EMS.controller;


import com.example.EMS.model.dto.EmployeeRequestDto;
import com.example.EMS.model.dto.EmployeeResponseDto;
import com.example.EMS.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    private EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService){
        this.employeeService = employeeService;
    }

    @PostMapping("/addEmployee")
    ResponseEntity<EmployeeResponseDto> addEmployee(@Valid @RequestBody EmployeeRequestDto employeeRequestDto){
        logger.info("addEmployee, employeeRequestDto is {}", employeeRequestDto);

        EmployeeResponseDto employeeAdded = employeeService.addEmployee(employeeRequestDto);
        logger.info("addEmployee, employeeAdded is {}", employeeAdded);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                        .replacePath("/api/employees/{id}")
                        .buildAndExpand(employeeAdded.getId())
                        .toUri();
        logger.info("addEmployee, location is {}", location);

        return ResponseEntity.created(location).body(employeeAdded);
    }

    @PostMapping("/addEmployees")
    ResponseEntity<List<EmployeeResponseDto>> addEmployees(@Valid @RequestBody List< @Valid EmployeeRequestDto> employeeRequestDtoList){
        logger.info("addEmployees, employeeRequestDtoList is {}", employeeRequestDtoList);

        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.addEmployees(employeeRequestDtoList);
        logger.info("addEmployees, employeeResponseDtoList is {}", employeeResponseDtoList);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        return ResponseEntity.created(location).body(employeeResponseDtoList);
    }

    //This endpoint functionality is same of /addEmployees only the difference is duplicate email check
    //This endpoint is mainly to demonstrate the @Query annotated repository method
    @PostMapping("/addEmployees1")
    ResponseEntity<List<EmployeeResponseDto>> addEmployees1(@Valid @RequestBody List< @Valid EmployeeRequestDto> employeeRequestDtoList){
        logger.info("addEmployees1, employeeRequestDtoList is {}", employeeRequestDtoList);

        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.addEmployees1(employeeRequestDtoList);
        logger.info("addEmployees1, employeeResponseDtoList is {}", employeeResponseDtoList);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();
        logger.info("addEmployees1, location is {}", location);

        return ResponseEntity.created(location).body(employeeResponseDtoList);
    }


    @GetMapping("/{id}") //http://localhost:8080/api/employees/12
    ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable("id") Long id){
        logger.info("getEmployeeById, id is {}", id);

        EmployeeResponseDto employeeResponseDto = employeeService.getEmployeeById(id);
        logger.info("getEmployeeById, employeeResponseDto is {}", employeeResponseDto);

        return ResponseEntity.ok(employeeResponseDto);

    }

    @GetMapping
    ResponseEntity<List<EmployeeResponseDto>> getAllEmployees(){
        logger.info("getAllEmployees");

        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.getAllEmployees();
        logger.info("getAllEmployees, employeeResponseDtoList is {}", employeeResponseDtoList);

        return ResponseEntity.ok(employeeResponseDtoList);
    }

    @GetMapping("/searchEmployeeById") //http://localhost:8080/api/employees/searchEmployeeById?id={id}
    ResponseEntity<EmployeeResponseDto> searchEmployeeById(@RequestParam("id") Long id){
        logger.info("searchEmployeeById, id is {}", id);

        EmployeeResponseDto employeeResponseDto = employeeService.searchEmployeeById(id);
        logger.info("searchEmployeeById, employeeResponseDto is {}", employeeResponseDto);

        return ResponseEntity.ok(employeeResponseDto);

    }

    @GetMapping("/searchEmployees")
    ResponseEntity<List<EmployeeResponseDto>> searchEmployees(
            @RequestParam (name = "name", required = false) String name,
            @Positive(message = "minSalary must be greater than 0") @RequestParam (name="minSalary", required = false) Integer salary,
            @RequestParam (name="department", required = false) String department,
            @Email(message = "email id must be in valid format") @RequestParam (name = "email", required = false) String email){

        logger.info("searchEmployees, name is {}, salary is {}, department is {}, email is {}", name, salary, department, email);

        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.searchEmployees(name, salary, department, email);
        logger.info("searchEmployees, employeeResponseDtoList is {}", employeeResponseDtoList);

        return ResponseEntity.ok(employeeResponseDtoList);

    }

    @PutMapping("/updateEmployeeById/{id}")
    ResponseEntity<EmployeeResponseDto> updateEmployeeById(@Valid @RequestBody EmployeeRequestDto employeeRequestDto, @PathVariable("id") Long id){
        logger.info("updateEmployeeById, employeeRequestDto is {}, id is {}", employeeRequestDto, id);

        EmployeeResponseDto updated = employeeService.updateEmployeeById(employeeRequestDto, id);
        logger.info("updateEmployeeById, updated is {}", updated);

        return ResponseEntity.ok(updated);
    }

    @PutMapping("/updateEmployeeByName/{name}")
    ResponseEntity<List<EmployeeResponseDto>> updateEmployeeByName(@Valid @RequestBody EmployeeRequestDto employeeRequestDto, @PathVariable("name") String name){
        logger.info("updateEmployeeByName, employeeRequestDto is {}, name is {}", employeeRequestDto, name);

        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.updateEmployeeByName(employeeRequestDto, name);
        logger.info("updateEmployeeByName, employeeResponseDtoList is {}", employeeResponseDtoList);

        if(employeeResponseDtoList == null || employeeResponseDtoList.isEmpty()){
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(employeeResponseDtoList);
    }

    @DeleteMapping("/deleteEmployeeById/{id}")
    ResponseEntity<HttpStatus> deleteEmployeeById(@PathVariable(name = "id") Long id){
        logger.info("deleteEmployeeById, id is {}", id);

        employeeService.deleteEmployeeById(id); //delete returns void

        return ResponseEntity.noContent().build(); //204 no content
    }

    @DeleteMapping("/deleteAllEmployees")
    ResponseEntity<HttpStatus> deleteAllEmployees(){
        logger.info("deleteAllEmployees");

        employeeService.deleteAllEmployees();

        return ResponseEntity.noContent().build(); //204 no content
    }

    //eg. url : http://localhost:8080/api/employees/getAllEmployeesWithPagination?page=0&size=5
    //eg. url : http://localhost:8080/api/employees/getAllEmployeesWithPagination
    //eg. url : http://localhost:8080/api/employees/getAllEmployeesWithPagination?page=1&size=5
    @GetMapping("/getAllEmployeesWithPagination")
    ResponseEntity<Page<EmployeeResponseDto>> getAllEmployeesWithPagination(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable){
        logger.info("getAllEmployeesWithPagination, pageable is {}", pageable);

        Page<EmployeeResponseDto> employeeResponseDtoPage = employeeService.getAllEmployeesWithPagination(pageable);
        logger.info("getAllEmployeesWithPagination, employeeResponseDtoPage is {}", employeeResponseDtoPage);

        return ResponseEntity.ok(employeeResponseDtoPage);
    }

    //eg. url : http://localhost:8080/api/employees/searchEmployeesWithPagination?page=0&name=sach&size=5
    //eg. url : http://localhost:8080/api/employees/searchEmployeesWithPagination
    //The below endpoint is to understand the concept of filter with pagination using Specification feature
    @GetMapping("/searchEmployeesWithPagination")
    ResponseEntity<Page<EmployeeResponseDto>> searchEmployeesWithPagination(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "minSalary", required = false) Integer salary,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "email", required = false) String email,
            Pageable pageable //default : page : 0, size : 20, sort : UNSORTED
    ){
        logger.info("searchEmployeesWithPagination, pageable is {}", pageable);

        Page<EmployeeResponseDto> employeeResponseDtoPage = employeeService.searchEmployeesWithPagination(name, salary, department, email, pageable);
        logger.info("searchEmployeesWithPagination, employeeResponseDtoPage is {}", employeeResponseDtoPage);

        return ResponseEntity.ok(employeeResponseDtoPage);
    }

    //The below endpoint is to demonstrate with latest Specification features anyOf, AllOf
    @GetMapping("/searchEmployeesWithPagination1")
    ResponseEntity<Page<EmployeeResponseDto>> searchEmployeesWithPagination1(
            @RequestParam(name = "name", required = false) String name,
            @Min(value = 0) @RequestParam(name = "minSalary", required = false) Integer salary,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "email", required = false) String email,
            Pageable pageable //default : page : 0, size : 20, sort : UNSORTED
    ){
        logger.info("searchEmployeesWithPagination1, pageable is {}", pageable);

        Page<EmployeeResponseDto> employeeResponseDtoPage = employeeService.searchEmployeesWithPagination1(name, salary, department, email, pageable);
        logger.info("searchEmployeesWithPagination1, employeeResponseDtoPage is {}", employeeResponseDtoPage);

        return ResponseEntity.ok(employeeResponseDtoPage);
    }


}
