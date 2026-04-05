const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

async function api(path, opts = {}) {
    const res = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...opts
    });
    if (res.status === 401) { showLogin(); throw new Error('Unauthorized'); }
    return res;
}

// --- Auth ---
function showLogin() { $('#login-page').classList.remove('hidden'); $('#app').classList.add('hidden'); }
function showApp() { $('#login-page').classList.add('hidden'); $('#app').classList.remove('hidden'); loadAll(); }

$('#login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const res = await api('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: $('#username').value, password: $('#password').value })
    });
    if (res.ok) showApp();
    else $('#login-error').textContent = 'Invalid credentials';
});

$('#logout-btn').addEventListener('click', async () => {
    await api('/api/auth/logout', { method: 'POST' });
    showLogin();
});

// --- Tabs ---
$$('nav a[data-tab]').forEach(a => a.addEventListener('click', (e) => {
    e.preventDefault();
    $$('nav a[data-tab]').forEach(x => x.classList.remove('active'));
    a.classList.add('active');
    $$('.tab').forEach(t => t.classList.add('hidden'));
    $(`#tab-${a.dataset.tab}`).classList.remove('hidden');
    if (a.dataset.tab === 'transactions') loadDropdowns();
    if (a.dataset.tab === 'holdings') loadHoldingDropdowns();
}));

// --- CRUD helpers ---
async function loadTable(url, tbodySelector, rowFn) {
    const res = await api(url);
    const data = await res.json();
    const tbody = $(tbodySelector);
    tbody.innerHTML = data.map(rowFn).join('');
}

async function deleteItem(url, loadFn) {
    await api(url, { method: 'DELETE' });
    loadFn();
}

// --- Clients ---
function loadClients() {
    loadTable('/api/clients', '#clients-table tbody', c =>
        `<tr><td>${c.id}</td><td>${c.name}</td><td>${c.email||''}</td><td>${c.phone||''}</td>
         <td><button class="del-btn" onclick="deleteItem('/api/clients/${c.id}', loadClients)">Delete</button></td></tr>`
    );
}
$('#client-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    await api('/api/clients', { method: 'POST', body: JSON.stringify({
        name: $('#client-name').value, email: $('#client-email').value, phone: $('#client-phone').value
    })});
    $('#client-form').reset();
    loadClients();
});

// --- Schemes ---
function loadSchemes() {
    loadTable('/api/schemes', '#schemes-table tbody', s =>
        `<tr><td>${s.id}</td><td>${s.name}</td><td>${s.type||''}</td><td>${s.nav}</td>
         <td><button class="del-btn" onclick="deleteItem('/api/schemes/${s.id}', loadSchemes)">Delete</button></td></tr>`
    );
}
$('#scheme-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    await api('/api/schemes', { method: 'POST', body: JSON.stringify({
        name: $('#scheme-name').value, type: $('#scheme-type').value, nav: parseFloat($('#scheme-nav').value)
    })});
    $('#scheme-form').reset();
    loadSchemes();
});

// --- Transactions ---
async function loadDropdowns() {
    const [clients, schemes] = await Promise.all([
        api('/api/clients').then(r => r.json()),
        api('/api/schemes').then(r => r.json())
    ]);
    $('#tx-client').innerHTML = '<option value="">Select Client</option>' + clients.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    $('#tx-scheme').innerHTML = '<option value="">Select Scheme</option>' + schemes.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
}
function loadTransactions() {
    loadTable('/api/transactions', '#transactions-table tbody', t =>
        `<tr><td>${t.id}</td><td>${t.clientId}</td><td>${t.schemeId}</td><td>${t.type}</td><td>${t.units}</td><td>${t.amount}</td><td>${t.date}</td>
         <td><button class="del-btn" onclick="deleteItem('/api/transactions/${t.id}', loadTransactions)">Delete</button></td></tr>`
    );
}
$('#transaction-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    await api('/api/transactions', { method: 'POST', body: JSON.stringify({
        clientId: parseInt($('#tx-client').value), schemeId: parseInt($('#tx-scheme').value),
        type: $('#tx-type').value, units: parseFloat($('#tx-units').value),
        amount: parseFloat($('#tx-amount').value), date: $('#tx-date').value
    })});
    $('#transaction-form').reset();
    loadTransactions();
});

// --- Holdings ---
async function loadHoldingDropdowns() {
    const [clients, schemes] = await Promise.all([
        api('/api/clients').then(r => r.json()),
        api('/api/schemes').then(r => r.json())
    ]);
    $('#hold-client').innerHTML = '<option value="">All Clients</option>' + clients.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    $('#hold-scheme').innerHTML = '<option value="">All Schemes</option>' + schemes.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
}
function loadHoldings() {
    const params = new URLSearchParams();
    if ($('#hold-client').value) params.set('clientId', $('#hold-client').value);
    if ($('#hold-scheme').value) params.set('schemeId', $('#hold-scheme').value);
    if ($('#hold-date').value) params.set('asOfDate', $('#hold-date').value);
    if ($('#hold-sort').value) { params.set('sortBy', 'value'); params.set('sortOrder', $('#hold-sort').value); }
    loadTable(`/api/holdings?${params}`, '#holdings-table tbody', h =>
        `<tr><td>${h.clientName}</td><td>${h.schemeName}</td><td>${h.units}</td><td>${h.holdingValue}</td></tr>`
    );
}
$('#hold-filter-btn').addEventListener('click', loadHoldings);

// --- Load all ---
function loadAll() { loadClients(); loadSchemes(); loadTransactions(); loadHoldings(); }
