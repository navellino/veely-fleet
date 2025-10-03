package com.veely.controller;

import com.veely.entity.PublicAuthority;
import com.veely.service.PublicAuthorityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/public-authorities")
@RequiredArgsConstructor
public class PublicAuthorityController {

    private final PublicAuthorityService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("authorities", service.findAll());
        return "settings/public_authorities/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("authority", new PublicAuthority());
        return "settings/public_authorities/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("authority") PublicAuthority authority,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/public_authorities/form";
        }
        PublicAuthority saved = service.save(authority);
        return "redirect:/settings/public-authorities/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("authority", service.findByIdOrThrow(id));
        return "settings/public_authorities/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("authority") PublicAuthority authority,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "settings/public_authorities/form";
        }
        authority.setId(id);
        service.save(authority);
        return "redirect:/settings/public-authorities/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/settings/public-authorities";
    }
}
