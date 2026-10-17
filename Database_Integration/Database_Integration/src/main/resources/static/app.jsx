// 🔐 AUTOMATED ROUTING GATE KEEPER
(function verifyActiveSession() {
    const activeSessionUser = localStorage.getItem("authenticatedUser");
    
    if (!activeSessionUser) {
        window.location.href = "login.html";
    } else {
        window.addEventListener("DOMContentLoaded", () => {
            const badge = document.getElementById("authenticatedUserBadge");
            if (badge) badge.innerText = `Operator: ${activeSessionUser}`;
        });
    }
})();

// Use relative path so it hits your Spring Boot controller perfectly
const API_BASE = "/api/chat"; 
let lastFetchedData = [];
let recentConversationsLog = [];

function handleKeyPress(e) { 
    if (e.key === 'Enter') sendChatMessage(); 
}

function toggleRightSidebar() { 
    document.getElementById("rightSidebar").classList.toggle("hidden"); 
}

function executeLogout() {
    localStorage.removeItem("authenticatedUser");
    window.location.href = "login.html";
}

function triggerQuickQuery(queryText) {
    document.getElementById("userPrompt").value = queryText;
    sendChatMessage();
}

function filterSchemaBrowser() {
    const query = document.getElementById("schemaSearch").value.toLowerCase();
    document.querySelectorAll(".schema-item").forEach(item => {
        item.style.display = item.innerText.toLowerCase().includes(query) ? "block" : "none";
    });
}

async function sendChatMessage() {
    const inputField = document.getElementById("userPrompt");
    const query = inputField.value.trim();
    if (!query) return;

    appendUserBubble(query);
    logRecentConversationRow(query); 
    inputField.value = "";
    const loaderId = appendLoaderBubble();

    try {
        const res = await fetch(`${API_BASE}/message`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: query })
        });
        const data = await res.json();
        document.getElementById(loaderId).remove();
        appendAiResponseBubble(data);
    } catch (err) {
        if(document.getElementById(loaderId)) document.getElementById(loaderId).remove();
        appendSystemNotice("Connection dropped. Ensure backend controller is listening.");
    }
}

function logRecentConversationRow(phrase) {
    if(recentConversationsLog.includes(phrase)) return;
    recentConversationsLog.unshift(phrase);
    
    const listContainer = document.getElementById("recentChatsHistoryContainer");
    if (!listContainer) return;
    listContainer.innerHTML = ""; 
    
    recentConversationsLog.slice(0, 8).forEach(item => {
        const cleanDisplayStr = item.length > 28 ? item.substring(0, 26) + "..." : item;
        const linkElement = document.createElement("button");
        linkElement.className = "w-full text-left px-3 py-2 rounded-lg hover:bg-gray-800 hover:text-white transition-all text-gray-400 font-medium truncate block flex items-center gap-2 select-none border border-transparent hover:border-gray-700/50";
        linkElement.innerHTML = `<i class="fa-regular fa-comment text-gray-600 flex-shrink-0"></i> <span class="truncate">${cleanDisplayStr}</span>`;
        linkElement.onclick = () => triggerQuickQuery(item);
        listContainer.appendChild(linkElement);
    });
}

function appendUserBubble(text) {
    const container = document.getElementById("chatTimeline");
    const wrapper = document.createElement("div");
    wrapper.className = "flex gap-3 items-start max-w-[80%] self-end ml-auto flex-row-reverse animate-fade-in";
    wrapper.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm"><i class="fa-solid fa-user text-sm"></i></div>
        <div class="bg-indigo-600 text-white rounded-2xl rounded-tr-none px-4 py-2.5 shadow-sm text-sm">${text}</div>`;
    container.appendChild(wrapper);
    container.scrollTop = container.scrollHeight;
}

function appendLoaderBubble() {
    const id = "loader_" + Date.now();
    const container = document.getElementById("chatTimeline");
    const wrapper = document.createElement("div");
    wrapper.id = id;
    wrapper.className = "flex gap-4 items-start max-w-[85%] animate-pulse";
    wrapper.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-gray-200 border border-gray-300 flex items-center justify-center text-gray-500 flex-shrink-0"><i class="fa-solid fa-gear fa-spin text-sm"></i></div>
        <div class="bg-white border border-gray-200 rounded-2xl rounded-tl-none px-4 py-2.5 shadow-sm text-gray-500 text-sm italic">Analyzing warehouse assets schema...</div>`;
    container.appendChild(wrapper);
    container.scrollTop = container.scrollHeight;
    return id;
}

function appendSystemNotice(text) {
    const container = document.getElementById("chatTimeline");
    if (!container) return;
    const notice = document.createElement("div");
    notice.className = "w-full text-center my-1 text-xs font-medium text-gray-400 italic bg-gray-200/40 rounded-lg py-1.5 max-w-md mx-auto border border-gray-200/50";
    notice.innerText = text;
    container.appendChild(notice);
    container.scrollTop = container.scrollHeight;
}

