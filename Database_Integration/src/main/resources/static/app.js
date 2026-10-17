<<<<<<< HEAD
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
    localStorage.removeItem("username");
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

// Get the active operator name dynamically from session or UI
function getActiveUsername() {
    return localStorage.getItem("authenticatedUser") || 
           localStorage.getItem("username") || 
           document.getElementById("operatorName")?.innerText.trim().replace("Operator: ", "") || 
           "sejal";
}

async function sendChatMessage() {
    const inputField = document.getElementById("userPrompt");
    const query = inputField.value.trim();
    if (!query) return;

    const currentUsername = getActiveUsername();

    appendUserBubble(query);
    logRecentConversationRow(query); 
    inputField.value = "";
    const loaderId = appendLoaderBubble();

    try {
        const res = await fetch('/api/chat/send', {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ 
                username: currentUsername, // Uses active logged-in user dynamically
                prompt: query 
            })
        });

        const data = await res.json();
        
        if (document.getElementById(loaderId)) {
            document.getElementById(loaderId).remove();
        }

        if (data.type) {
            appendAiResponseBubble(data);
        } else {
            const botMessage = data.response || data.reply || data.aiResponse || "No response text found.";
            appendAiResponseBubble({
                type: "TEXT",
                text: botMessage
            });
        }

    } catch (err) {
        if (document.getElementById(loaderId)) {
            document.getElementById(loaderId).remove();
        }
        appendSystemNotice("Connection dropped. Ensure backend controller is listening.");
    }
}

