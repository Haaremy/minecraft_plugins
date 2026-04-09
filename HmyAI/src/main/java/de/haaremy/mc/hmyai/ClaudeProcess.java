package de.haaremy.mc.hmyai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Spawns the Claude CLI as a subprocess and reads the stream-json output.
 *
 * Claude is invoked with:
 *   claude --print "<prompt>" --dangerously-skip-permissions
 *          --output-format stream-json
 *          --mcp-config <path>
 *          [--resume <session-id>]
 *
 * The final JSON line with "type":"result" contains the response text
 * and the session_id for conversation continuity.
 */
public class ClaudeProcess {

    public record ClaudeResult(String text, String sessionId) {}

    private final HmyAI plugin;

    public ClaudeProcess(HmyAI plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs Claude synchronously (call from an async thread).
     *
     * @param prompt    Full prompt string (context + user message)
     * @param sessionId Existing session ID to resume, or null for new session
     * @return ClaudeResult with response text and new session ID
     * @throws Exception if the process fails or times out
     */
    public ClaudeResult run(String prompt, String sessionId) throws Exception {
        String claudeCmd = plugin.getConfig().getString("claude-command", "claude");
        int timeout = plugin.getConfig().getInt("timeout-seconds", 120);

        File mcpConfig = new File(plugin.getDataFolder(), "mcp-config.json");

        List<String> cmd = new ArrayList<>();
        cmd.add(claudeCmd);
        cmd.add("--print");
        cmd.add(prompt);
        cmd.add("--dangerously-skip-permissions");
        cmd.add("--output-format");
        cmd.add("stream-json");
        cmd.add("--mcp-config");
        cmd.add(mcpConfig.getAbsolutePath());

        if (sessionId != null && !sessionId.isBlank()) {
            cmd.add("--resume");
            cmd.add(sessionId);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);           // keep stderr separate
        pb.environment().put("ANTHROPIC_API_KEY", ""); // Claude CLI reads its own config

        Process process = pb.start();

        String resultText = null;
        String resultSession = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    String type = json.has("type") ? json.get("type").getAsString() : "";

                    if ("result".equals(type)) {
                        if (json.has("result")) {
                            resultText = json.get("result").getAsString();
                        }
                        if (json.has("session_id")) {
                            resultSession = json.get("session_id").getAsString();
                        }
                    }
                } catch (Exception ignored) {
                    // Non-JSON lines (debug output) — skip
                }
            }
        }

        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new Exception("Claude timed out after " + timeout + "s");
        }

        if (resultText == null) {
            // Try to get stderr for a useful error message
            String stderr = new String(process.getErrorStream().readAllBytes()).trim();
            throw new Exception("Keine Antwort von Claude." + (stderr.isEmpty() ? "" : " " + stderr));
        }

        return new ClaudeResult(resultText, resultSession);
    }
}
