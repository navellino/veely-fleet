package com.veely.controller;

import com.veely.entity.EmployeeRole;
import com.veely.service.EmployeeRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/settings/employee-roles")
@RequiredArgsConstructor
public class EmployeeRoleController {

    private final EmployeeRoleService roleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "settings/employee_roles/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("role", new EmployeeRole());
        return "settings/employee_roles/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("role") EmployeeRole role, BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/employee_roles/form";
        }
        EmployeeRole saved = roleService.create(role);
        return "redirect:/settings/employee-roles/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("role", roleService.findByIdOrThrow(id));
        return "settings/employee_roles/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("role") EmployeeRole role, BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/employee_roles/form";
        }
        roleService.update(id, role);
        return "redirect:/settings/employee-roles/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        roleService.delete(id);
        return "redirect:/settings/employee-roles";
    }
}
