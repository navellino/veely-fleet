package com.veely.controller;

import com.veely.entity.DocumentTypeEntity;
import com.veely.service.DocumentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", documentTypeService.findAll());
        return "settings/document_types/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("type", new DocumentTypeEntity());
        return "settings/document_types/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("type") DocumentTypeEntity type,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/document_types/form";
        }
        DocumentTypeEntity saved = documentTypeService.save(type);
        return "redirect:/settings/document-types/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("type", documentTypeService.findByIdOrThrow(id));
        return "settings/document_types/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("type") DocumentTypeEntity type,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/document_types/form";
        }
        type.setId(id);
        documentTypeService.save(type);
        return "redirect:/settings/document-types/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        documentTypeService.delete(id);
        return "redirect:/settings/document-types";
    }
}
