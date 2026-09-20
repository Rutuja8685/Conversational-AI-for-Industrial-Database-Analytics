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

    // Chat Console
    const queryForm = document.getElementById('queryForm');
    const promptInput = document.getElementById('promptInput');
    const clearPromptBtn = document.getElementById('clearPromptBtn');
    const chatFeed = document.getElementById('chatFeed');
    const welcomeBanner = document.getElementById('welcomeBanner');
    const dismissBannerBtn = document.getElementById('dismissBannerBtn');

    let currentUser = null;
    let chatHistory = JSON.parse(localStorage.getItem('sql_assistant_history') || '[]');

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
        renderChatHistory();
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

    // 3. Render Chat History Sidebar List
    function renderChatHistory() {
        if (!chatHistoryList) return;
        if (chatHistory.length === 0) {
            chatHistoryList.innerHTML = `<p class="empty-history-text">No previous conversations yet.</p>`;
            return;
        }

        chatHistoryList.innerHTML = '';
        chatHistory.slice().reverse().forEach(item => {
            const elem = document.createElement('div');
            elem.className = 'history-item';
            elem.innerHTML = `
                <div class="history-prompt">💬 ${escapeHtml(item.prompt)}</div>
                <div class="history-time">${item.time}</div>
            `;
            elem.addEventListener('click', () => {
                promptInput.value = item.prompt;
                promptInput.focus();
            });
            chatHistoryList.appendChild(elem);
        });
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
