/**
 * HmyAI – MCP Server
 * Bridges Claude's tool calls to the Minecraft plugin's HTTP API (localhost).
 * Launched automatically by the plugin on startup.
 *
 * Environment variables set by the plugin:
 *   MC_API      – e.g. http://127.0.0.1:25580
 *   MC_API_KEY  – secret shared with the plugin
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
    CallToolRequestSchema,
    ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

const API_BASE = process.env.MC_API     || "http://127.0.0.1:25580";
const API_KEY  = process.env.MC_API_KEY || "";

// ── HTTP helper ────────────────────────────────────────────────────────────

async function api(path, body = null) {
    const opts = {
        method : body ? "POST" : "GET",
        headers: {
            "Content-Type": "application/json",
            "X-API-Key"   : API_KEY,
        },
    };
    if (body) opts.body = JSON.stringify(body);
    const res  = await fetch(`${API_BASE}${path}`, opts);
    const text = await res.text();
    try   { return JSON.parse(text); }
    catch { return { raw: text }; }
}

function text(obj) {
    return [{ type: "text", text: typeof obj === "string" ? obj : JSON.stringify(obj, null, 2) }];
}

// ── Tool definitions ───────────────────────────────────────────────────────

const TOOLS = [
    {
        name       : "execute_command",
        description: "Führt einen Minecraft-Konsolenbefehl aus (ohne führendes /). Gibt Erfolg/Fehlschlag zurück.",
        inputSchema: {
            type      : "object",
            properties: { command: { type: "string", description: "Konsolenbefehl, z.B. 'give Haaremy diamond 64'" } },
            required  : ["command"],
        },
    },
    {
        name       : "get_server_info",
        description: "Gibt Serverversion, MOTD, TPS, Spieleranzahl und Weltliste zurück.",
        inputSchema: { type: "object", properties: {} },
    },
    {
        name       : "get_players",
        description: "Listet alle Online-Spieler mit Name, UUID, Welt und Position.",
        inputSchema: { type: "object", properties: {} },
    },
    {
        name       : "get_player_info",
        description: "Detaillierte Infos zu einem Spieler: Position, Health, Gamemode, Level, Inventar-Zusammenfassung.",
        inputSchema: {
            type      : "object",
            properties: { name: { type: "string", description: "Spielername (exakt)" } },
            required  : ["name"],
        },
    },
    {
        name       : "get_world_info",
        description: "Zeit, Wetter, Difficulty und Seed einer Welt.",
        inputSchema: {
            type      : "object",
            properties: { world: { type: "string", description: "Weltname (Standard: erste Welt)" } },
        },
    },
    {
        name       : "get_block",
        description: "Gibt den Blocktyp an den angegebenen Koordinaten zurück.",
        inputSchema: {
            type      : "object",
            properties: {
                x    : { type: "number" },
                y    : { type: "number" },
                z    : { type: "number" },
                world: { type: "string", description: "Weltname (optional)" },
            },
            required: ["x", "y", "z"],
        },
    },
    {
        name       : "send_message",
        description: "Sendet eine Chat-Nachricht an einen Spieler oder alle (target='@a').",
        inputSchema: {
            type      : "object",
            properties: {
                target : { type: "string", description: "Spielername oder @a" },
                message: { type: "string" },
            },
            required: ["target", "message"],
        },
    },
];

// ── MCP Server ─────────────────────────────────────────────────────────────

const server = new Server(
    { name: "minecraft", version: "1.0.0" },
    { capabilities: { tools: {} } },
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools: TOOLS }));

server.setRequestHandler(CallToolRequestSchema, async (req) => {
    const { name, arguments: args } = req.params;
    try {
        let result;
        switch (name) {
            case "execute_command":
                result = await api("/tool/command", { command: args.command });
                break;
            case "get_server_info":
                result = await api("/tool/server");
                break;
            case "get_players":
                result = await api("/tool/players");
                break;
            case "get_player_info":
                result = await api("/tool/player?name=" + encodeURIComponent(args.name));
                break;
            case "get_world_info":
                result = await api("/tool/world" + (args.world ? "?world=" + encodeURIComponent(args.world) : ""));
                break;
            case "get_block":
                result = await api(`/tool/block?x=${args.x}&y=${args.y}&z=${args.z}${args.world ? "&world=" + args.world : ""}`);
                break;
            case "send_message":
                result = await api("/tool/message", { target: args.target, message: args.message });
                break;
            default:
                return { content: text(`Unbekanntes Tool: ${name}`), isError: true };
        }
        return { content: text(result) };
    } catch (err) {
        return { content: text(`Fehler: ${err.message}`), isError: true };
    }
});

const transport = new StdioServerTransport();
await server.connect(transport);
