package edu.uclm.es.gramola.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.es.gramola.Dao.UserDAO;
import edu.uclm.es.gramola.dao.TokenDAO;
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;

@Service
public class UserService {

    @Autowired
    private UserDAO userDao;

    @Autowired
    private TokenDAO tokenDao;

    @Autowired
    private EmailService emailService; 

    public void register(String bar, String email, String pwd, String clientId, String clientSecret) {
        //  Buscamos por email usando el método personalizado de UserDao
        User existingUser = this.userDao.findByEmail(email);
        
        if (existingUser != null) {
            // Escenario 409: El bar ya existe y ha pagado 
            if (existingUser.isPaid()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El bar ya existe y está activo");
            }
            // Escenarios alternativos: Si no confirmó o no pagó, se borra para reintentar
            this.userDao.delete(existingUser);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(pwd);
        user.setBar(bar);
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);
        
        Token token = new Token();
        user.setCreationToken(token);
        
        this.userDao.save(user);

        // Envío del correo con la URL que incluye el token 
        emailService.sendConfirmationEmail(user.getEmail(), token.getId());
    }

    public void confirmToken(String email, String token) {
        // Buscamos por email y controlamos si es nulo
        User user = this.userDao.findByEmail(email);
        
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario");
        }
        
        Token userToken = user.getCreationToken();
        
        if (!userToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Token incorrecto");
        }
        if (userToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El token ya ha sido utilizado");
        }
        
        userToken.use(); // Marca el token como confirmado 
        this.tokenDao.save(userToken);
    }
    
    public User login(String email, String pwd) {
        User tempUser = new User();
        String encryptedPwd = tempUser.encryptPassword(pwd);
        User user = this.userDao.findByEmailAndPwd(email, encryptedPwd);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credenciales incorrectas");
        }

        

        // Si tiene token y NO está usado, lanzamos error. Si es null, le dejamos pasar.
        if (user.getCreationToken() != null && !user.getCreationToken().isUsed()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Falta confirmar el email");
        }

        // El bar no puede entrar si no ha pagado la suscripción 
        if (!user.isPaid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Suscripción no pagada");
        }

        String nuevaCookie = UUID.randomUUID().toString();
        user.setGramolaCookie(nuevaCookie);
        this.userDao.save(user);

        return user;
    }

    public void requestPasswordReset(String email) {
        //  Buscamos por email
        User user = this.userDao.findByEmail(email);
        
        if (user != null) {
            // 1. Creamos un token nuevo específicamente para el reseteo
            Token resetToken = new Token();
            user.setResetToken(resetToken); 
            this.userDao.save(user); // Al guardar el usuario, el cascade guardará el token en la BD
            
            // 2. Enviamos el correo 
            this.emailService.sendRecoveryEmail(user.getEmail(), resetToken.getId());
        }
        // Si el usuario no existe, no hacemos nada (por seguridad, para que no adivinen emails)
    }

    public void confirmPasswordReset(String tokenId, String newPassword) {
        // 1. Buscamos al usuario que sea dueño de este token de reseteo
        User user = this.userDao.findByResetTokenId(tokenId);
        
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace es inválido o ha caducado");
        }
        
        // 2. Cambiamos la contraseña. Usamos setPassword igual que en el register
        user.setPassword(newPassword); 
        
        // 3. Destruimos/desvinculamos el token para que no se pueda reutilizar
        user.setResetToken(null);
        this.userDao.save(user);
        
        // Opcional: Si quieres borrar el token físicamente de la tabla Token:
        this.tokenDao.deleteById(tokenId);
    }

    public void delete(String email) {
        //  Primero buscamos al usuario por email, y si existe, lo borramos pasando el objeto entero
        User user = this.userDao.findByEmail(email);
        if (user != null) {
            this.userDao.delete(user);
        }
    }

    public void logout(String gramolaCookie) {
        User user = this.userDao.findByGramolaCookie(gramolaCookie);
        if (user != null) {
            user.setGramolaCookie(null); // Elimina la cookie para cerrar sesión
            this.userDao.save(user);
        }
    }

    public User findByGramolaCookie(String cookie) {
        return this.userDao.findByGramolaCookie(cookie);
    }

    public User findById(Long id) {
        return this.userDao.findById(id).orElse(null); 
    }
}