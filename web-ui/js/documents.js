const uploadForm = document.getElementById("upload-form");
const fileInput = document.getElementById("file");
const uploadBtn = document.getElementById("upload-btn");
const uploadStatus = document.getElementById("upload-status");
const tableBody = document.getElementById("documents-body");
const refreshBtn = document.getElementById("refresh-btn");

function formatSize(bytes) {
    if (bytes == null) return "—";
    const units = ["B", "KB", "MB", "GB"];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) {
        size /= 1024;
        i++;
    }
    return `${size.toFixed(1)} ${units[i]}`;
}

function formatDate(iso) {
    if (!iso) return "—";
    try {
        return new Date(iso).toLocaleString("ru-RU", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    } catch {
        return iso;
    }
}

function escapeHtml(str) {
    return String(str || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

async function loadDocuments() {
    tableBody.innerHTML =
        '<tr><td colspan="6" class="empty-state">Загрузка...</td></tr>';
    try {
        const docs = await apiFetch("/documents");
        renderDocuments(docs);
    } catch (err) {
        tableBody.innerHTML = `<tr><td colspan="6" class="empty-state error">Ошибка: ${escapeHtml(err.message)}</td></tr>`;
    }
}

function renderDocuments(docs) {
    if (!docs || docs.length === 0) {
        tableBody.innerHTML =
            '<tr><td colspan="6" class="empty-state">Нет документов. Загрузите первый!</td></tr>';
        return;
    }

    tableBody.innerHTML = docs
        .map(
            (doc) => `
        <tr>
            <td>${escapeHtml(doc.filename)}</td>
            <td>${escapeHtml(doc.contentType || "—")}</td>
            <td>${formatSize(doc.sizeBytes)}</td>
            <td><span class="status-${doc.status}">${doc.status}</span></td>
            <td>${formatDate(doc.createdAt)}</td>
            <td><button class="delete-btn" data-id="${doc.id}">Удалить</button></td>
        </tr>
    `,
        )
        .join("");

    tableBody.querySelectorAll(".delete-btn").forEach((btn) => {
        btn.addEventListener("click", () => deleteDocument(btn.dataset.id));
    });
}

async function deleteDocument(id) {
    if (!confirm("Удалить документ и его эмбеддинги?")) return;
    try {
        await apiFetch(`/documents/${id}`, { method: "DELETE" });
        await loadDocuments();
    } catch (err) {
        alert("Ошибка удаления: " + err.message);
    }
}

uploadForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const file = fileInput.files[0];
    if (!file) return;

    uploadBtn.disabled = true;
    uploadStatus.className = "pending";
    uploadStatus.textContent = "⏳ Загрузка и обработка...";
    uploadStatus.style.display = "block";

    const formData = new FormData();
    formData.append("file", file);

    try {
        const result = await apiFetch("/documents", {
            method: "POST",
            body: formData,
        });

        uploadStatus.className = "success";
        uploadStatus.textContent = `✅ Загружено: ${result.filename}. Через 5–10 секунд документ будет доступен в чате.`;

        fileInput.value = "";
        await loadDocuments();

        // Автообновление через 8 секунд — проверить, что статус стал READY
        setTimeout(loadDocuments, 8000);
    } catch (err) {
        uploadStatus.className = "error";
        uploadStatus.textContent = "❌ Ошибка: " + err.message;
    } finally {
        uploadBtn.disabled = false;
    }
});

refreshBtn.addEventListener("click", loadDocuments);

loadDocuments();
