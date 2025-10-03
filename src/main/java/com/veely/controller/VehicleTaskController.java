package com.veely.controller;

import com.veely.entity.VehicleTask;
import com.veely.service.TaskTypeService;
import com.veely.service.VehicleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/fleet/tasks")
@RequiredArgsConstructor
public class VehicleTaskController {
    private final VehicleTaskService taskService;
    private final TaskTypeService typeService;

    @GetMapping("/vehicle/{vehicleId}/new")
    public String newForm(@PathVariable Long vehicleId, Model model) {
        VehicleTask task = new VehicleTask();
        task.setVehicle(new com.veely.entity.Vehicle());
        task.getVehicle().setId(vehicleId);
        model.addAttribute("task", task);
        model.addAttribute("types", typeService.findAll());
        return "fleet/tasks/form";
    }

    @PostMapping("/vehicle/{vehicleId}/new")
    public String create(@PathVariable Long vehicleId,
                         @ModelAttribute VehicleTask task) {
        taskService.create(vehicleId, task.getType().getId(),
                task.getDueDate(), task.getDueMileage());
        return "redirect:/fleet/vehicles/" + vehicleId + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        VehicleTask task = taskService.findById(id);
        model.addAttribute("task", task);
        model.addAttribute("types", typeService.findAll());
        return "fleet/tasks/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute VehicleTask task) {
        taskService.update(id, task.getType().getId(), task.getDueDate(), task.getDueMileage());
        return "redirect:/fleet/vehicles/" + task.getVehicle().getId() + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam Long vehicleId) {
        taskService.delete(id);
        return "redirect:/fleet/vehicles/" + vehicleId + "/edit";
    }
    
    @PostMapping("/vehicle/{vehicleId}/auto")
    public String updateAuto(@PathVariable Long vehicleId,
                             @RequestParam(value = "typeIds", required = false) java.util.List<Long> typeIds) {
        if (typeIds == null) typeIds = java.util.Collections.emptyList();
        taskService.updateAutoTasks(vehicleId, typeIds);
        return "redirect:/fleet/vehicles/" + vehicleId + "/edit#pane-tasks";
    }
}