function logRecentConversationRow(phrase) {
    if (recentConversationsLog.includes(phrase)) return;
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

function getChatContainer() {
    return document.getElementById("chatTimeline") || 
           document.getElementById("chatContainer") || 
           document.querySelector(".flex-1.overflow-y-auto");
}

function appendUserBubble(messageText) {
    const container = getChatContainer();
    if (!container) return;

    const userHtml = `
        <div class="flex justify-end items-center gap-3 my-4">
            <div class="bg-indigo-600 text-white px-4 py-2 rounded-2xl max-w-md shadow-sm text-sm">
                ${messageText}
            </div>
            <div class="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center text-white text-xs font-bold">
                <i class="fa-solid fa-user"></i>
            </div>
        </div>`;
    container.insertAdjacentHTML("beforeend", userHtml);
    container.scrollTop = container.scrollHeight;
}

function appendLoaderBubble() {
    const id = "loader_" + Date.now();
    const container = getChatContainer();
    if (!container) return id;

    const wrapper = document.createElement("div");
    wrapper.id = id;
    wrapper.className = "flex gap-4 items-start max-w-[85%] animate-pulse my-4";
    wrapper.innerHTML = `
        <div class="w-8 h-8 rounded-full bg-gray-200 border border-gray-300 flex items-center justify-center text-gray-500 flex-shrink-0"><i class="fa-solid fa-gear fa-spin text-sm"></i></div>
        <div class="bg-white border border-gray-200 rounded-2xl rounded-tl-none px-4 py-2.5 shadow-sm text-gray-500 text-sm italic">Analyzing warehouse assets schema...</div>`;
    container.appendChild(wrapper);
    container.scrollTop = container.scrollHeight;
    return id;
}

function appendSystemNotice(text) {
    const container = getChatContainer();
    if (!container) return;
    const notice = document.createElement("div");
    notice.className = "w-full text-center my-2 text-xs font-medium text-gray-400 italic bg-gray-200/40 rounded-lg py-1.5 max-w-md mx-auto border border-gray-200/50";
    notice.innerText = text;
    container.appendChild(notice);
    container.scrollTop = container.scrollHeight;
}

function appendAiResponseBubble(data) {
    const container = getChatContainer();
    if (!container) return;

    const wrapper = document.createElement("div");
    wrapper.className = "flex gap-4 items-start w-full animate-fade-in my-4";
    
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
        innerContent = `<p class="text-indigo-700 font-medium">${data.text || data}</p>`;
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

async function loadUserChatHistory(username) {
    try {
        const response = await fetch(`/api/chat/history/${username}`);
        if (!response.ok) return;

        const chatLogs = await response.json();
        
        // 1. Populate sidebar with past prompts
        chatLogs.forEach(chat => {
            if (chat.userPrompt) {
                logRecentConversationRow(chat.userPrompt);
            }
        });

        // 2. Clear main timeline so logging in opens a clean workspace
        const container = getChatContainer();
        if (container) {
            // Keep initial security banner and greeting only
            const staticBanner = container.firstElementChild ? container.firstElementChild.cloneNode(true) : null;
            container.innerHTML = "";
            if (staticBanner) container.appendChild(staticBanner);
        }

    } catch (error) {
        console.error("Failed to load chat history:", error);
    }
}
function logRecentConversationRow(phrase) {
    if (recentConversationsLog.includes(phrase)) return;
    recentConversationsLog.unshift(phrase);
    
    const listContainer = document.getElementById("recentChatsHistoryContainer");
    if (!listContainer) return;
    listContainer.innerHTML = ""; 
    
    recentConversationsLog.slice(0, 8).forEach(item => {
        const cleanDisplayStr = item.length > 28 ? item.substring(0, 26) + "..." : item;
        const linkElement = document.createElement("button");
        linkElement.className = "w-full text-left px-3 py-2 rounded-lg hover:bg-gray-800 hover:text-white transition-all text-gray-400 font-medium truncate block flex items-center gap-2 select-none border border-transparent hover:border-gray-700/50";
        linkElement.innerHTML = `<i class="fa-regular fa-comment text-gray-600 flex-shrink-0"></i> <span class="truncate">${cleanDisplayStr}</span>`;
        
        // Clicking a history item re-populates the prompt into chat
        linkElement.onclick = () => {
            triggerQuickQuery(item);
        };
        
        listContainer.appendChild(linkElement);
    });
}

// Single initialization entry point
document.addEventListener("DOMContentLoaded", () => {
    const user = getActiveUsername();
    loadUserChatHistory(user);
});
=======
document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const loginModal = document.getElementById('loginModal');
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('usernameInput');
    const passwordInput = document.getElementById('passwordInput');
    const loginError = document.getElementById('loginError');
    const quickLoginBtns = document.querySelectorAll('.quick-login-btn');
    const activeUserName = document.getElementById('activeUserName');
    const roleBadge = document.getElementById('roleBadge');
    const logoutBtn = document.getElementById('logoutBtn');

    // Sidebar Overview & History
    const statTables = document.getElementById('statTables');
    const statSensors = document.getElementById('statSensors');
    const statLogs = document.getElementById('statLogs');
    const statDepts = document.getElementById('statDepts');
    const refreshOverviewBtn = document.getElementById('refreshOverviewBtn');
    const suggestionList = document.getElementById('suggestionList');
    const chatHistoryList = document.getElementById('chatHistoryList');
    const clearHistoryBtn = document.getElementById('clearHistoryBtn');
    const newChatBtn = document.getElementById('newChatBtn');

    // Chat Console
    const queryForm = document.getElementById('queryForm');
    const promptInput = document.getElementById('promptInput');
    const clearPromptBtn = document.getElementById('clearPromptBtn');
    const chatFeed = document.getElementById('chatFeed');
    const welcomeBanner = document.getElementById('welcomeBanner');
    const dismissBannerBtn = document.getElementById('dismissBannerBtn');

    let currentUser = null;
    // Session state
    let sessions = []; // Array of session objects {id, title}
    let currentSessionId = null;

    // Automatic Scroll To Bottom Helper
    function scrollToBottom() {
        setTimeout(() => {
            chatFeed.scrollTop = chatFeed.scrollHeight;
        }, 60);
    }

    // Dismiss Welcome Info Banner
    if (dismissBannerBtn) {
        dismissBannerBtn.addEventListener('click', () => {
            if (welcomeBanner) welcomeBanner.style.display = 'none';
        });
    }

    // Check stored user session
    const storedUser = localStorage.getItem('sql_assistant_user');
    if (storedUser) {
        try {
            currentUser = JSON.parse(storedUser);
            applyUserSession(currentUser);
        } catch(e) {
            showLoginModal();
        }
    } else {
        showLoginModal();
    }

    function showLoginModal() {
        loginModal.style.display = 'flex';
    }

    function hideLoginModal() {
        loginModal.style.display = 'none';
    }

    function applyUserSession(user) {
        currentUser = user;
        localStorage.setItem('sql_assistant_user', JSON.stringify(user));
        activeUserName.textContent = `${user.fullName} (${user.username})`;
        roleBadge.textContent = user.role;
        roleBadge.className = `role-badge role-${user.role.toLowerCase()}`;
        hideLoginModal();

        // Initial Data Fetch
        loadDatabaseOverview();
        loadSuggestions();
        loadSessions();
    }

    // Login Form Submit
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.style.display = 'none';
        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        await performLogin(username, password);
    });

    // Quick Login Demo Buttons
    quickLoginBtns.forEach(btn => {
        btn.addEventListener('click', async () => {
            const u = btn.getAttribute('data-user');
            const p = btn.getAttribute('data-pass');
            await performLogin(u, p);
        });
    });

    async function performLogin(username, password) {
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await res.json();
            if (res.ok && data.success) {
                applyUserSession(data.user);
            } else {
                loginError.textContent = data.message || 'Invalid username or password';
                loginError.style.display = 'block';
            }
        } catch (err) {
            loginError.textContent = 'Server connection error: ' + err.message;
            loginError.style.display = 'block';
        }
    }

    // Logout
    logoutBtn.addEventListener('click', () => {
        currentUser = null;
        localStorage.removeItem('sql_assistant_user');
        activeUserName.textContent = 'Not Logged In';
        showLoginModal();
    });

    // 1. Fetch Live Database Overview Counts from MySQL
    async function loadDatabaseOverview() {
        try {
            const res = await fetch('/api/schema/overview');
            const data = await res.json();

            statTables.textContent = data.totalTables || 0;
            statSensors.textContent = data.totalSensors || 0;
            statLogs.textContent = data.totalLogs || 0;
            statDepts.textContent = data.totalDepartments || 0;
        } catch (err) {
            console.warn('Could not load MySQL database overview counts', err);
        }
    }

    refreshOverviewBtn.addEventListener('click', loadDatabaseOverview);

    // 2. Recommended Prompt Chips
    async function loadSuggestions() {
        try {
            const res = await fetch('/api/schema/suggest');
            let suggestions = await res.json();

            // Prepend metadata count prompt chip
            suggestions.unshift({
                id: 's0',
                category: 'Database Metrics',
                prompt: 'how many data you have',
                roleRequired: 'VIEWER',
                operationType: 'SELECT'
            });

            suggestionList.innerHTML = '';
            suggestions.forEach(s => {
                const card = document.createElement('div');
                card.className = 'suggestion-card';
                card.innerHTML = `
                    <div class="suggestion-header">
                        <span>${s.category}</span>
                        <span class="op-tag ${s.operationType}">${s.operationType}</span>
                    </div>
                    <div class="suggestion-prompt">${s.prompt}</div>
                `;
                card.addEventListener('click', () => {
                    promptInput.value = s.prompt;
                    promptInput.focus();
                });
                suggestionList.appendChild(card);
            });
        } catch (err) {
            suggestionList.innerHTML = `<div style="color:var(--text-muted)">Could not load suggestions</div>`;
        }
    }

    // 3. Load and Render Chat Sessions List
    async function loadSessions() {
        try {
            const res = await fetch('/api/chat/sessions');
            const data = await res.json();
            sessions = data || [];
            renderSessionList();
        } catch (err) {
            console.warn('Could not load chat sessions', err);
            sessions = [];
            renderSessionList();
        }
    }

    function renderSessionList() {
        if (!chatHistoryList) return;
        chatHistoryList.innerHTML = '';
        if (sessions.length === 0) {
            chatHistoryList.innerHTML = `<p class="empty-history-text">No previous conversations yet.</p>`;
            return;
        }
        sessions.slice().reverse().forEach(sess => {
            const elem = document.createElement('div');
            elem.className = 'history-item';
            elem.innerHTML = `
                <div class="history-prompt">📁 ${escapeHtml(sess.title || 'Untitled Session')}</div>
                <div class="history-time">${new Date(sess.createdAt).toLocaleString()}</div>
            `;
            if (sess.id === currentSessionId) {
                elem.classList.add('active-session');
            }
            elem.addEventListener('click', () => selectSession(sess.id));
            chatHistoryList.appendChild(elem);
        });
    }

    function selectSession(sessionId) {
        currentSessionId = sessionId;
        renderSessionList();
        loadSessionMessages(sessionId);
    }

    async function loadSessionMessages(sessionId) {
        try {
            const res = await fetch(`/api/chat/session/${sessionId}/messages`);
            const msgs = await res.json();
            renderMessages(msgs);
        } catch (err) {
            console.warn('Could not load session messages', err);
        }
    }

    function renderMessages(messages) {
        // Simple rendering of stored messages: user prompt and AI summary (if any)
        chatFeed.innerHTML = '';
        messages.forEach(msg => {
            const card = document.createElement('div');
            card.className = 'query-card';
            const header = `<div class="query-card-header"><span class="user-prompt-title">💬 "${escapeHtml(msg.userPrompt)}"</span><span class="status-badge">${msg.status || 'LOADED'}</span></div>`;
            const body = `<div class="query-card-body"><div class="summary-box"><strong>🤖 AI Summary:</strong> ${escapeHtml(msg.aiSummary || '')}</div></div>`;
            card.innerHTML = header + body;
            chatFeed.appendChild(card);
        });
        scrollToBottom();
    }


    function addChatHistoryItem(promptText) {
        const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        chatHistory.push({ prompt: promptText, time: timeStr, id: Date.now() });
        if (chatHistory.length > 30) chatHistory.shift();
        localStorage.setItem('sql_assistant_history', JSON.stringify(chatHistory));
        renderChatHistory();
    }

    clearHistoryBtn.addEventListener('click', () => {
        chatHistory = [];
        localStorage.removeItem('sql_assistant_history');
        renderChatHistory();
    });
    // New Chat Button - create a fresh session
    newChatBtn.addEventListener('click', async () => {
        try {
            const res = await fetch('/api/chat/session/new', { method: 'POST' });
            const data = await res.json();
            if (data && data.id) {
                currentSessionId = data.id;
                await loadSessions();
                selectSession(currentSessionId);
                chatFeed.innerHTML = '';
            }
        } catch (err) {
            console.warn('Failed to create new chat session', err);
        }
    });

    clearPromptBtn.addEventListener('click', () => {
        promptInput.value = '';
    });

    // 4. Submit Natural Language Query
    queryForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const prompt = promptInput.value.trim();
        if (!prompt) return;

        if (!currentUser) {
            showLoginModal();
            return;
        }

        // Hide welcome banner on first query
        if (welcomeBanner) welcomeBanner.style.display = 'none';

        const userId = currentUser.id;
        addChatHistoryItem(prompt);

        // Append query card in processing state
        const cardId = 'card-' + Date.now();
        const card = document.createElement('div');
        card.className = 'query-card';
        card.id = cardId;
        card.innerHTML = `
            <div class="query-card-header">
                <span class="user-prompt-title">💬 "${escapeHtml(prompt)}"</span>
                <span class="status-badge">PROCESSING...</span>
            </div>
            <div class="query-card-body">
                <div class="loading-spinner">Translating Natural Language to SQL using LLM...</div>
            </div>
        `;

        chatFeed.appendChild(card);
        scrollToBottom();
        promptInput.value = '';

        try {
            const res = await fetch('/api/chat/query', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ prompt, userId })
            });
            const data = await res.json();
            renderQueryCard(card, data, userId);
            scrollToBottom();
            loadDatabaseOverview(); // Refresh live stat counts
        } catch (err) {
            card.querySelector('.query-card-body').innerHTML = `
                <div style="color:var(--danger)">Execution Error: ${err.message}</div>
            `;
            scrollToBottom();
        }
    });

    // 5. Render Query Card Result in exact requested order:
    // 1. User Prompt Bubble (card header)
    // 2. AI Response / Summary Text
    // 3. Executed SQL Query Block (with Copy SQL button)
    // 4. MySQL Output Data Table (with overflow-x scrollbar)
    // 5. Export Excel/CSV Button (directly beside/below table header)
    // 6. Confirmation Modal/Buttons (for UPDATE/DELETE)
    function renderQueryCard(card, data, userId) {
        const header = card.querySelector('.query-card-header');
        const body = card.querySelector('.query-card-body');

        const statusClass = data.status || 'EXECUTED';
        header.querySelector('.status-badge').className = `status-badge ${statusClass}`;
        header.querySelector('.status-badge').textContent = statusClass.replace('_', ' ');

        let bodyHtml = '';

        // 2. AI Response & Summary Text
        if (data.summary) {
            bodyHtml += `
                <div class="summary-box">
                    <strong>🤖 AI Summary & Response:</strong> ${escapeHtml(data.summary)}
                </div>
            `;
        }

        // 3. Executed SQL Query Block with Copy button
        const sqlText = data.sql || '';
        const sqlBlockId = 'sql-' + Date.now() + '-' + Math.floor(Math.random() * 1000);
        bodyHtml += `
            <div class="sql-block-wrapper">
                <div class="sql-block-header">
                    <span>-- Executed ANSI SQL (${data.operationType || 'QUERY'})</span>
                    <button class="copy-sql-btn" data-sql-id="${sqlBlockId}">📋 Copy SQL</button>
                </div>
                <div class="sql-block">
                    <code id="${sqlBlockId}">${escapeHtml(sqlText)}</code>
                </div>
            </div>
            <div class="sql-explanation">ℹ️ ${escapeHtml(data.explanation || '')}</div>
        `;

        // 4 & 5. MySQL Data Table + Export to Excel/CSV Button
        if (data.status === 'EXECUTED' && data.result && data.result.columns) {
            const tableId = 'table-' + Date.now() + '-' + Math.floor(Math.random() * 1000);
            bodyHtml += `
                <div class="table-section-header">
                    <span class="table-section-title">📊 Query Results (${data.result.rowCount || 0} row(s))</span>
                    <button class="export-excel-btn" data-table-id="${tableId}">📊 Export to CSV / Excel</button>
                </div>
                <div class="table-container" id="${tableId}">
                    ${renderTable(data.result.columns, data.result.rows)}
                </div>
            `;
        } 
        // 6. Guardrail Confirmation Action Card (for UPDATE/DELETE)
        else if (data.status === 'NEEDS_CONFIRMATION') {
            bodyHtml += `
                <div class="guardrail-card" id="guardrail-${data.queryId}">
                    <div class="guardrail-title">
                        <span>🛡️ Safety Guardrail Triggered</span>
                    </div>
                    <p style="font-size:0.88rem;">This is a write operation (<strong>${data.operationType}</strong>) on table(s): <code>${(data.affectedTables || []).join(', ')}</code>.<br>
                    <strong>RBAC Policy:</strong> Only users with role <code>ADMIN</code> can confirm execution.</p>
                    <div class="guardrail-actions">
                        <button class="confirm-btn" data-query-id="${data.queryId}">Approve & Execute</button>
                        <button class="reject-btn" data-query-id="${data.queryId}">Cancel / Reject</button>
                    </div>
                </div>
            `;
        }
        // Error state
        else if (data.status === 'ERROR') {
            bodyHtml += `
                <div style="color:var(--danger); font-weight:500;">
                    ❌ Error: ${escapeHtml(data.error || 'Failed to process query')}
                </div>
            `;
        }

        body.innerHTML = bodyHtml;

        // Copy SQL Listener
        const copyBtn = body.querySelector('.copy-sql-btn');
        if (copyBtn) {
            copyBtn.addEventListener('click', () => {
                const codeElem = document.getElementById(sqlBlockId);
                if (codeElem) {
                    navigator.clipboard.writeText(codeElem.textContent).then(() => {
                        copyBtn.textContent = '✅ Copied!';
                        setTimeout(() => copyBtn.textContent = '📋 Copy SQL', 2000);
                    });
                }
            });
        }

        // Export to CSV / Excel Listener
        const exportBtn = body.querySelector('.export-excel-btn');
        if (exportBtn && data.result && data.result.columns) {
            exportBtn.addEventListener('click', () => {
                exportToCsv(data.result.columns, data.result.rows, 'query_export_' + Date.now() + '.csv');
            });
        }

        // Confirmation listeners
        if (data.status === 'NEEDS_CONFIRMATION' && data.queryId) {
            const confirmBtn = body.querySelector('.confirm-btn');
            const rejectBtn = body.querySelector('.reject-btn');

            confirmBtn.addEventListener('click', () => handleConfirmation(data.queryId, true, card));
            rejectBtn.addEventListener('click', () => handleConfirmation(data.queryId, false, card));
        }

        scrollToBottom();
    }

    // Export Table Data to CSV/Excel File
    function exportToCsv(columns, rows, filename) {
        if (!columns || !rows) return;
        let csvContent = 'data:text/csv;charset=utf-8,';
        csvContent += columns.map(c => `"${c}"`).join(',') + '\r\n';

        rows.forEach(row => {
            const rowStr = row.map(val => {
                if (val === null || val === undefined) return '""';
                return `"${String(val).replace(/"/g, '""')}"`;
            }).join(',');
            csvContent += rowStr + '\r\n';
        });

        const encodedUri = encodeURI(csvContent);
        const link = document.createElement('a');
        link.setAttribute('href', encodedUri);
        link.setAttribute('download', filename);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    // Handle Confirmation / Rejection Call (/api/chat/confirm)
    async function handleConfirmation(queryId, confirm, card) {
        if (!currentUser) return;
        const currentUserId = currentUser.id;
        const guardrailBox = card.querySelector(`#guardrail-${queryId}`);
        if (guardrailBox) {
            guardrailBox.innerHTML = `<div class="loading-spinner">Verifying RBAC credentials & processing confirmation...</div>`;
        }

        try {
            const res = await fetch('/api/chat/confirm', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ queryId, userId: currentUserId, confirm })
            });

            const data = await res.json();
            const header = card.querySelector('.query-card-header');

            header.querySelector('.status-badge').className = `status-badge ${data.status}`;
            header.querySelector('.status-badge').textContent = data.status.replace('_', ' ');

            if (data.status === 'BLOCKED_RBAC') {
                guardrailBox.className = 'guardrail-card';
                guardrailBox.style.borderColor = 'var(--danger)';
                guardrailBox.style.background = 'rgba(239, 68, 68, 0.1)';
                guardrailBox.innerHTML = `
                    <div style="color:var(--danger); font-weight:600;">
                        ⛔ Security Violation: Action Blocked by RBAC Policy
                    </div>
                    <p style="font-size:0.85rem; color:#fca5a5;">${escapeHtml(data.error)}</p>
                `;
            } else if (data.status === 'EXECUTED') {
                guardrailBox.outerHTML = `
                    <div class="summary-box" style="border-left-color: var(--success); background: rgba(16, 185, 129, 0.08);">
                        <strong>✅ Action Approved & Executed by ADMIN (${currentUser.username}):</strong> ${escapeHtml(data.summary)}
                    </div>
                `;
                loadDatabaseOverview(); // Refresh live stat counts
            } else if (data.status === 'REJECTED') {
                guardrailBox.outerHTML = `
                    <div class="summary-box" style="border-left-color: var(--text-muted); background: rgba(148, 163, 184, 0.08);">
                        <strong>🚫 Operation Cancelled:</strong> Query execution was rejected by user.
                    </div>
                `;
            }

            scrollToBottom();

        } catch (err) {
            if (guardrailBox) {
                guardrailBox.innerHTML = `<div style="color:var(--danger)">Error: ${err.message}</div>`;
            }
            scrollToBottom();
        }
    }

    // Render HTML Data Table
    function renderTable(columns, rows) {
        if (!columns || columns.length === 0) {
            return `<div class="placeholder-text">No columns returned</div>`;
        }
        if (!rows || rows.length === 0) {
            return `<div class="placeholder-text">No rows matched criteria</div>`;
        }

        let ths = columns.map(c => `<th>${escapeHtml(c)}</th>`).join('');
        let trs = rows.map(row => {
            let tds = row.map(cell => `<td>${cell === null ? '<span style="color:var(--text-muted)">NULL</span>' : escapeHtml(String(cell))}</td>`).join('');
            return `<tr>${tds}</tr>`;
        }).join('');

        return `<table><thead><tr>${ths}</tr></thead><tbody>${trs}</tbody></table>`;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }
});
>>>>>>> c53c816beb0269b25f2cde238dfd99efefe0017a