function appendAiResponseBubble(data) {
    const container = document.getElementById("chatTimeline");
    const wrapper = document.createElement("div");
    wrapper.className = "flex gap-4 items-start w-full animate-fade-in";
    
    let innerContent = "";

    if (data.type === "DATA") {
        lastFetchedData = data.data;
        innerContent = `
            <p class="mb-3 font-medium text-gray-800"><i class="fa-solid fa-square-poll-horizontal text-indigo-500 mr-1"></i> Data transaction completed:</p>
            ${renderTableHtml(data.data)}
            <div class="mt-3 flex gap-2 justify-end border-t border-gray-100 pt-2.5">
                <button onclick="exportCSV()" class="text-xs font-semibold px-2.5 py-1.5 border border-gray-200 hover:bg-gray-50 rounded text-gray-600 flex items-center gap-1"><i class="fa-solid fa-file-csv text-emerald-600"></i> Export CSV</button>
                <button onclick="exportExcel()" class="text-xs font-semibold px-2.5 py-1.5 border border-gray-200 hover:bg-gray-50 rounded text-gray-600 flex items-center gap-1"><i class="fa-solid fa-file-excel text-green-600"></i> Export Excel</button>
            </div>`;
    } else if (data.type === "CONFIRMATION") {
        innerContent = `<p class="text-amber-700 font-semibold mb-1"><i class="fa-solid fa-triangle-exclamation mr-1 text-amber-500"></i> ${data.text}</p>`;
    } else if (data.type === "ERROR") {
        innerContent = `<p class="text-red-700 font-medium"><i class="fa-solid fa-circle-xmark mr-1 text-red-500"></i> ${data.text}</p>`;
    } else {
        innerContent = `<p class="text-indigo-700 font-medium">${data.text}</p>`;
    }

    if (data.sql) {
        innerContent += `
            <div class="mt-4 bg-gray-50 rounded-lg p-3 border border-gray-200">
                <span class="text-[10px] font-bold uppercase tracking-wider text-gray-400 block mb-1.5 font-sans">Compiled Engine SQL:</span>
                <code class="text-xs font-mono text-indigo-600 block whitespace-pre-wrap select-all bg-white p-2 border border-gray-100 rounded shadow-inner">${data.sql}</code>
            </div>`;
    }

    wrapper.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-white border border-gray-200 flex items-center justify-center text-indigo-600 flex-shrink-0 shadow-sm"><i class="fa-solid fa-robot text-sm"></i></div>
        <div class="bg-white border border-gray-200 rounded-2xl rounded-tl-none p-5 shadow-sm text-sm text-gray-700 flex-1 max-w-full overflow-hidden">${innerContent}</div>`;
        
    container.appendChild(wrapper);
    container.scrollTop = container.scrollHeight;
}

function renderTableHtml(data) {
    if (!data || data.length === 0) return '<div class="text-gray-400 italic p-2 text-center border border-dashed rounded">Empty record set matrix returned.</div>';
    const headers = Object.keys(data[0]);
    let html = `<div class="overflow-x-auto border border-gray-200 rounded-lg shadow-sm bg-white"><table class="w-full text-left border-collapse text-xs">`;
    html += `<thead><tr class="bg-gray-50 text-gray-500 font-semibold border-b border-gray-200 uppercase tracking-wider">`;
    headers.forEach(h => html += `<th class="p-2.5 border-r border-gray-200/60">${h.replace('_', ' ')}</th>`);
    html += `</tr></thead><tbody class="divide-y divide-gray-100 text-gray-600">`;
    data.forEach(row => {
        html += `<tr class="hover:bg-indigo-50/20 transition-colors">`;
        headers.forEach(h => html += `<td class="p-2.5 border-r border-gray-100 font-medium whitespace-nowrap">${row[h] !== null ? row[h] : 'NULL'}</td>`);
        html += `</tr>`;
    });
    return html + `</tbody></table></div>`;
}

function exportCSV() {
    if (lastFetchedData.length === 0) return alert("No active data entries to export.");
    const headers = Object.keys(lastFetchedData[0]);
    const rows = [headers.join(','), ...lastFetchedData.map(r => headers.map(h => `"${r[h]}"`).join(','))];
    triggerDownload(new Blob([rows.join('\n')], { type: 'text/csv' }), `Plant_Report_${Date.now()}.csv`);
}

function exportExcel() {
    if (lastFetchedData.length === 0) return alert("No active data entries to export.");
    const headers = Object.keys(lastFetchedData[0]);
    let table = '<table border="1"><tr>' + headers.map(h => `<th>${h}</th>`).join('') + '</tr>';
    lastFetchedData.forEach(r => table += '<tr>' + headers.map(h => `<td>${r[h] ?? ''}</td>`).join('') + '</tr>');
    triggerDownload(new Blob([table + '</table>'], { type: 'application/vnd.ms-excel' }), `Plant_Report_${Date.now()}.xls`);
}

function triggerDownload(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
}