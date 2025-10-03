package com.veely.controller;

import com.veely.entity.FuelCard;
import com.veely.service.EmployeeService;
import com.veely.service.FuelCardService;
import com.veely.service.SupplierService;
import com.veely.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/fleet/fuel-cards")
@RequiredArgsConstructor
public class FuelCardController {

    private final FuelCardService fuelCardService;
    private final SupplierService supplierService;
    private final EmployeeService employeeService;
    private final VehicleService vehicleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("cards", fuelCardService.findAll());
        return "fleet/fuel_cards/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
    	FuelCard card = new FuelCard();
        model.addAttribute("card", card);
        addOptions(model, card);
        return "fleet/fuel_cards/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("card") FuelCard card,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
        	addOptions(model, card);
            return "fleet/fuel_cards/form";
        }
        FuelCard saved = fuelCardService.create(card);
        return "redirect:/fleet/fuel-cards/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        FuelCard card = fuelCardService.findByIdOrThrow(id);
        model.addAttribute("card", card);
        addOptions(model, card);
        return "fleet/fuel_cards/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("card") FuelCard card,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addOptions(model, card);
            return "fleet/fuel_cards/form";
        }
        fuelCardService.update(id, card);
        return "redirect:/fleet/fuel-cards/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        fuelCardService.delete(id);
        return "redirect:/fleet/fuel-cards";
    }
    
    @PostMapping("/{id}/flush")
    public String flush(@PathVariable Long id) {
        fuelCardService.flush(id);
        return "redirect:/fleet/fuel-cards";
    }

    private void addOptions(Model model, FuelCard current) {
        model.addAttribute("suppliers", supplierService.findAll());
        var employees = employeeService.findWithoutFuelCard();
        if (current.getEmployee() != null && employees.stream().noneMatch(e -> e.getId().equals(current.getEmployee().getId()))) {
            employees.add(current.getEmployee());
        }
        model.addAttribute("employees", employees);
        var vehicles = vehicleService.findWithoutFuelCard();
        if (current.getVehicle() != null && vehicles.stream().noneMatch(v -> v.getId().equals(current.getVehicle().getId()))) {
            vehicles.add(current.getVehicle());
        }
        model.addAttribute("vehicles", vehicles);
    }
}
