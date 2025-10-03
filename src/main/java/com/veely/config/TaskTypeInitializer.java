package com.veely.config;

import com.veely.entity.TaskType;
import com.veely.service.TaskTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskTypeInitializer implements CommandLineRunner {
    private final TaskTypeService service;

    @Override
    public void run(String... args) {
	create("ORDINARY_SERVICE", "Tagliando", true, true, 12, 20000, true);
        create("REVISION", "Revisione", true, false, 24, null, true);
        create("EXTRA_SERVICE", "Manutenzione Straordinaria", true, true, null, null, false);
        create("TYRE_CHANGE_SUMMER", "Cambio gomme estive", true, false, 6, null, true);
        create("TYRE_CHANGE_WINTER", "Cambio gomme invernali", true, false, 6, null, true);
        create("OTHER", "Altro", true, false, null, null, false);
    }

    private void create(String code, String desc, boolean byDate, boolean byKm,
	    Integer months, Integer km, boolean auto) {
        if (service.findByCode(code) == null) {
            service.save(TaskType.builder()
                    .code(code)
                    .description(desc)
                    .byDate(byDate)
                    .byMileage(byKm)
                    .monthsInterval(months)
                    .kmInterval(km)
                    .auto(auto)
                    .build());
        }
    }
}
