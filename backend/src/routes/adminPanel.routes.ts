import { Router, Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";
import prisma from "../utils/prisma.js";
import { applyTokens } from "../utils/tokens.js";
import { TxReason } from "@prisma/client";

const router = Router();

// ── Auth helpers ──────────────────────────────────────────────────────────────

const ADMIN_PASSWORD = process.env["ADMIN_PASSWORD"] ?? "changeme";
const SESSION_SECRET = process.env["SESSION_SECRET"] ?? "session-secret";
const TOKEN_MAX_AGE  = 24 * 60 * 60; // 24h in seconds

// In-memory login rate-limiter (per IP)
const loginAttempts = new Map<string, { count: number; resetAt: number }>();

function checkLoginRateLimit(ip: string): { allowed: boolean; remaining: number } {
  const now = Date.now();
  const entry = loginAttempts.get(ip);
  if (!entry || entry.resetAt < now) {
    loginAttempts.set(ip, { count: 0, resetAt: now + 60 * 60 * 1000 });
    return { allowed: true, remaining: 5 };
  }
  if (entry.count >= 5) return { allowed: false, remaining: 0 };
  return { allowed: true, remaining: 5 - entry.count };
}

function recordFailedLogin(ip: string) {
  const entry = loginAttempts.get(ip);
  if (entry) entry.count++;
}

function clearLoginAttempts(ip: string) {
  loginAttempts.delete(ip);
}

function signAdminToken(): string {
  return jwt.sign({ admin: true }, SESSION_SECRET, { expiresIn: TOKEN_MAX_AGE });
}

function verifyAdminToken(token: string): boolean {
  try {
    const payload = jwt.verify(token, SESSION_SECRET) as { admin?: boolean };
    return payload.admin === true;
  } catch {
    return false;
  }
}

function requireAdminApi(req: Request, res: Response, next: NextFunction): void {
  const auth = req.headers["authorization"];
  const token = auth?.startsWith("Bearer ") ? auth.slice(7) : null;
  if (!token || !verifyAdminToken(token)) {
    res.status(401).json({ error: "Unauthorized" });
    return;
  }
  next();
}

// ── HTML helpers ──────────────────────────────────────────────────────────────

function layout(title: string, body: string): string {
  return /* html */`<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>${title} — لوحة مرتجع</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script>tailwind.config = { theme: { extend: { colors: { gold: '#C9A961' } } } }</script>
  <style>
    body { font-family: 'Segoe UI', Tahoma, sans-serif; }
    .btn { @apply px-4 py-2 rounded-lg font-semibold transition-colors; }
    .btn-gold { @apply bg-yellow-600 text-white hover:bg-yellow-700; }
    .btn-red  { @apply bg-red-600 text-white hover:bg-red-700; }
    .btn-gray { @apply bg-gray-200 text-gray-800 hover:bg-gray-300; }
  </style>
  <script>
    function getToken() { return localStorage.getItem('adminToken'); }
    function setToken(t) { localStorage.setItem('adminToken', t); }
    function clearToken() { localStorage.removeItem('adminToken'); }
    function authHeaders() { return { 'Authorization': 'Bearer ' + getToken(), 'Content-Type': 'application/json' }; }
    function logout() { clearToken(); location.href = '/admin/login'; }
    async function apiFetch(url, opts={}) {
      const r = await fetch(url, { headers: authHeaders(), ...opts });
      if (r.status === 401) { logout(); return null; }
      return r;
    }
  </script>
</head>
<body class="bg-gray-100 min-h-screen">
${body}
</body>
</html>`;
}

function navbar(active: string): string {
  const links = [
    { href: "/admin/dashboard", label: "الرئيسية", key: "dashboard" },
    { href: "/admin/users",     label: "المستخدمون", key: "users" },
    { href: "/admin/tokens",    label: "التوكنز",   key: "tokens" },
  ];
  const linksHtml = links.map(l =>
    `<a href="${l.href}" class="px-3 py-2 rounded-lg text-sm font-medium ${active === l.key ? "bg-yellow-600 text-white" : "text-gray-300 hover:text-white hover:bg-gray-700"}">${l.label}</a>`
  ).join("");
  return /* html */`
<nav class="bg-gray-900 shadow-lg">
  <div class="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
    <div class="flex items-center gap-2">
      <span class="text-yellow-500 text-xl font-bold">🏛 مرتجع Admin</span>
    </div>
    <div class="flex items-center gap-2">
      ${linksHtml}
      <button onclick="logout()" class="px-3 py-2 rounded-lg text-sm font-medium text-gray-300 hover:text-white hover:bg-red-700">خروج</button>
    </div>
  </div>
</nav>`;
}

// ── Login page ────────────────────────────────────────────────────────────────

router.get("/login", (_req, res) => {
  res.send(layout("تسجيل الدخول", /* html */`
<div class="min-h-screen flex items-center justify-center">
  <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">
    <div class="text-center mb-6">
      <div class="text-5xl mb-3">🏛</div>
      <h1 class="text-2xl font-bold text-gray-800">لوحة تحكم مرتجع</h1>
      <p class="text-gray-500 text-sm mt-1">للمسؤولين فقط</p>
    </div>
    <div id="error" class="hidden bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm"></div>
    <form id="loginForm" onsubmit="doLogin(event)">
      <label class="block text-sm font-medium text-gray-700 mb-1">كلمة المرور</label>
      <input type="password" id="password" required
        class="w-full border border-gray-300 rounded-lg px-4 py-2 mb-4 focus:outline-none focus:ring-2 focus:ring-yellow-500"
        placeholder="أدخل كلمة المرور"/>
      <button type="submit" id="submitBtn"
        class="w-full bg-yellow-600 hover:bg-yellow-700 text-white font-bold py-2 px-4 rounded-lg transition-colors">
        دخول
      </button>
    </form>
  </div>
</div>
<script>
  if (getToken()) location.href = '/admin/dashboard';
  async function doLogin(e) {
    e.preventDefault();
    const btn = document.getElementById('submitBtn');
    const errEl = document.getElementById('error');
    btn.disabled = true; btn.textContent = 'جار التحقق...';
    errEl.classList.add('hidden');
    try {
      const r = await fetch('/admin/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: document.getElementById('password').value }),
      });
      const data = await r.json();
      if (r.ok && data.token) { setToken(data.token); location.href = '/admin/dashboard'; }
      else { errEl.textContent = data.error || 'كلمة المرور غير صحيحة'; errEl.classList.remove('hidden'); }
    } catch { errEl.textContent = 'خطأ في الاتصال'; errEl.classList.remove('hidden'); }
    btn.disabled = false; btn.textContent = 'دخول';
  }
</script>`));
});

// ── Dashboard ─────────────────────────────────────────────────────────────────

router.get("/dashboard", (_req, res) => {
  res.send(layout("الرئيسية", /* html */`
${navbar("dashboard")}
<div class="max-w-7xl mx-auto px-4 py-8">
  <h1 class="text-2xl font-bold text-gray-800 mb-6">لوحة التحكم</h1>
  <div id="stats" class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
    <div class="col-span-4 text-center text-gray-400 py-8">جار التحميل...</div>
  </div>
  <div class="bg-white rounded-2xl shadow p-6">
    <h2 class="text-lg font-bold text-gray-700 mb-4">آخر المستخدمين المسجلين</h2>
    <div id="recentUsers" class="text-gray-400">جار التحميل...</div>
  </div>
</div>
<script>
  (async function() {
    const r = await apiFetch('/admin/api/stats');
    if (!r) return;
    const d = await r.json();
    const cards = [
      { label: 'إجمالي المستخدمين', value: d.totalUsers, icon: '👥', color: 'blue' },
      { label: 'إجمالي الإعلانات',  value: d.totalListings, icon: '📋', color: 'green' },
      { label: 'توكنز مكتسبة اليوم', value: d.tokensEarnedToday, icon: '🪙', color: 'yellow' },
      { label: 'مستخدمون جدد اليوم', value: d.newUsersToday, icon: '🆕', color: 'purple' },
    ];
    const colors = { blue:'bg-blue-50 border-blue-200 text-blue-700', green:'bg-green-50 border-green-200 text-green-700', yellow:'bg-yellow-50 border-yellow-200 text-yellow-700', purple:'bg-purple-50 border-purple-200 text-purple-700' };
    document.getElementById('stats').innerHTML = cards.map(c =>
      \`<div class="rounded-2xl border p-5 \${colors[c.color]}">
        <div class="text-3xl mb-2">\${c.icon}</div>
        <div class="text-3xl font-bold">\${c.value}</div>
        <div class="text-sm font-medium mt-1">\${c.label}</div>
      </div>\`
    ).join('');

    const r2 = await apiFetch('/admin/api/users?limit=5');
    if (!r2) return;
    const users = await r2.json();
    document.getElementById('recentUsers').innerHTML = \`
      <table class="w-full text-sm">
        <thead><tr class="text-gray-500 border-b">
          <th class="pb-2 text-right">الاسم</th><th class="pb-2 text-right">الهاتف</th>
          <th class="pb-2 text-right">النوع</th><th class="pb-2 text-right">التوكنز</th>
        </tr></thead>
        <tbody>\${users.users.map(u => \`
          <tr class="border-b hover:bg-gray-50 cursor-pointer" onclick="location.href='/admin/users/'+\${JSON.stringify(u.id)}">
            <td class="py-2">\${u.fullName || '—'}</td>
            <td class="py-2 font-mono">\${u.phoneNumber || '—'}</td>
            <td class="py-2">\${u.userType}</td>
            <td class="py-2 font-bold text-yellow-600">\${u.tokenBalance} 🪙</td>
          </tr>\`).join('')}
        </tbody>
      </table>\`;
  })();
</script>`));
});

// ── Users list ────────────────────────────────────────────────────────────────

router.get("/users", (_req, res) => {
  res.send(layout("المستخدمون", /* html */`
${navbar("users")}
<div class="max-w-7xl mx-auto px-4 py-8">
  <h1 class="text-2xl font-bold text-gray-800 mb-6">المستخدمون</h1>
  <div class="bg-white rounded-2xl shadow p-6">
    <div class="flex gap-3 mb-6">
      <input id="search" type="text" placeholder="ابحث برقم الهاتف أو الاسم..."
        class="flex-1 border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-yellow-500"
        oninput="debounceSearch()"/>
      <button onclick="loadUsers()" class="px-4 py-2 bg-yellow-600 text-white rounded-lg font-medium hover:bg-yellow-700">بحث</button>
    </div>
    <div id="usersTable">جار التحميل...</div>
  </div>
</div>
<script>
  let searchTimer;
  function debounceSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadUsers, 400); }
  async function loadUsers() {
    const q = document.getElementById('search').value.trim();
    const r = await apiFetch('/admin/api/users?q=' + encodeURIComponent(q) + '&limit=50');
    if (!r) return;
    const d = await r.json();
    if (!d.users.length) { document.getElementById('usersTable').innerHTML = '<p class="text-gray-400 text-center py-8">لا توجد نتائج</p>'; return; }
    const typeLabel = { OFFICER:'ضابط', BUYER:'مشتري', MEDICAL_EXEMPT:'معفي طبي', ADMIN:'مسؤول' };
    document.getElementById('usersTable').innerHTML = \`
      <div class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead><tr class="text-gray-500 border-b text-right">
          <th class="pb-2 px-2">الاسم</th><th class="pb-2 px-2">الهاتف</th>
          <th class="pb-2 px-2">النوع</th><th class="pb-2 px-2">التوكنز</th>
          <th class="pb-2 px-2">تاريخ التسجيل</th>
        </tr></thead>
        <tbody>\${d.users.map(u => \`
          <tr class="border-b hover:bg-gray-50 cursor-pointer" onclick="location.href='/admin/users/'+\${JSON.stringify(u.id)}">
            <td class="py-2 px-2 font-medium">\${u.fullName || '—'}</td>
            <td class="py-2 px-2 font-mono text-xs">\${u.phoneNumber || '—'}</td>
            <td class="py-2 px-2"><span class="px-2 py-1 rounded-full text-xs bg-blue-100 text-blue-700">\${typeLabel[u.userType] || u.userType}</span></td>
            <td class="py-2 px-2 font-bold text-yellow-600">\${u.tokenBalance} 🪙</td>
            <td class="py-2 px-2 text-gray-400 text-xs">\${new Date(u.createdAt).toLocaleDateString('ar')}</td>
          </tr>\`).join('')}
        </tbody>
      </table></div>\`;
  }
  loadUsers();
</script>`));
});

// ── Tokens management (alias for users with add-tokens focus) ─────────────────

router.get("/tokens", (_req, res) => {
  res.redirect("/admin/users");
});

// ── User detail ───────────────────────────────────────────────────────────────

router.get("/users/:id", (req, res) => {
  const userId = req.params["id"];
  res.send(layout("تفاصيل المستخدم", /* html */`
${navbar("users")}
<div class="max-w-4xl mx-auto px-4 py-8">
  <a href="/admin/users" class="text-yellow-600 hover:underline text-sm mb-4 inline-block">← العودة للقائمة</a>
  <div id="content">جار التحميل...</div>
</div>
<script>
  const userId = ${JSON.stringify(userId)};
  const txReasonLabels = {
    WELCOME:'ترحيب', AD_WATCH:'مشاهدة إعلان', LISTING_SHARE:'مشاركة إعلان',
    REFERRAL:'إحالة', POST_LISTING:'نشر إعلان', POST_EXEMPTION:'نشر إعفاء',
    REVEAL_CONTACT:'كشف هاتف', START_CONVERSATION:'بدء محادثة',
    BOOST_LISTING:'تمييز إعلان', RENEW_LISTING:'تجديد إعلان',
    PURCHASE:'شراء', REGISTER_CLEARANCE_AGENT:'تسجيل مخلص',
    CLEARANCE_OFFER:'عرض تخليص', CLEARANCE_SELECT:'اختيار مخلص',
    CLEARANCE_RATE_BONUS:'مكافأة تقييم',
  };
  async function load() {
    const r = await apiFetch('/admin/api/users/' + userId);
    if (!r) return;
    const u = await r.json();
    document.getElementById('content').innerHTML = \`
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div class="bg-white rounded-2xl shadow p-5 md:col-span-2">
          <h2 class="text-lg font-bold text-gray-800 mb-3">\${u.fullName || 'بدون اسم'}</h2>
          <div class="grid grid-cols-2 gap-2 text-sm">
            <div><span class="text-gray-500">الهاتف:</span> <span class="font-mono">\${u.phoneNumber || '—'}</span></div>
            <div><span class="text-gray-500">البريد:</span> <span>\${u.email || '—'}</span></div>
            <div><span class="text-gray-500">النوع:</span> <span>\${u.userType}</span></div>
            <div><span class="text-gray-500">المحافظة:</span> <span>\${u.governorate || '—'}</span></div>
            <div><span class="text-gray-500">محظور:</span> <span class="\${u.isBlocked?'text-red-600':'text-green-600'}">\${u.isBlocked?'نعم':'لا'}</span></div>
            <div><span class="text-gray-500">التسجيل:</span> <span>\${new Date(u.createdAt).toLocaleDateString('ar')}</span></div>
          </div>
        </div>
        <div class="bg-yellow-50 border border-yellow-200 rounded-2xl p-5 text-center">
          <div class="text-yellow-600 text-4xl font-bold">\${u.tokenBalance}</div>
          <div class="text-yellow-700 font-medium mt-1">🪙 رصيد التوكنز</div>
          <div class="text-xs text-gray-500 mt-2">مكتسب: \${u.totalTokensEarned} | منفق: \${u.totalTokensSpent}</div>
        </div>
      </div>

      <div class="bg-white rounded-2xl shadow p-5 mb-6">
        <h3 class="text-md font-bold text-gray-800 mb-4">➕ إضافة توكنز</h3>
        <div id="addResult" class="hidden mb-3 p-3 rounded-lg text-sm"></div>
        <div class="flex gap-3 flex-wrap">
          <input type="number" id="amount" placeholder="الكمية" min="1" max="10000"
            class="border border-gray-300 rounded-lg px-3 py-2 w-28 focus:outline-none focus:ring-2 focus:ring-yellow-500"/>
          <select id="reason" class="border border-gray-300 rounded-lg px-3 py-2 flex-1 focus:outline-none focus:ring-2 focus:ring-yellow-500">
            <option value="200">شراء حزمة 200 توكن</option>
            <option value="600">شراء حزمة 600 توكن</option>
            <option value="1500">شراء حزمة 1500 توكن</option>
            <option value="custom">مكافأة يدوية</option>
          </select>
          <button onclick="addTokens()"
            class="px-5 py-2 bg-yellow-600 text-white rounded-lg font-bold hover:bg-yellow-700 transition-colors">
            أضف التوكنز
          </button>
        </div>
      </div>

      <div class="bg-white rounded-2xl shadow p-5">
        <h3 class="text-md font-bold text-gray-800 mb-4">سجل المعاملات (آخر 30)</h3>
        <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="text-gray-500 border-b text-right">
            <th class="pb-2 px-2">السبب</th><th class="pb-2 px-2">المبلغ</th>
            <th class="pb-2 px-2">الرصيد بعد</th><th class="pb-2 px-2">التاريخ</th>
          </tr></thead>
          <tbody>\${u.transactions.map(t => \`
            <tr class="border-b">
              <td class="py-2 px-2">\${txReasonLabels[t.reason] || t.reason}</td>
              <td class="py-2 px-2 font-bold \${t.amount > 0 ? 'text-green-600' : 'text-red-600'}">\${t.amount > 0 ? '+' : ''}\${t.amount}</td>
              <td class="py-2 px-2">\${t.balanceAfter}</td>
              <td class="py-2 px-2 text-gray-400 text-xs">\${new Date(t.createdAt).toLocaleString('ar')}</td>
            </tr>\`).join('')}
          </tbody>
        </table></div>
      </div>\`;
  }

  async function addTokens() {
    const amountInput = document.getElementById('amount').value;
    const reasonSel   = document.getElementById('reason').value;
    const amount = parseInt(amountInput || (reasonSel === 'custom' ? '0' : reasonSel));
    if (!amount || amount < 1) { alert('أدخل كمية صحيحة'); return; }
    const reasonLabels = { '200': 'شراء حزمة 200 توكن', '600': 'شراء حزمة 600 توكن', '1500': 'شراء حزمة 1500 توكن', 'custom': 'مكافأة يدوية' };
    const r = await apiFetch('/admin/api/users/' + userId + '/add-tokens', {
      method: 'POST',
      body: JSON.stringify({ amount, reason: reasonLabels[reasonSel] || reasonSel }),
    });
    if (!r) return;
    const d = await r.json();
    const el = document.getElementById('addResult');
    el.classList.remove('hidden');
    if (r.ok) {
      el.className = 'mb-3 p-3 rounded-lg text-sm bg-green-50 border border-green-200 text-green-700';
      el.textContent = 'تمت الإضافة! الرصيد الجديد: ' + d.newBalance + ' توكن 🎉';
      setTimeout(load, 1000);
    } else {
      el.className = 'mb-3 p-3 rounded-lg text-sm bg-red-50 border border-red-200 text-red-700';
      el.textContent = d.error || 'فشلت العملية';
    }
  }

  load();
</script>`));
});

// ── Root redirect ─────────────────────────────────────────────────────────────

router.get("/", (_req, res) => res.redirect("/admin/login"));

// ── JSON APIs ─────────────────────────────────────────────────────────────────

router.post("/api/login", (req: Request, res: Response): void => {
  const ip = (req.ip ?? req.socket.remoteAddress ?? "unknown").replace("::ffff:", "");
  const { password } = req.body as { password?: string };

  const { allowed } = checkLoginRateLimit(ip);
  if (!allowed) {
    res.status(429).json({ error: "تم تجاوز الحد الأقصى لمحاولات الدخول. حاول بعد ساعة." });
    return;
  }

  if (password !== ADMIN_PASSWORD) {
    recordFailedLogin(ip);
    res.status(401).json({ error: "كلمة المرور غير صحيحة" });
    return;
  }

  clearLoginAttempts(ip);
  const token = signAdminToken();
  res.json({ token });
});

router.get("/api/stats", requireAdminApi, async (_req, res): Promise<void> => {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);

  const [totalUsers, totalListings, newUsersToday, tokensEarnedToday] = await Promise.all([
    prisma.user.count(),
    prisma.listing.count(),
    prisma.user.count({ where: { createdAt: { gte: startOfDay } } }),
    prisma.tokenTransaction.aggregate({
      where: { createdAt: { gte: startOfDay }, amount: { gt: 0 } },
      _sum: { amount: true },
    }),
  ]);

  res.json({
    totalUsers,
    totalListings,
    newUsersToday,
    tokensEarnedToday: tokensEarnedToday._sum.amount ?? 0,
  });
});

