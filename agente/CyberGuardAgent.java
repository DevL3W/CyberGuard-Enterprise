import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Agente de CyberGuard Enterprise (Java 21, un solo archivo, sin dependencias).
 *
 * Uso:
 *   java CyberGuardAgent.java --server http://servidor:8080 --device-id 1 --key CLAVE
 *
 * Opciones:
 *   --interval 30        segundos entre señales (por defecto 30)
 *   --disk-threshold 10  porcentaje libre de disco bajo el cual se envía una alerta (por defecto 10)
 * También puede usar las variables CYBERGUARD_SERVER, CYBERGUARD_DEVICE_ID y CYBERGUARD_AGENT_KEY.
 *
 * Qué hace (y solo esto):
 *   - Se identifica con el ID del dispositivo y su clave (generada por un administrador en el panel).
 *   - Envía una señal periódica con el nombre del equipo, su IP y su sistema operativo.
 *   - Avisa cuando queda poco espacio libre en disco.
 * No lee archivos, no ejecuta comandos ni modifica nada en el equipo.
 */
public class CyberGuardAgent {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static String server;
    private static String deviceId;
    private static String key;
    private static boolean lowDiskReported = false;

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parse(args);
        server = first(opts.get("server"), System.getenv("CYBERGUARD_SERVER"));
        deviceId = first(opts.get("device-id"), System.getenv("CYBERGUARD_DEVICE_ID"));
        key = first(opts.get("key"), System.getenv("CYBERGUARD_AGENT_KEY"));
        int interval = number(opts.get("interval"), 30);
        int diskThreshold = number(opts.get("disk-threshold"), 10);

        if (server == null || deviceId == null || key == null) {
            System.err.println("Faltan datos. Uso: java CyberGuardAgent.java --server http://servidor:8080 --device-id 1 --key CLAVE");
            System.exit(2);
        }
        while (server.endsWith("/")) server = server.substring(0, server.length() - 1);
        if (server.startsWith("http://") && !server.contains("localhost") && !server.contains("127.0.0.1")) {
            log("AVISO: la conexión no usa HTTPS; la clave viajaría sin cifrar por la red. Use HTTPS en producción.");
        }

        log("Agente iniciado. Servidor: " + server + " · Dispositivo: " + deviceId + " · Cada " + interval + " s");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Agente detenido.")));

        while (true) {
            int status = post("/api/agent/heartbeat", heartbeatJson());
            if (status == 200) {
                log("Señal enviada.");
            } else if (status == 401) {
                System.err.println("El ID del dispositivo o la clave no son válidos. Genere una clave nueva en el panel.");
                System.exit(1);
            } else if (status > 0) {
                log("El servidor respondió con el código " + status + ".");
            }
            checkDisk(diskThreshold);
            Thread.sleep(interval * 1000L);
        }
    }

    private static void checkDisk(int thresholdPercent) {
        File root = new File(".").getAbsoluteFile().toPath().getRoot().toFile();
        long total = root.getTotalSpace();
        if (total <= 0) return;
        long freePercent = root.getUsableSpace() * 100 / total;
        if (freePercent < thresholdPercent && !lowDiskReported) {
            String body = "{\"title\":\"Poco espacio en disco\",\"severity\":\"ALTA\",\"description\":\""
                    + esc("Queda un " + freePercent + " % libre en " + root.getPath()) + "\"}";
            if (post("/api/agent/events", body) == 201) lowDiskReported = true;
        } else if (freePercent >= thresholdPercent + 5) {
            lowDiskReported = false;
        }
    }

    private static String heartbeatJson() {
        return "{\"hostname\":\"" + esc(hostname()) + "\",\"ipAddress\":\"" + esc(localIp())
                + "\",\"operatingSystem\":\"" + esc(System.getProperty("os.name") + " " + System.getProperty("os.version")) + "\"}";
    }

    /** Envía un POST y devuelve el código HTTP, o -1 si no se pudo conectar. */
    private static int post(String path, String json) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(server + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Device-Id", deviceId)
                    .header("X-Agent-Key", key)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return CLIENT.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } catch (Exception e) {
            log("No se pudo contactar con el servidor (" + e.getClass().getSimpleName() + "). Se reintentará.");
            return -1;
        }
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String env = System.getenv("COMPUTERNAME");
            return env != null ? env : "desconocido";
        }
    }

    private static String localIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof java.net.Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
            // sin IP local utilizable
        }
        return "";
    }

    private static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
                }
            }
        }
        return b.toString();
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].startsWith("--")) m.put(args[i].substring(2), args[++i]);
        }
        return m;
    }

    private static String first(String a, String b) { return a != null && !a.isBlank() ? a : (b != null && !b.isBlank() ? b : null); }

    private static int number(String s, int def) {
        try { return s == null ? def : Math.max(5, Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return def; }
    }

    private static void log(String msg) {
        System.out.println(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + msg);
    }
}
