const API = '/api/employees';
const app = document.getElementById('app');
const state = { page: 0, size: 10, search: '', department: '', status: '', sortBy: 'id', direction: 'desc' };

/* ---------- helpers ---------- */
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
const fmtDate = d => new Date(d + 'T00:00:00').toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });

function toast(msg, isErr) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast' + (isErr ? ' err' : '');
  setTimeout(() => t.classList.add('hidden'), 3000);
}

async function api(url, options) {
  const res = await fetch(url, options);
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }
  if (!res.ok) {
    const err = new Error((data && data.message) || 'Request failed');
    err.fieldErrors = data && data.errors;
    err.status = res.status;
    throw err;
  }
  return data;
}

const debounce = (fn, ms = 350) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; };

/* ---------- list page ---------- */
async function renderList() {
  const depts = await api(API + '/departments').catch(() => []);
  app.innerHTML = `
    <div class="card">
      <h2>Employees</h2>
      <div class="toolbar">
        <input id="search" placeholder="Search by name or email..." value="${esc(state.search)}">
        <select id="department"><option value="">All departments</option>
          ${depts.map(d => `<option ${d === state.department ? 'selected' : ''}>${esc(d)}</option>`).join('')}
        </select>
        <select id="status">
          <option value="">All statuses</option>
          <option value="ACTIVE" ${state.status === 'ACTIVE' ? 'selected' : ''}>Active</option>
          <option value="INACTIVE" ${state.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option>
        </select>
        <button class="btn" id="export">Export CSV</button>
      </div>
      <div id="table-area"><p class="empty">Loading...</p></div>
    </div>`;

  document.getElementById('search').addEventListener('input', debounce(e => { state.search = e.target.value; state.page = 0; loadTable(); }));
  document.getElementById('department').addEventListener('change', e => { state.department = e.target.value; state.page = 0; loadTable(); });
  document.getElementById('status').addEventListener('change', e => { state.status = e.target.value; state.page = 0; loadTable(); });
  document.getElementById('export').addEventListener('click', () => {
    window.location.href = API + '/export?' + filterParams().toString();
  });
  loadTable();
}

function filterParams() {
  const p = new URLSearchParams();
  if (state.search) p.set('search', state.search);
  if (state.department) p.set('department', state.department);
  if (state.status) p.set('status', state.status);
  return p;
}