router.get("/api/users", requireAdminApi, async (req, res): Promise<void> => {
  const q = String(req.query["q"] ?? "");
  const limit = String(req.query["limit"] ?? "20");
  const take = Math.min(parseInt(limit), 100);

  const users = await prisma.user.findMany({
    where: q ? {
      OR: [
        { phoneNumber: { contains: q } },
        { fullName: { contains: q, mode: "insensitive" } },
        { email: { contains: q, mode: "insensitive" } },
      ],
    } : undefined,
    select: {
      id: true, fullName: true, phoneNumber: true, email: true,
      userType: true, tokenBalance: true, createdAt: true, isBlocked: true,
    },
    orderBy: { createdAt: "desc" },
    take,
  });

  res.json({ users, total: users.length });
});

router.get("/api/users/:id", requireAdminApi, async (req, res): Promise<void> => {
  const userId = String(req.params["id"]);
  const [user, txs] = await Promise.all([
    prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true, fullName: true, phoneNumber: true, email: true,
        userType: true, tokenBalance: true, totalTokensEarned: true,
        totalTokensSpent: true, governorate: true, isBlocked: true,
        createdAt: true,
      },
    }),
    prisma.tokenTransaction.findMany({
      where: { userId },
      orderBy: { createdAt: "desc" },
      take: 30,
      select: { id: true, amount: true, reason: true, balanceAfter: true, createdAt: true },
    }),
  ]);

  if (!user) { res.status(404).json({ error: "المستخدم غير موجود" }); return; }

  res.json({ ...user, transactions: txs });
});

router.post("/api/users/:id/add-tokens", requireAdminApi, async (req, res): Promise<void> => {
  const { amount, reason } = req.body as { amount?: number; reason?: string };

  if (!amount || typeof amount !== "number" || amount < 1 || amount > 10000) {
    res.status(400).json({ error: "amount يجب أن يكون بين 1 و10000" });
    return;
  }

  const user = await prisma.user.findUnique({ where: { id: String(req.params["id"]) }, select: { id: true } });
  if (!user) { res.status(404).json({ error: "المستخدم غير موجود" }); return; }

  const { balanceAfter } = await applyTokens(
    user.id,
    TxReason.PURCHASE,
    undefined,
    amount,
  );

  // Optionally store the custom reason in notes (we'll just log it)
  console.log(`[ADMIN] Added ${amount} tokens to ${user.id}. Reason: ${reason ?? "manual"}. New balance: ${balanceAfter}`);

  res.json({ success: true, newBalance: balanceAfter, userId: user.id, amount });
});

export default router;
