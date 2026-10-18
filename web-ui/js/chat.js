const messagesEl = document.getElementById("messages");
const formEl = document.getElementById("chat-form");
const queryEl = document.getElementById("query");
const sendBtn = document.getElementById("send-btn");

let chatId = null;
let isStreaming = false;

/**
 * Убирает пустое состояние при первом сообщении.
 */
function clearEmptyState() {
    const emptyState = messagesEl.querySelector(".empty-state");
    if (emptyState) emptyState.remove();
}

function appendMessage(role, content) {
    clearEmptyState();

    const div = document.createElement("div");
    div.className = `message ${role}`;

    const bubble = document.createElement("div");
    bubble.className = "message-bubble";
    bubble.textContent = content;

    div.appendChild(bubble);
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;

    return bubble;
}

function appendSources(sources) {
    if (!sources || sources.length === 0) return;

    const div = document.createElement("div");
    div.className = "sources";

    const title = document.createElement("div");
    title.className = "sources-title";
    title.textContent = `📚 Источники (${sources.length}):`;
    div.appendChild(title);

    sources.forEach((s, i) => {
        const sourceDiv = document.createElement("div");
        sourceDiv.className = "source";

        const similarity = (s.similarity * 100).toFixed(1);
        sourceDiv.innerHTML = `
            <div class="source-header">
                <span class="source-similarity">[${i + 1}] ${similarity}%</span>
                <span class="source-doc">${escapeHtml(s.documentId?.substring(0, 8) || "")}…</span>
            </div>
            <div class="source-preview">${escapeHtml(s.preview || "")}</div>
        `;
        div.appendChild(sourceDiv);
    });

    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

function escapeHtml(str) {
    return String(str || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

async function sendQuery(query) {
    if (isStreaming) return;
    isStreaming = true;

    sendBtn.disabled = true;
    queryEl.disabled = true;

    appendMessage("user", query);
    const assistantBubble = appendMessage("assistant", "");
    assistantBubble.innerHTML = '<span class="typing-indicator">▊</span>';

    let answerText = "";
    let gotAnyToken = false;

    try {
        const response = await fetch("/api/rag/query", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${getToken()}`,
            },
            body: JSON.stringify({ query, chatId }),
        });

        if (response.status === 401) {
            clearAuth();
            window.location.href = "login.html";
            return;
        }

        if (!response.ok) {
            const errText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errText}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });

            // SSE-события разделяются \n\n
            const events = buffer.split("\n\n");
            buffer = events.pop(); // последний может быть неполным

            for (const event of events) {
                const line = event.trim();
                if (!line.startsWith("data:")) continue;

                const json = line.substring(5).trim();
                if (!json) continue;

                let chunk;
                try {
                    chunk = JSON.parse(json);
                } catch (e) {
                    console.warn("Failed to parse SSE chunk:", json);
                    continue;
                }

                handleChunk(chunk, {
                    onToken: (t) => {
                        if (!gotAnyToken) {
                            assistantBubble.textContent = "";
                            gotAnyToken = true;
                        }
                        answerText += t;
                        assistantBubble.textContent = answerText;
                        messagesEl.scrollTop = messagesEl.scrollHeight;
                    },
                    onSources: appendSources,
                    onDone: (id) => {
                        if (id) chatId = id;
                    },
                    onError: (msg) => {
                        assistantBubble.textContent = "⚠️ " + msg;
                    },
                });
            }
        }

        if (!gotAnyToken) {
            assistantBubble.textContent = "(пустой ответ)";
        }
    } catch (err) {
        console.error("Chat error:", err);
        assistantBubble.textContent = "⚠️ Ошибка: " + err.message;
    } finally {
        isStreaming = false;
        sendBtn.disabled = false;
        queryEl.disabled = false;
        queryEl.focus();
    }
}

function handleChunk(chunk, handlers) {
    switch (chunk.type) {
        case "token":
            if (chunk.content) handlers.onToken(chunk.content);
            break;
        case "sources":
            handlers.onSources(chunk.sources);
            break;
        case "done":
            handlers.onDone(chunk.chatId);
            break;
        case "error":
            handlers.onError(chunk.error || "Unknown error");
            break;
        default:
            console.warn("Unknown chunk type:", chunk.type);
    }
}

formEl.addEventListener("submit", (e) => {
    e.preventDefault();
    const query = queryEl.value.trim();
    if (!query || isStreaming) return;
    queryEl.value = "";
    sendQuery(query);
});

queryEl.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        formEl.requestSubmit();
    }
});

queryEl.focus();