async function loadTable() {
  const area = document.getElementById('table-area');
  const p = filterParams();
  p.set('page', state.page); p.set('size', state.size);
  p.set('sortBy', state.sortBy); p.set('direction', state.direction);
  try {
    const data = await api(API + '?' + p.toString());
    if (data.totalPages > 0 && state.page >= data.totalPages) { state.page = data.totalPages - 1; return loadTable(); }
    const arrow = f => state.sortBy === f ? (state.direction === 'asc' ? ' ▲' : ' ▼') : '';
    const th = (f, label) => `<th data-sort="${f}">${label}${arrow(f)}</th>`;
    const rows = data.content.map(e => `
      <tr>
        <td>${esc(e.employeeId)}</td><td>${esc(e.fullName)}</td><td>${esc(e.email)}</td>
        <td>${esc(e.department)}</td><td>${esc(e.designation)}</td>
        <td><span class="badge ${esc(e.status)}">${esc(e.status)}</span></td>
        <td><div class="actions">
          <a class="btn btn-sm" href="#/view/${e.id}">View</a>
          <a class="btn btn-sm" href="#/edit/${e.id}">Edit</a>
          <button class="btn btn-sm btn-danger" data-del="${e.id}" data-name="${esc(e.fullName)}">Delete</button>
        </div></td>
      </tr>`).join('');
    area.innerHTML = data.content.length === 0 ? '<p class="empty">No employees found.</p>' : `
      <div class="table-wrap"><table>
        <thead><tr>${th('employeeId', 'ID')}${th('fullName', 'Name')}${th('email', 'Email')}${th('department', 'Department')}${th('designation', 'Designation')}${th('status', 'Status')}<th>Actions</th></tr></thead>
        <tbody>${rows}</tbody></table></div>
      <div class="pager">
        <span>Showing page ${data.page + 1} of ${data.totalPages} (${data.totalElements} employees)</span>
        <div>
          <select id="page-size">${[5, 10, 20, 50].map(s => `<option ${s === state.size ? 'selected' : ''}>${s}</option>`).join('')}</select>
          <button class="btn btn-sm" id="prev" ${data.page === 0 ? 'disabled' : ''}>Previous</button>
          <button class="btn btn-sm" id="next" ${data.page + 1 >= data.totalPages ? 'disabled' : ''}>Next</button>
        </div>
      </div>`;
    if (!data.content.length) return;
    area.querySelectorAll('th[data-sort]').forEach(h => h.addEventListener('click', () => {
      const f = h.dataset.sort;
      if (state.sortBy === f) state.direction = state.direction === 'asc' ? 'desc' : 'asc';
      else { state.sortBy = f; state.direction = 'asc'; }
      loadTable();
    }));
    area.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => confirmDelete(b.dataset.del, b.dataset.name)));
    document.getElementById('prev').addEventListener('click', () => { state.page--; loadTable(); });
    document.getElementById('next').addEventListener('click', () => { state.page++; loadTable(); });
    document.getElementById('page-size').addEventListener('change', e => { state.size = +e.target.value; state.page = 0; loadTable(); });
  } catch (err) {
    area.innerHTML = `<p class="empty">Could not load employees: ${esc(err.message)}</p>`;
  }
}

function confirmDelete(id, name) {
  const modal = document.getElementById('modal');
  document.getElementById('modal-text').textContent = `Are you sure you want to delete "${name}"? This cannot be undone.`;
  modal.classList.remove('hidden');
  const close = () => modal.classList.add('hidden');
  document.getElementById('modal-cancel').onclick = close;
  document.getElementById('modal-confirm').onclick = async () => {
    close();
    try { await api(`${API}/${id}`, { method: 'DELETE' }); toast('Employee deleted'); loadTable(); }
    catch (e) { toast(e.message, true); }
  };
}

/* ---------- form page (add / edit) ---------- */
const FIELDS = [
  ['fullName', 'Full Name', 'text'], ['email', 'Email Address', 'email'], ['phoneNumber', 'Phone Number', 'tel'],
  ['department', 'Department', 'text'], ['designation', 'Designation', 'text'],
  ['dateOfJoining', 'Date of Joining', 'date'], ['salary', 'Salary (INR)', 'number'],
];

