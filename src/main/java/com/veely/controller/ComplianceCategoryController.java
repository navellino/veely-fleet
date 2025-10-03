package com.veely.controller;

import com.veely.entity.ComplianceCategory;
import com.veely.service.ComplianceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/safety-categories")
@RequiredArgsConstructor
public class ComplianceCategoryController {
    private final ComplianceCategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "settings/compliance_categories/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("category", new ComplianceCategory());
        return "settings/compliance_categories/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("category") ComplianceCategory category) {
        ComplianceCategory saved = categoryService.create(category);
        return "redirect:/settings/safety-categories/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findByIdOrThrow(id));
        return "settings/compliance_categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("category") ComplianceCategory category) {
        categoryService.update(id, category);
        return "redirect:/settings/safety-categories/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/settings/safety-categories";
    }
}
