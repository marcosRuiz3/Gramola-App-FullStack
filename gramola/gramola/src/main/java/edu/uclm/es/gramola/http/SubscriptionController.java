package edu.uclm.es.gramola.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.es.gramola.Dao.SubscriptionPlanRepository;
import edu.uclm.es.gramola.model.SubscriptionPlan;

@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" }, allowCredentials = "true")
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionPlanRepository repo;

    @GetMapping("/plans")
    public List<SubscriptionPlan> getPlans() {
        return repo.findAll();
    }
}