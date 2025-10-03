package com.veely.controller;

import com.veely.entity.LaborUnion;
import com.veely.service.LaborUnionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/unions")
@RequiredArgsConstructor
public class LaborUnionController {

    private final LaborUnionService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("unions", service.findAll());
        return "settings/unions/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("union", new LaborUnion());
        return "settings/unions/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("union") LaborUnion union) {
        LaborUnion saved = service.save(union);
        return "redirect:/settings/unions/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("union", service.findById(id));
        return "settings/unions/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("union") LaborUnion union) {
        service.update(id, union);
        return "redirect:/settings/unions/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/settings/unions";
    }
}
