package com.veely.controller;

import com.veely.entity.Refuel;
import com.veely.exception.BusinessValidationException;
import com.veely.service.FuelCardService;
import com.veely.service.RefuelService;
import com.veely.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/fleet/refuels")
@RequiredArgsConstructor
public class RefuelController {

    private final RefuelService refuelService;
    private final VehicleService vehicleService;
    private final FuelCardService fuelCardService;

    @GetMapping
    public String list(@RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "cardId", required = false) Long cardId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "from", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate from,
            @RequestParam(value = "to", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate to,
            Model model) {
    	model.addAttribute("refuels", refuelService.search(vehicleId, cardId, year, from, to));
        model.addAttribute("refuel", new Refuel());
        addOptions(model);
        return "fleet/refuels/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("refuel", new Refuel());
        addOptions(model);
        return "fleet/refuels/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("refuel") Refuel refuel,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addOptions(model);
            return "fleet/refuels/form";
        }
        try {
            refuelService.create(refuel);
            return "redirect:/fleet/refuels";
        } catch (BusinessValidationException | IllegalArgumentException e) {
            String msg = e instanceof BusinessValidationException && !((BusinessValidationException) e).getErrors().isEmpty()
                    ? ((BusinessValidationException) e).getErrors().get(0)
                    : e.getMessage();
            binding.rejectValue("mileage", "invalidMileage", msg);
            addOptions(model);
            return "fleet/refuels/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Refuel r = refuelService.findByIdOrThrow(id);
        model.addAttribute("refuel", r);
        addOptions(model);
        return "fleet/refuels/form";
    }
/*
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("refuel") Refuel refuel,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addOptions(model);
            return "fleet/refuels/form";
        }
        refuelService.update(id, refuel);
        return "redirect:/fleet/refuels/" + id + "/edit";
    }
*/
    
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("refuel") Refuel refuel,
                         BindingResult binding, Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            addOptions(model);
            return "fleet/refuels/form";
        }
        
        try {
            refuelService.update(id, refuel);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rifornimento aggiornato con successo!");
            // FIX: Redirect alla lista invece che alla pagina di edit
            return "redirect:/fleet/refuels";
        } catch (Exception e) {
            model.addAttribute("errorMessage", 
                "Errore durante l'aggiornamento del rifornimento: " + e.getMessage());
            addOptions(model);
            return "fleet/refuels/form";
        }
    }
    
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        refuelService.delete(id);
        return "redirect:/fleet/refuels";
    }

    private void addOptions(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        model.addAttribute("fuelCards", fuelCardService.findAll());
    }
}
