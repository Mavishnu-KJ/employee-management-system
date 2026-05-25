package com.example.EMS.service.impl;

import com.example.EMS.exceptions.DuplicateEmailException;
import com.example.EMS.exceptions.ResourceNotFoundException;
import com.example.EMS.model.dto.EmployeeRequestDto;
import com.example.EMS.model.dto.EmployeeResponseDto;
import com.example.EMS.model.entity.Employee;
import com.example.EMS.repository.EmployeeRepository;
import com.example.EMS.service.EmployeeService;
import jakarta.transaction.Transactional;
import org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private EmployeeRepository employeeRepository;
    private ModelMapper modelMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, ModelMapper modelMapper){
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public EmployeeResponseDto addEmployee(EmployeeRequestDto employeeRequestDto){
        logger.info("addEmployee, employeeRequestDto is {}", employeeRequestDto);

        //Throw DuplicateEmailException if validation fails
        if(employeeRequestDto != null && !employeeRequestDto.getEmail().isBlank() && employeeRepository.existsByEmail(employeeRequestDto.getEmail())){
            throw new DuplicateEmailException(employeeRequestDto.getEmail());
        }

        //Map EmployeeRequestDto to Employee
        Employee employee = modelMapper.map(employeeRequestDto, Employee.class);
        logger.info("addEmployee, employee is {}", employee);

        Employee employeeSaved = employeeRepository.save(employee);
        logger.info("addEmployee, employeeSaved is {}", employeeSaved);

        //Map Employee to EmployeeResponseDto
        EmployeeResponseDto employeeResponseDto = modelMapper.map(employeeSaved, EmployeeResponseDto.class);
        logger.info("addEmployee, employeeResponseDto is {}", employeeResponseDto);

        return employeeResponseDto;

    }

    @Override
    @Transactional
    public List<EmployeeResponseDto> addEmployees(List<EmployeeRequestDto> employeeRequestDtoList){
        logger.info("addEmployees, employeeRequestDtoList is {}", employeeRequestDtoList);

        if(employeeRequestDtoList.isEmpty()){
            throw new ResourceNotFoundException("Input request is empty");
        }

        //Map employee request dto to employee
        List<Employee> employeeList = new ArrayList<>();
        Set<String> seenEmailSet = new HashSet<>();
        ArrayList<String> duplicateEmailListInTheRequestDto =new ArrayList<>();
        ArrayList<String> duplicateEmailList =new ArrayList<>();

        for(EmployeeRequestDto employeeRequestDto : employeeRequestDtoList){
            if(employeeRequestDto != null && !employeeRequestDto.getEmail().isBlank()){
                //Ensure there is no duplicate email
                if(!seenEmailSet.add(employeeRequestDto.getEmail())){ // seenEmailSet.add(employeeRequestDto.email() returns true after addition, else false without adding, HashSet does not allow dupllicates
                    duplicateEmailListInTheRequestDto.add(employeeRequestDto.getEmail());
                }else if(employeeRepository.existsByEmail(employeeRequestDto.getEmail())){ //Validate email with db records
                    duplicateEmailList.add(employeeRequestDto.getEmail());
                }else if(duplicateEmailList.isEmpty()){ //Add only if there is no even one duplicate email
                    employeeList.add(modelMapper.map(employeeRequestDto, Employee.class));
                }
            }
        }
        logger.info("addEmployees, employeeList is {}", employeeList);

        //Throw duplicate email exception
        if(!duplicateEmailListInTheRequestDto.isEmpty()){
            logger.info("addEmployees, duplicateEmailListInTheRequestDto is {}", duplicateEmailListInTheRequestDto);
            throw new DuplicateEmailException("Duplicate email in the request itself", duplicateEmailListInTheRequestDto.toString());
        }

        if (!duplicateEmailList.isEmpty()) {
            logger.info("addEmployees, duplicateEmailList is {}", duplicateEmailList);
            throw new DuplicateEmailException(duplicateEmailList.toString());
        }

        //Save
        List<Employee> savedEmployeeList = employeeRepository.saveAll(employeeList);
        logger.info("addEmployees, savedEmployeeList is {}", savedEmployeeList);

        //Map employee to employee response dto
        List<EmployeeResponseDto> employeeResponseDtoList = new ArrayList<>();

        for(Employee emp : savedEmployeeList){
            employeeResponseDtoList.add(modelMapper.map(emp, EmployeeResponseDto.class));
        }
        logger.info("addEmployees, employeeResponseDtoList is {}", employeeResponseDtoList);

        return employeeResponseDtoList;

    }

    @Override
    @Transactional
    public List<EmployeeResponseDto> addEmployees1(List<EmployeeRequestDto> employeeRequestDtoList){
        logger.info("addEmployees1, employeeRequestDtoList is {}", employeeRequestDtoList);

        if(employeeRequestDtoList == null || employeeRequestDtoList.isEmpty()){
            throw new ResourceNotFoundException("Resource not found, employeeRequestDtoList : "+employeeRequestDtoList);
        }

        //Duplicate email check - start

        //1. Ensure there is no duplicate email in the request itself
        //NOTE : Bean validation annotations in the EmployeeRequestDto will take care of not null, not blank checks
        List<String> emailListInTheRequest = employeeRequestDtoList.stream()
                .map(EmployeeRequestDto::getEmail)
                .toList();
        logger.info("addEmployees1, emailListInTheRequest is {}", emailListInTheRequest);

        HashSet<String> seenEmailSet = new HashSet<>(); //HashSet does not allow duplicate keys, So preferred HashSet
        ArrayList<String> duplicateEmailListInTheRequest = new ArrayList<>();

        emailListInTheRequest.forEach(email -> {
            if(!seenEmailSet.add(email)){ // add method returns true on successful addition, else false
                duplicateEmailListInTheRequest.add(email);
            }
        });
        logger.info("addEmployees1, duplicateEmailListInTheRequest is {}", duplicateEmailListInTheRequest);

        if(!duplicateEmailListInTheRequest.isEmpty()){
            throw new DuplicateEmailException("Email already exists in the request itself : "+duplicateEmailListInTheRequest);
        }

        //2. Ensure there is no duplicate email by comparing with the existing mails in the DB
        List<String> duplicateEmailList = employeeRepository.duplicateEmailList(emailListInTheRequest);
        logger.info("addEmployees1, duplicateEmailList is {}", duplicateEmailList);

        if(!duplicateEmailList.isEmpty()){
            throw new DuplicateEmailException("Email already exists in the DB : "+duplicateEmailList);
        }

        //Map employee request dto to employee
        List<Employee> employeeList = employeeRequestDtoList.stream()
                .map(empDto -> modelMapper.map(empDto, Employee.class))
                .toList();
        logger.info("addEmployees1, employeeList is {}", employeeList);

        //Save
        List<Employee> savedEmployeeList = employeeRepository.saveAll(employeeList);
        logger.info("addEmployees1, savedEmployeeList is {}", savedEmployeeList);

        //Map employee to employee response dto
        List<EmployeeResponseDto> employeeResponseDtoList = savedEmployeeList.stream()
                .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class))
                .toList();

        logger.info("addEmployees1, employeeResponseDtoList is {}", employeeResponseDtoList);

        return employeeResponseDtoList;
    }

    @Override
    public EmployeeResponseDto getEmployeeById(Long id){
        logger.info("getEmployeeById, id is {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Resource not found for the given id : "+id));
        logger.info("getEmployeeById, employee is {}", employee);

        //Map employee to employee response dto
        EmployeeResponseDto employeeResponseDto = modelMapper.map(employee, EmployeeResponseDto.class);
        logger.info("getEmployeeById, employeeResponseDto is {}", employeeResponseDto);

        return employeeResponseDto;

    }

    @Override
    public List<EmployeeResponseDto> getAllEmployees(){
        logger.info("getAllEmployees");

        List<Employee> employeeList = employeeRepository.findAll();
        logger.info("getAllEmployees, employeeList is {}", employeeList);

        //Map employee to employee response dto
        List<EmployeeResponseDto> employeeResponseDtoList = employeeList.stream()
                        .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class))
                        .toList();

        logger.info("getAllEmployees, employeeResponseDtoList is {}", employeeResponseDtoList);

        return employeeResponseDtoList;

    }

    @Override
    public EmployeeResponseDto searchEmployeeById(Long id){
        logger.info("searchEmployeeById, id is {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found for the given id : "+id));
        logger.info("searchEmployeeById, employee is {}", employee);

        //Map employee to employee response dto
        EmployeeResponseDto employeeResponseDto = modelMapper.map(employee, EmployeeResponseDto.class);
        logger.info("searchEmployeeById, employeeResponseDto is {}", employeeResponseDto);

        return employeeResponseDto;

    }

    @Override
    public List<EmployeeResponseDto> searchEmployees(String name, Integer salary, String department, String email){
        logger.info("searchEmployees, name is {}, salary is {}, department is {}, email is {}", name, salary, department, email);

        //Create employee list stream
        Stream<Employee> employeeListStream = employeeRepository.findAll().stream();

        if(name !=null && !name.isBlank()){
            logger.info("searchEmployees, inside name !=null, name is {}", name);
            employeeListStream = employeeListStream.filter(emp -> emp.getName().toLowerCase().contains(name.trim().toLowerCase()));
        }

        //minSalary
        if(salary !=null){
            logger.info("searchEmployees, inside salary !=null, salary is {}", salary);
            employeeListStream = employeeListStream.filter(emp -> emp.getSalary() >= salary);
        }

        if(department !=null && !department.isBlank()){
            logger.info("searchEmployees, inside department !=null, department is {}", department);
            employeeListStream = employeeListStream.filter(emp -> emp.getDepartment().equalsIgnoreCase(department));
        }

        if(email !=null && !email.isBlank()){
            logger.info("searchEmployees, inside email !=null, email is {}", email);
            employeeListStream = employeeListStream.filter(emp -> emp.getEmail().equalsIgnoreCase(email));
        }

        List<Employee> employeeList = employeeListStream.collect(Collectors.toList());
        logger.info("searchEmployees, employeeList is {}", employeeList);

        //Map employee to employee response dto
        List<EmployeeResponseDto> employeeResponseDtoList = employeeList.stream()
                .filter(Objects::nonNull)
                .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class))
                .toList();
        logger.info("searchEmployees, employeeResponseDtoList is {}", employeeResponseDtoList);

        return employeeResponseDtoList;
    }

    @Override
    @Transactional
    public EmployeeResponseDto updateEmployeeById(EmployeeRequestDto employeeRequestDto, Long id){
        logger.info("updateEmployeeById, employeeRequestDto is {}, id is {}", employeeRequestDto, id);

        //find the employee
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found for the given id : "+id));

        logger.info("updateEmployeeById, existingEmployee is {}", existingEmployee);

        //Update existingEmployee
        existingEmployee.setName(employeeRequestDto.getName());
        existingEmployee.setSalary(employeeRequestDto.getSalary());
        existingEmployee.setDepartment(employeeRequestDto.getDepartment());
        existingEmployee.setEmail(employeeRequestDto.getEmail());

        logger.info("updateEmployeeById, existingEmployee with updated values is {}", existingEmployee);

        //save
        Employee updatedEmployee = employeeRepository.save(existingEmployee);
        logger.info("updateEmployeeById, updatedEmployee is {}", updatedEmployee);

        //Map employee to employee response dto
        return modelMapper.map(updatedEmployee, EmployeeResponseDto.class);
    }

    @Override
    @Transactional
    public List<EmployeeResponseDto> updateEmployeeByName(EmployeeRequestDto employeeRequestDto, String name) {
        logger.info("updateEmployeeByName, employeeRequestDto is {}, name is {}", employeeRequestDto, name);

        List<Employee> exisingEmployeeList = employeeRepository.findByName(name); //exact match case sensitive, not contains
        logger.info("updateEmployeeByName, exisingEmployeeList is {}", exisingEmployeeList);

        if(exisingEmployeeList == null || exisingEmployeeList.isEmpty()){
            throw new ResourceNotFoundException("Resource not found for the name : "+name);
        }

        //Update exisingEmployeeList with given values
        List<Employee> updatedEmployeeList = new ArrayList<>();

        for (Employee existingEmployee : exisingEmployeeList) {

            existingEmployee.setName(employeeRequestDto.getName());
            existingEmployee.setSalary(employeeRequestDto.getSalary());
            existingEmployee.setDepartment(employeeRequestDto.getDepartment());
            existingEmployee.setEmail(employeeRequestDto.getEmail());

            updatedEmployeeList.add(existingEmployee);
        }
        logger.info("updateEmployeeByName, updatedEmployeeList updated is {}", updatedEmployeeList);

        //Save
        List<Employee> savedEmployeeList = employeeRepository.saveAll(updatedEmployeeList);
        logger.info("updateEmployeeByName, updatedEmployeeList updated is {}", updatedEmployeeList);

        //Map employee to employeeResponseDto
        List<EmployeeResponseDto> employeeResponseDtoList = savedEmployeeList.stream()
                .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class))
                .toList();

        return employeeResponseDtoList;
    }

    @Override
    @Transactional
    public void deleteEmployeeById(Long id){
        logger.info("deleteEmployeeById, id is {}", id);

        //find existing employee by id
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found for the given id : "+id));
        logger.info("deleteEmployeeById, existingEmployee is {}", existingEmployee);

        //delete
        employeeRepository.delete(existingEmployee); // delete returns void
        logger.info("deleteEmployeeById, deleted the record for employeeId : {}", id);
    }

    @Override
    @Transactional
    public void deleteAllEmployees(){
        logger.info("deleteAllEmployees");

        //delete all
        employeeRepository.deleteAll();
        logger.info("deleteAllEmployees, deleted all records");

    }

    @Override
    public Page<EmployeeResponseDto> getAllEmployeesWithPagination(Pageable pageable){
        logger.info("getAllEmployeesWithPagination, pageable is {}", pageable);

        //Fetch all employees with pagination
        Page<Employee> employeePage = employeeRepository.findAll(pageable);
        logger.info("getAllEmployeesWithPagination, employeePage is {}", employeePage);

        //Map employee page to employee response dto
        //Convert Page<Employee> → Page<EmployeeResponseDto> while keeping pagination info
        //Use map on the Page object itself, Spring Data provides a built-in method for this
        //Don't use stream(), toList()
        Page<EmployeeResponseDto> employeeResponseDtoPage = employeePage
                .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class)); //Same wil work with manual mapping as well

        logger.info("getAllEmployeesWithPagination, employeeResponseDtoPage is {}", employeeResponseDtoPage);

        return employeeResponseDtoPage;
    }

    @Override
    public Page<EmployeeResponseDto> searchEmployeesWithPagination(String name, Integer salary, String department, String email, Pageable pageable){
        logger.info("searchEmployeesWithPagination, pageable is {}", pageable);

        //The below code actually form a sql like Select * from Employee where 1=1
        Specification<Employee> specification = Specification.where(null); //deprecated, still using this for better understanding
        //logger.info("searchEmployeesWithPagination, specification is {}", specification);
        //Commented the above line, because that won't print any useful data

        //add specific conditions

        //name
        if(name != null && !name.isBlank()){
            logger.info("searchEmployeesWithPagination, name is {}", name);
            //and for AND, or for OR in the SQL
            specification = specification.and(
                    (root, query, cb) ->
                            cb.like(cb.lower(root.get("name")), "%"+name.toLowerCase()+"%")
                            //NOTE : We cannot use toLowerCase() with root. So we used cb.lower(root.get("name"))
                    );
        }

        //minSalary
        if(salary != null){
            logger.info("searchEmployeesWithPagination, salary is {}", salary);
            specification = specification.and(
                    (root, query, cb)->
                            cb.greaterThanOrEqualTo(root.get("salary"), salary)
            );
        }

        //department
        if(department != null && !department.isBlank()){
            logger.info("searchEmployeesWithPagination, department is {}", department);

            specification = specification.and(
                    (root, query, cb) ->
                            cb.equal(root.get("department"), department)
            );
        }

        //email
        if(email != null && !email.isBlank()){
            logger.info("searchEmployeesWithPagination, email is {}", email);

            specification = specification.and(
                    (root, query, cb) ->
                            cb.equal(cb.lower(root.get("email")), email.toLowerCase())
            );
        }

        //logger.info("searchEmployeesWithPagination, after adding conditions, specification is {}", specification);
        //Commented the above line, because that won't print any useful data

        //Fetch data from repository
        Page<Employee> employeePage = employeeRepository.findAll(specification, pageable);
        logger.info("searchEmployeesWithPagination, employeePage is {}", employeePage);

        //Map employee page to employee response dto page
        Page<EmployeeResponseDto> employeeResponseDtoPage = employeePage
                .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class));
        logger.info("searchEmployeesWithPagination, employeeResponseDtoPage is {}", employeeResponseDtoPage);

        return employeeResponseDtoPage;
    }

    @Override
    public Page<EmployeeResponseDto> searchEmployeesWithPagination1(String name, Integer salary, String department, String email, Pageable pageable){
        logger.info("searchEmployeesWithPagination1, pageable is {}", pageable);

        //Since, the Specification.where(null) is deprecated, we are using the below approach
        List<Specification<Employee>> specificationList = new ArrayList<>();

        //add specific conditions
        if(name != null && !name.isBlank()){
            logger.info("searchEmployeesWithPagination1, name is {}", name);
            //and for AND, or for OR in the SQL
            specificationList.add(
                    (root, query, cb) ->
                            cb.like(cb.lower(root.get("name")), "%"+name.toLowerCase()+"%")
                    //NOTE : We cannot use toLowerCase() with root. So we used cb.lower(root.get("name"))
            );
        }

        //minSalary
        if(salary != null){
            logger.info("searchEmployeesWithPagination1, salary is {}", salary);
            specificationList.add(
                    (root, query, cb)->
                            cb.greaterThanOrEqualTo(root.get("salary"), salary)
            );
        }

        //department
        if(department != null && !department.isBlank()){
            logger.info("searchEmployeesWithPagination1, department is {}", department);

            specificationList.add(
                    (root, query, cb) ->
                            cb.equal(root.get("department"), department)
            );
        }

        //email
        if(email != null && !email.isBlank()){
            logger.info("searchEmployeesWithPagination1, email is {}", email);

            specificationList.add(
                    (root, query, cb) ->
                            cb.equal(cb.lower(root.get("email")), email.toLowerCase())
            );
        }

        //Combine the specification conditions
        //Specification.allOf(specificationList) for AND
        //specificationList.anyOf(specificationList) for OR
        Specification<Employee> specification = Specification.allOf(specificationList);

        //Fetch data from repository
        Page<Employee> employeePage = employeeRepository.findAll(specification, pageable);
        logger.info("searchEmployeesWithPagination1, employeePage is {}", employeePage);

        //Map employee page to employee response dto page
        Page<EmployeeResponseDto> employeeResponseDtoPage = employeePage
                .map(emp -> modelMapper.map(emp, EmployeeResponseDto.class));
        logger.info("searchEmployeesWithPagination1, employeeResponseDtoPage is {}", employeeResponseDtoPage);

        return employeeResponseDtoPage;
    }


}
