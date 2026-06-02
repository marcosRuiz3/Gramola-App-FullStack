package edu.uclm.es.gramola; // Asegúrate de que esta línea coincide con tu paquete

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys; // 🔴 Importante para pulsar ENTER
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.uclm.es.gramola.Dao.QueueTrackDao;
import edu.uclm.es.gramola.model.QueueTrack;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class PagoCancionSeleniumTest {

    @Autowired
    private QueueTrackDao queueTrackDao;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15)); 
    }

    @Test
    public void testClienteBuscaPagaYPoneCancion() throws InterruptedException {
        // Guardamos estado inicial de la BD
        long cancionesAntes = queueTrackDao.count();

        // ---------------------------------------------------------
        // FASE 0: LOGIN DEL DUEÑO Y VINCULACIÓN DE SPOTIFY
        // ---------------------------------------------------------
        driver.get("http://127.0.0.1:4200/login"); 

        // 1. Rellenamos credenciales del dueño del bar
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']")));
        emailInput.sendKeys("alevinmarcos@gmail.com"); // 🔴 CAMBIAR AQUÍ

        WebElement passInput = driver.findElement(By.cssSelector("input[type='password']"));
        passInput.sendKeys("MrbAlmagro2003"); // 🔴 CAMBIAR AQUÍ (la contraseña del propietario)

        WebElement btnLogin = driver.findElement(By.cssSelector("button[type='submit']"));
        btnLogin.click();

        // 2. Esperamos a entrar al Dashboard y le damos a Reconectar Spotify
        WebElement btnSpotify = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[contains(text(), 'Reconectar Spotify')]")));
        btnSpotify.click();

        // 3. Rellenamos el login oficial de Spotify
        wait.until(ExpectedConditions.urlContains("accounts.spotify.com"));

        try {
            WebElement spotUser = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-username")));
            spotUser.sendKeys("marcosrbplay4@gmail.com"); // 🔴 CAMBIAR AQUÍ

            WebElement spotPass = driver.findElement(By.id("login-password"));
            spotPass.sendKeys("MrbAlmagro2003@"); // 🔴 CAMBIAR AQUÍ

            WebElement spotBtn = driver.findElement(By.id("login-button"));
            spotBtn.click();

            // Si nos pide aceptar permisos
            WebElement btnAceptar = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[data-testid='auth-accept']")));
            btnAceptar.click();
            
        } catch (Exception e) {
            System.out.println("No pidió login de Spotify o aceptó automáticamente...");
        }

        // 4. Esperamos a que vuelva al Dashboard y damos 3s para que guarde el token en BD
        wait.until(ExpectedConditions.urlContains("dashboard")); 
        Thread.sleep(3000);

        // ---------------------------------------------------------
        // FASE 1: BÚSQUEDA EN LA VISTA DEL CLIENTE
        // ---------------------------------------------------------
        driver.get("http://127.0.0.1:4200/bar/1");

        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[placeholder*='escuchar']")));
        
        // Escribimos la canción y pulsamos ENTER simulando el teclado
        searchInput.sendKeys("Bohemian Rhapsody", Keys.ENTER);
        
        // Damos tiempo visual para que se carguen los resultados de Spotify
        Thread.sleep(2000);

        // ---------------------------------------------------------
        // FASE 2: SELECCIÓN Y REDIRECCIÓN A PAGO
        // ---------------------------------------------------------
        WebElement botonPagar = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector(".result-track button")));
        botonPagar.click(); // 🔴 ESTO TE FALTABA EN TU CÓDIGO

        wait.until(ExpectedConditions.urlContains("/payment"));

        // ---------------------------------------------------------
        // FASE 3: FORMULARIO DE STRIPE
        // ---------------------------------------------------------
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
            By.cssSelector("#card-element iframe")));

        WebElement cardNumber = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("cardnumber")));
        cardNumber.sendKeys("4242424242424242");

        WebElement expDate = driver.findElement(By.name("exp-date"));
        expDate.sendKeys("1230");

        WebElement cvc = driver.findElement(By.name("cvc"));
        cvc.sendKeys("123");

        // 🔴 AÑADE ESTAS DOS LÍNEAS PARA EL CÓDIGO POSTAL:
        WebElement postal = driver.findElement(By.name("postal"));
        postal.sendKeys("28001"); // Ponemos un código postal ficticio

        // Salimos del iframe de Stripe
        driver.switchTo().defaultContent();

        // ---------------------------------------------------------
        // FASE 4: CONFIRMAR PAGO
        // ---------------------------------------------------------
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("submit")));
        submitButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".result-message.alert-success")));
        wait.until(ExpectedConditions.urlContains("/bar/1"));

        Thread.sleep(1500); // Pequeño margen para que Java haga el INSERT en BD

        // ---------------------------------------------------------
        // FASE 5: VERIFICACIÓN EN LA BASE DE DATOS
        // ---------------------------------------------------------
        long cancionesDespues = queueTrackDao.count();
        assertTrue(cancionesDespues > cancionesAntes, "¡Fallo! No se guardó la nueva canción.");

        List<QueueTrack> todasLasCanciones = queueTrackDao.findAll();
        QueueTrack ultimaCancion = todasLasCanciones.get(todasLasCanciones.size() - 1);
        
        assertNotNull(ultimaCancion);
        assertTrue(ultimaCancion.isPaid(), "¡Fallo! La canción no se guardó como VIP (isPaid).");
        
        String estado = ultimaCancion.getStatus();
        assertTrue(estado.equals("PENDING") || estado.equals("PLAYING"), 
                  "¡Fallo! Estado incorrecto: " + estado);
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}