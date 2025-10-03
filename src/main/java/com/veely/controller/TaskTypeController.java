package com.veely.controller;

import com.veely.entity.TaskType;
import com.veely.service.TaskTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings/task-types")
@RequiredArgsConstructor
public class TaskTypeController {
    private final TaskTypeService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", service.findAll());
        return "settings/task_types/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("type", new TaskType());
        return "settings/task_types/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("type") TaskType type) {
        TaskType saved = service.save(type);
        return "redirect:/settings/task-types/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("type", service.findById(id));
        return "settings/task_types/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("type") TaskType type) {
        service.update(id, type);
        return "redirect:/settings/task-types/" + id + "/edit";
        
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/settings/task-types";
    }
}

