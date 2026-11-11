import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lanzador de CyberGuard Enterprise (Java 21, un solo archivo).
 *
 *   java Ejecutar.java        -> arranca con "mvn spring-boot:run"
 *   java Ejecutar.java jar    -> genera el .jar y lo ejecuta
 *
 * Comprueba Java, Maven, el puerto 8080 y PostgreSQL, y abre el navegador cuando el servidor está listo.
 */
public class Ejecutar {
    private static final int PORT = 8080;
    private static final String URL = "http://localhost:" + PORT + "/login.html";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/cyberguard";

    public static void main(String[] args) throws Exception {
        boolean modoJar = false;
        boolean forzarLocal = false;
        for (String a : args) {
            if (a.equalsIgnoreCase("jar")) modoJar = true;
            if (a.equalsIgnoreCase("local")) forzarLocal = true;
        }
        System.out.println("=== CyberGuard Enterprise ===");

        if (!new File("pom.xml").exists()) {
            fail("Ejecute este programa desde la carpeta cyberguard-enterprise (donde está pom.xml).");
        }
        int java = Runtime.version().feature();
        if (java < 21) {
            fail("Se necesita Java 21 o superior y está usando Java " + java + ". Revise JAVA_HOME.");
        }
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        String mvn = obtenerMaven(windows);
        comprobarPuerto();
        boolean local = forzarLocal || !baseDeDatosDisponible();
        if (local) {
            System.out.println("Modo local: se usa una base de datos incorporada (carpeta data), sin instalar PostgreSQL.");
            System.out.println("Para usar PostgreSQL, inicie su servicio y ejecute de nuevo.");
        }
        mostrarCredenciales();
        System.out.println("Todo listo. Iniciando... (la primera vez Maven descarga dependencias y tarda unos minutos)");
        esperarYAbrirNavegador();

        if (modoJar) {
            System.out.println("Generando el ejecutable (mvn clean package)...");
            if (ejecutar(mvn, "clean", "package") != 0) {
                fail("La compilación o las pruebas fallaron. Revise los mensajes de Maven.");
            }
            String javaBin = new File(System.getProperty("java.home"), "bin" + File.separator + "java").getPath();
            System.exit(local
                ? ejecutar(javaBin, "-jar", "target/cyberguard-enterprise.jar", "--spring.profiles.active=local")
                : ejecutar(javaBin, "-jar", "target/cyberguard-enterprise.jar"));
        }
        System.exit(local
            ? ejecutar(mvn, "spring-boot:run", "-Dspring-boot.run.profiles=local")
            : ejecutar(mvn, "spring-boot:run"));
    }

    private static final String MAVEN_VERSION = "3.9.9";

    /** Usa el Maven instalado; si no hay, descarga una copia local en la carpeta .maven (sin instalar nada). */
    private static String obtenerMaven(boolean windows) throws Exception {
        String nombre = windows ? "mvn.cmd" : "mvn";
        if (funciona(nombre, "-version")) return nombre;

        Path destino = Path.of(".maven");
        Path bin = destino.resolve("apache-maven-" + MAVEN_VERSION).resolve("bin");
        Path mvn = bin.resolve(nombre);
        if (!Files.exists(mvn)) {
            System.out.println("Maven no está instalado. Descargando una copia local en .maven (solo la primera vez, unos 9 MB)...");
            descargarMaven(destino);
        }
        if (!Files.exists(mvn)) {
            fail("No se pudo preparar Maven en la carpeta .maven.");
        }
        try (var archivos = Files.list(bin)) {
            archivos.forEach(f -> f.toFile().setExecutable(true));
        }
        return mvn.toAbsolutePath().toString();
    }

