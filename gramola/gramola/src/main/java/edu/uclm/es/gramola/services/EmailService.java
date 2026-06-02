package edu.uclm.es.gramola.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender; // Inyecta el servicio de correo de Spring

    /**
     * Envía un correo electrónico de confirmación al usuario recién registrado.
     * @param userEmail La dirección de correo del destinatario.
     * @param tokenId El token de confirmación generado para el usuario.
     */
    public void sendConfirmationEmail(String userEmail, String tokenId) {
        // Crea el mensaje de correo
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(userEmail);
        message.setSubject("¡Bienvenido a La Gramola! Confirma tu cuenta");

        // Construye el cuerpo del mensaje con el enlace de confirmación [cite: 881]
        String confirmationUrl = "http://127.0.0.1:8080/users/confirmToken/" + userEmail + "?token=" + tokenId;
        String emailBody = "Gracias por registrarte en La Gramola Virtual.\n\n"
                         + "Para activar tu cuenta, por favor, haz clic en el siguiente enlace:\n"
                         + confirmationUrl;
        
        message.setText(emailBody);

        // Envía el correo
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // En una aplicación real, aquí deberías manejar el error de forma más robusta
            // (por ejemplo, registrar el error en un log).
            System.err.println("Error al enviar el correo de confirmación: " + e.getMessage());
        }
    }

    public void sendRecoveryEmail(String email, String tokenId) {

        // Crea el mensaje de correo
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setTo(email);
        message.setSubject("¡Bienvenido a La Gramola! Confirma tu cuenta");

        String enlace = "http://127.0.0.1:4200/recoverypwd?token=" + tokenId;
        String body = "Has solicitado recuperar tu contraseña. Haz clic en el siguiente enlace para establecer una nueva:\n\n" + enlace;

        message.setText(body);

        // Envía el correo
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // En una aplicación real, aquí deberías manejar el error de forma más robusta
            // (por ejemplo, registrar el error en un log).
            System.err.println("Error al enviar el correo de confirmación: " + e.getMessage());
        }
        // Usa tu código existente para enviar el email aquí...
    }
}