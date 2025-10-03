package com.veely.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller per route di navigazione "amichevoli"
 */
@Controller
public class NavigationController {

    @GetMapping({"/vehicleList"})
    public String vehicleListRedirect() {
        return "redirect:/fleet/vehicles";
    }

    @GetMapping({"/employeeList"})
    public String employeeListRedirect() {
        return "redirect:/fleet/employees";
    }

    @GetMapping({"/employmentList"})
    public String employmentListRedirect() {
        return "redirect:/fleet/employments";
    }

    @GetMapping({"/assignmentList"})
    public String assignmentListRedirect() {
        return "redirect:/fleet/assignments";
    }
    
    @GetMapping({"/supplierList"})
    public String supplierListRedirect() {
        return "redirect:/fleet/suppliers";
    }
}
