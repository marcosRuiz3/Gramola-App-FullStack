package edu.uclm.es.gramola.Dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.es.gramola.model.User;

@Repository
public interface UserDAO extends JpaRepository<User, Long> {
    
    // Método para buscar usuario por email y contraseña (encriptada)
    User findByEmailAndPwd(String email, String pwd);

    User findByEmail(String email);

    
    
    // Método para buscar usuario por el ID de su token de creación
    User findByCreationTokenId(String tokenId);
    
    User findByResetTokenId(String tokenId);
    // Método para buscar usuario por la cookie de sesión (Gramola-Cookie)
    User findByGramolaCookie(String gramolaCookie);

}