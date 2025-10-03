package com.veely.controller;

import com.veely.entity.AdminDocumentType;
import com.veely.service.AdminDocumentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/admin-document-types")
@RequiredArgsConstructor
public class AdminDocumentTypeController {

    private final AdminDocumentTypeService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", service.findAll());
        return "settings/admin_document_types/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("type", new AdminDocumentType());
        return "settings/admin_document_types/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("type") AdminDocumentType type,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/admin_document_types/form";
        }
        AdminDocumentType saved = service.save(type);
        return "redirect:/settings/admin-document-types/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("type", service.findByIdOrThrow(id));
        return "settings/admin_document_types/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("type") AdminDocumentType type,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/admin_document_types/form";
        }
        type.setId(id);
        service.save(type);
        return "redirect:/settings/admin-document-types/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/settings/admin-document-types";
    }
}
