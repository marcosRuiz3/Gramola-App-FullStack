package edu.uclm.es.gramola;

import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.uclm.es.gramola.Dao.UserDAO; 
import edu.uclm.es.gramola.model.User;

@SpringBootTest
public class GeneradorUsuariosTest {

    
    @Autowired
    private UserDAO userDAO;

    @Test
    public void fabricarUsuariosYCSV() {
        System.out.println("⏳ Empezando a fabricar 1000 usuarios...");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter("credenciales.csv"))) {
            
            for (int i = 1; i <= 1000; i++) {
                User user = new User();
                String email = "user" + i + "@test.com";
                String rawPassword = "123456";
                
                // Rellenamos datos
                user.setEmail(email);
                user.setPassword(rawPassword); 
                user.setBar("Bar JMeter " + i);
                user.setPrecioCancion(1.00); 
                user.setPaid(true); 
                
                // Guardamos en BD
                userDAO.save(user);

                // Escribimos en el CSV
                writer.println(email + "," + rawPassword);
            }
            
            System.out.println("✅ ¡Misión Cumplida!");
            System.out.println("✅ 1000 usuarios inyectados en la base de datos.");
            
        } catch (Exception e) {
            System.err.println("❌ Hubo un error al crear los usuarios:");
            e.printStackTrace();
        }
    }
}