    private static void descargarMaven(Path destino) throws Exception {
        String url = "https://archive.apache.org/dist/maven/maven-3/" + MAVEN_VERSION
            + "/binaries/apache-maven-" + MAVEN_VERSION + "-bin.zip";
        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        byte[] zip;
        String esperado;
        try {
            zip = descargar(client, url);
            esperado = new String(descargar(client, url + ".sha512")).trim().split("\\s+")[0];
        } catch (IOException e) {
            fail("No se pudo descargar Maven (" + e.getMessage() + ").\n"
                + "  Compruebe su conexión a internet, o instale Maven desde https://maven.apache.org/download.cgi");
            return;
        }
        String real = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(zip));
        if (!real.equalsIgnoreCase(esperado)) {
            fail("La descarga de Maven está corrupta (la suma de verificación no coincide). Vuelva a intentarlo.");
            return;
        }
        Files.createDirectories(destino);
        Path raiz = destino.toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                Path out = raiz.resolve(e.getName()).normalize();
                if (!out.startsWith(raiz)) throw new IOException("Entrada no válida en el zip: " + e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        System.out.println("Maven " + MAVEN_VERSION + " listo.");
    }

    private static byte[] descargar(HttpClient client, String url) throws IOException {
        try {
            HttpResponse<byte[]> r = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " en " + url);
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("descarga interrumpida");
        }
    }

    private static void mostrarCredenciales() {
        String user = System.getenv("CYBERGUARD_ADMIN_USER");
        String pass = System.getenv("CYBERGUARD_ADMIN_PASSWORD");
        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println(" Cuenta principal:  " + (user == null || user.isBlank() ? "admin" : user));
        System.out.println(" Contraseña:        " + (pass == null || pass.isBlank()
            ? "CambiaEsta#2026" : "la definida en CYBERGUARD_ADMIN_PASSWORD"));
        System.out.println(" (Si ya la cambió, use la nueva. También puede pulsar");
        System.out.println("  \"Crear cuenta\" en la pantalla de inicio de sesión.)");
        System.out.println("------------------------------------------------------------");
        System.out.println();
    }

    private static void comprobarPuerto() {
        try (ServerSocket s = new ServerSocket(PORT)) {
            // libre
        } catch (IOException e) {
            fail("El puerto " + PORT + " está ocupado. Cierre el programa que lo usa (¿otra copia de CyberGuard?).");
        }
    }

    private static boolean baseDeDatosDisponible() {
        String url = System.getenv("CYBERGUARD_DB_URL");
        if (url == null || url.isBlank()) url = DEFAULT_DB_URL;
        Matcher m = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/").matcher(url);
        if (!m.find()) return true; // URL no estándar: se deja que Spring lo valide
        String host = m.group(1);
        int port = m.group(2) == null ? 5432 : Integer.parseInt(m.group(2));
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (IOException e) {
            System.out.println("No se encontró PostgreSQL en " + host + ":" + port + ".");
            return false;
        }
    }

    private static boolean funciona(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static int ejecutar(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
        // Maven usa exactamente el mismo Java 21 que está ejecutando este lanzador
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
        Process p = pb.start();
        Thread hook = new Thread(() -> {
            p.descendants().forEach(ProcessHandle::destroy);
            p.destroy();
        });
        Runtime.getRuntime().addShutdownHook(hook);
        int code = p.waitFor();
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // la JVM ya se está cerrando
        }
        return code;
    }

    private static void esperarYAbrirNavegador() {
        Thread t = new Thread(() -> {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(2)).GET().build();
            for (int i = 0; i < 300; i++) {
                try {
                    var resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                    if (resp.statusCode() == 200) {
                        abrir();
                        return;
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (Exception ignored) {
                    // el servidor aún no responde
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void abrir() {
        System.out.println("\n>>> CyberGuard está listo: " + URL + "\n");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(URL));
            }
        } catch (Exception e) {
            System.out.println("Abra el navegador manualmente en " + URL);
        }
    }

    private static void fail(String msg) {
        System.err.println("\nERROR: " + msg);
        System.exit(1);
    }
}