async function renderForm(id) {
  let emp = { status: 'ACTIVE' };
  if (id) {
    try { emp = await api(`${API}/${id}`); } catch (e) { app.innerHTML = `<div class="card"><p class="empty">${esc(e.message)}</p></div>`; return; }
  }
  const inputs = FIELDS.map(([name, label, type]) => `
    <div class="field" id="f-${name}"><label for="${name}">${label} *</label>
      <input id="${name}" name="${name}" type="${type}" ${type === 'number' ? 'min="1" step="0.01"' : ''} value="${esc(emp[name])}">
      <div class="error"></div></div>`).join('');
  app.innerHTML = `
    <div class="card"><h2>${id ? 'Edit' : 'Add'} Employee ${id ? `<small style="color:#6b7280">(${esc(emp.employeeId)})</small>` : ''}</h2>
      <form id="emp-form" novalidate>
        <div class="form-grid">${inputs}
          <div class="field" id="f-status"><label for="status">Employment Status *</label>
            <select id="status" name="status">
              <option value="ACTIVE" ${emp.status === 'ACTIVE' ? 'selected' : ''}>Active</option>
              <option value="INACTIVE" ${emp.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option>
            </select><div class="error"></div></div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" id="save">${id ? 'Update' : 'Save'}</button>
          <a href="#/" class="btn">Cancel</a>
        </div>
      </form></div>`;

  document.getElementById('emp-form').addEventListener('submit', async ev => {
    ev.preventDefault();
    const body = {};
    FIELDS.forEach(([n]) => body[n] = document.getElementById(n).value.trim());
    body.status = document.getElementById('status').value;
    body.salary = body.salary === '' ? null : Number(body.salary);
    const errors = validate(body);
    showErrors(errors);
    if (Object.keys(errors).length) return;
    const btn = document.getElementById('save'); btn.disabled = true;
    try {
      await api(id ? `${API}/${id}` : API, { method: id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      toast(id ? 'Employee updated' : 'Employee added');
      location.hash = '#/';
    } catch (e) {
      if (e.fieldErrors) showErrors(e.fieldErrors);
      else if (e.status === 409) showErrors({ email: e.message });
      toast(e.message, true);
      btn.disabled = false;
    }
  });
}

function validate(b) {
  const e = {};
  if (!b.fullName) e.fullName = 'Full name is required';
  if (!b.email) e.email = 'Email is required';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(b.email)) e.email = 'Email format is invalid';
  if (!b.phoneNumber) e.phoneNumber = 'Phone number is required';
  else if (!/^[0-9+\-\s]{7,15}$/.test(b.phoneNumber)) e.phoneNumber = 'Phone must be 7-15 digits';
  if (!b.department) e.department = 'Department is required';
  if (!b.designation) e.designation = 'Designation is required';
  if (!b.dateOfJoining) e.dateOfJoining = 'Date of joining is required';
  if (b.salary === null || isNaN(b.salary) || b.salary <= 0) e.salary = 'Salary must be greater than 0';
  return e;
}

function showErrors(errors) {
  [...FIELDS.map(f => f[0]), 'status'].forEach(n => {
    const box = document.getElementById('f-' + n);
    if (!box) return;
    box.classList.toggle('invalid', !!errors[n]);
    box.querySelector('.error').textContent = errors[n] || '';
  });
}

/* ---------- details page ---------- */
async function renderDetails(id) {
  try {
    const e = await api(`${API}/${id}`);
    const item = (l, v) => `<div><dt>${l}</dt><dd>${v}</dd></div>`;
    app.innerHTML = `
      <div class="card"><h2>${esc(e.fullName)} <span class="badge ${esc(e.status)}">${esc(e.status)}</span></h2>
        <dl class="details">
          ${item('Employee ID', esc(e.employeeId))}${item('Email', esc(e.email))}
          ${item('Phone', esc(e.phoneNumber))}${item('Department', esc(e.department))}
          ${item('Designation', esc(e.designation))}${item('Date of Joining', fmtDate(e.dateOfJoining))}
          ${item('Salary', money(e.salary))}${item('Status', esc(e.status))}
        </dl>
        <div class="form-actions">
          <a class="btn btn-primary" href="#/edit/${e.id}">Edit</a>
          <button class="btn btn-danger" id="del">Delete</button>
          <a class="btn" href="#/">Back to list</a>
        </div></div>`;
    document.getElementById('del').addEventListener('click', () => {
      confirmDelete(e.id, e.fullName);
      // after deletion go back to list
      document.getElementById('modal-confirm').addEventListener('click', () => setTimeout(() => location.hash = '#/', 300));
    });
  } catch (err) {
    app.innerHTML = `<div class="card"><p class="empty">${esc(err.message)}</p><a href="#/" class="btn">Back</a></div>`;
  }
}

/* ---------- router ---------- */
function route() {
  const [, page, id] = (location.hash || '#/').split('/');
  if (page === 'add') renderForm();
  else if (page === 'edit' && id) renderForm(id);
  else if (page === 'view' && id) renderDetails(id);
  else renderList();
}
window.addEventListener('hashchange', route);
route();
