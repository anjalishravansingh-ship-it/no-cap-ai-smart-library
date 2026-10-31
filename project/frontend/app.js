// NoCapAI frontend logic
// Talks to the FastAPI backend under /api and renders each page into #content.

const $ = (id) => document.getElementById(id);

let role = "Librarian";
let userName = "Administrator";

// Small fetch wrapper: adds JSON headers and turns non-2xx responses into errors.
async function api(path, opts = {}) {
  const res = await fetch("/api" + path, {
    headers: { "Content-Type": "application/json", ...(opts.headers || {}) },
    ...opts,
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.detail || data.error || "Request failed");
  return data;
}

function toast(message) {
  $("toast").textContent = message;
  $("toast").style.display = "block";
  setTimeout(() => ($("toast").style.display = "none"), 2500);
}

// --- Login ---
document.querySelectorAll(".roles button").forEach((btn) => {
  btn.onclick = () => {
    document.querySelectorAll(".roles button").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    role = btn.dataset.role;
  };
});

$("loginBtn").onclick = async () => {
  try {
    const data = await api("/login", {
      method: "POST",
      body: JSON.stringify({ username: $("username").value, password: $("password").value, role }),
    });
    userName = data.name;
    $("login").hidden = true;
    $("login").style.display = "none";
    $("app").hidden = false;
    $("app").style.display = "flex";
    $("who").textContent = userName;
    $("avatar").textContent = userName[0];
    show("dashboard");
  } catch (e) {
    $("loginError").textContent = e.message;
  }
};

$("logout").onclick = () => location.reload();

document.querySelectorAll("nav button").forEach((btn) => {
  btn.onclick = () => show(btn.dataset.page);
});

// Sets the header title + eyebrow, and highlights the matching sidebar item.
// Uses the page key directly (data-page) instead of reconstructing it from the
// display title, since titles like "ML Prediction" / "Overview" don't map
// back to their page keys ("prediction" / "dashboard") through a simple
// lowercase + strip-spaces transform.
function setHeader(pageKey, displayTitle, eyebrow = "CONTROL CENTER") {
  $("title").textContent = displayTitle;
  $("eyebrow").textContent = eyebrow;
  document.querySelectorAll("nav button").forEach((b) => {
    b.classList.toggle("active", b.dataset.page === pageKey);
  });
}

async function show(page) {
  try {
    const pages = { dashboard, books, members, circulation, prediction, analytics, actions };
    if (pages[page]) await pages[page]();
  } catch (e) {
    toast(e.message);
  }
}

// --- Pages ---

async function dashboard() {
  setHeader("dashboard", "Overview");
  const d = await api("/dashboard");
  const stats = [
    ["Books", d.stats.books, "catalogue"],
    ["Members", d.stats.members, "registered"],
    ["Available", d.stats.available, "copies"],
    ["Issued", d.stats.issued, "active loans"],
  ];

  $("content").innerHTML = `
    <div class="hero">
      <div><h3>Library intelligence at a glance</h3><span class="muted">Operational metrics + machine learning readiness</span></div>
      <span class="model-ready">● MODEL ONLINE</span>
    </div>
    <div class="grid">
      ${stats.map(([label, value, note]) => `<div class="card stat"><span>${label}</span><strong>${value}</strong><em>${note}</em></div>`).join("")}
    </div>
    <div class="two">
      <div class="card panel chartbox"><h3>Catalogue by genre</h3><canvas id="genreChart"></canvas></div>
      <div class="card panel">
        <div class="model-header"><h3>Model performance</h3><span class="model-ready">READY</span></div>
        <div class="grid" style="grid-template-columns:1fr 1fr;margin-top:10px">
          <div class="stat"><span>Accuracy</span><strong style="font-size:24px">${(d.model.accuracy * 100).toFixed(1)}%</strong></div>
          <div class="stat"><span>ROC-AUC</span><strong style="font-size:24px">${d.model.roc_auc.toFixed(2)}</strong></div>
        </div>
        <div class="feature-note">Random Forest classifier trained on ${d.model.trained_on} synthetic loan records. Use anonymised real history for final evaluation.</div>
      </div>
    </div>
    <div class="card panel" style="margin-top:16px">
      <h3>Recent activity</h3>
      ${d.activity.map((x) => `<p style="font-size:12px"><span class="badge">${x.type}</span> <b>${x.book}</b> → ${x.member} <span class="muted">· ${x.date}</span></p>`).join("")}
    </div>`;

  new Chart($("genreChart"), {
    type: "doughnut",
    data: { labels: Object.keys(d.genres), datasets: [{ data: Object.values(d.genres) }] },
    options: { plugins: { legend: { position: "bottom" } }, maintainAspectRatio: false },
  });
}

function bookRow(x) {
  return `<tr><td><b>${x.id}</b></td><td>${x.title}</td><td>${x.author}</td><td>${x.genre}</td><td>${x.available}/${x.copies}</td><td><span class="badge ${x.available ? "good" : "bad"}">${x.available ? "Available" : "Waitlist"}</span></td></tr>`;
}

async function books() {
  setHeader("books", "Books", "CATALOGUE");
  const list = await api("/books");
  $("content").innerHTML = `
    <div class="hero">
      <div><h3>Book catalogue</h3><span class="muted">Search and monitor availability.</span></div>
      <input class="search" id="bookSearch" placeholder="Search title, author or genre...">
    </div>
    <div class="card tablewrap">
      <table class="table"><tr><th>ID</th><th>Title</th><th>Author</th><th>Genre</th><th>Copies</th><th>Status</th></tr>${list.map(bookRow).join("")}</table>
    </div>`;

  $("bookSearch").oninput = async (e) => {
    const results = await api("/books?search=" + encodeURIComponent(e.target.value));
    document.querySelector(".tablewrap").innerHTML =
      `<table class="table"><tr><th>ID</th><th>Title</th><th>Author</th><th>Genre</th><th>Copies</th><th>Status</th></tr>${results.map(bookRow).join("")}</table>`;
  };
}

async function members() {
  setHeader("members", "Members", "MEMBER MANAGEMENT");
  const list = await api("/members");
  $("content").innerHTML = `
    <div class="card tablewrap">
      <table class="table">
        <tr><th>Member</th><th>Type</th><th>Tenure</th><th>Previous loans</th><th>Late returns</th><th>Current loans</th></tr>
        ${list.map((x) => `<tr><td><b>${x.name}</b><div class="muted">${x.id} · ${x.email}</div></td><td><span class="badge">${x.type}</span></td><td>${x.tenure} mo</td><td>${x.previous_loans}</td><td>${x.late_returns}</td><td>${x.current_loans}</td></tr>`).join("")}
      </table>
    </div>`;
}

async function circulation() {
  setHeader("circulation", "Circulation", "OPERATIONS");
  const [bookList, memberList] = await Promise.all([api("/books"), api("/members")]);

  $("content").innerHTML = `
    <div class="two">
      <div class="card panel">
        <h3>Issue a book</h3>
        <label>Book<select id="ib">${bookList.map((x) => `<option value="${x.id}">${x.id} — ${x.title} (${x.available} available)</option>`).join("")}</select></label>
        <label>Member<select id="im">${memberList.map((x) => `<option value="${x.id}">${x.id} — ${x.name}</option>`).join("")}</select></label>
        <button class="primary" id="issueBtn">Issue book</button>
      </div>
      <div class="card panel">
        <h3>Return workflow</h3>
        <p class="muted">14-day standard loan period with FIFO waiting queue.</p>
        <div class="feature-note">This module is the operational layer. The ML layer can score overdue risk before issuing a book.</div>
      </div>
    </div>`;

  $("issueBtn").onclick = async () => {
    try {
      const result = await api("/issue", {
        method: "POST",
        body: JSON.stringify({ book_id: $("ib").value, member_id: $("im").value }),
      });
      toast(result.message);
      circulation(); // refresh dropdown counts
    } catch (e) {
      toast(e.message);
    }
  };
}

async function prediction() {
  setHeader("prediction", "ML Prediction", "MACHINE LEARNING");
  $("content").innerHTML = `
    <div class="hero"><div><h3>Overdue Risk Predictor</h3><span class="muted">Random Forest classification model</span></div></div>
    <div class="two">
      <div class="card panel">
        <div class="formgrid">
          <label>Member tenure (months)<input id="tenure" type="number" value="12"></label>
          <label>Previous loans<input id="loans" type="number" value="10"></label>
          <label>Previous late returns<input id="late" type="number" value="2"></label>
          <label>Current loans<input id="current" type="number" value="1"></label>
          <label>Book popularity (1–100)<input id="pop" type="number" value="60"></label>
          <label>Loan duration (days)<input id="days" type="number" value="14"></label>
          <label>Member type<select id="type"><option>Student</option><option>Faculty</option><option>Researcher</option></select></label>
          <label>Genre<select id="genre"><option>Technology</option><option>Science</option><option>Business</option><option>Literature</option><option>History</option><option>Arts</option></select></label>
        </div>
        <button class="primary" id="predictBtn">Run prediction ✦</button>
      </div>
      <div id="result" class="card panel"><div class="empty">Enter borrower and book features, then run the model.</div></div>
    </div>
    <div class="card panel" style="margin-top:16px">
      <h3>Why these features?</h3>
      <p class="muted">The model uses borrower history, current borrowing load, book popularity, loan duration and categorical context. This demonstrates the complete Data Science lifecycle: data → preprocessing → training → evaluation → inference.</p>
    </div>`;

  $("predictBtn").onclick = async () => {
    try {
      const d = await api("/predict", {
        method: "POST",
        body: JSON.stringify({
          member_tenure_months: +$("tenure").value,
          previous_loans: +$("loans").value,
          previous_late_returns: +$("late").value,
          current_loans: +$("current").value,
          book_popularity: +$("pop").value,
          loan_duration_days: +$("days").value,
          member_type: $("type").value,
          genre: $("genre").value,
        }),
      });
      $("result").innerHTML = `
        <div class="risk ${d.risk.toLowerCase()}">
          <span class="metric">PREDICTED OVERDUE RISK</span>
          <strong>${d.probability_percent}%</strong>
          <span class="badge">${d.risk} Risk</span>
        </div>
        <div class="panel" style="padding:14px 0 0"><b>Recommendation</b><p class="muted">${d.recommendation}</p></div>`;
    } catch (e) {
      toast(e.message);
    }
  };
}

async function analytics() {
  setHeader("analytics", "Analytics", "MODEL INSIGHTS");
  const [m, comparison] = await Promise.all([api("/model"), api("/model/comparison")]);

  $("content").innerHTML = `
    <div class="grid">
      <div class="card stat"><span>Final Model</span><strong style="font-size:20px">Random Forest</strong><em>300 trees</em></div>
      <div class="card stat"><span>Training rows</span><strong>${m.train_rows || m.trained_on}</strong><em>80% training split</em></div>
      <div class="card stat"><span>Test Accuracy</span><strong>${(m.accuracy * 100).toFixed(1)}%</strong><em>held-out test set</em></div>
      <div class="card stat"><span>ROC-AUC</span><strong>${m.roc_auc.toFixed(2)}</strong><em>discrimination score</em></div>
    </div>
    <div class="two">
      <div class="card panel">
        <h3>Model comparison</h3>
        <div class="tablewrap">
          <table>
            <thead><tr><th>Model</th><th>Accuracy</th><th>F1</th><th>ROC-AUC</th></tr></thead>
            <tbody>${comparison.map((x) => `<tr><td><b>${x.model}</b></td><td>${(x.accuracy * 100).toFixed(1)}%</td><td>${x.f1.toFixed(2)}</td><td>${x.roc_auc.toFixed(2)}</td></tr>`).join("")}</tbody>
          </table>
        </div>
      </div>
      <div class="card panel">
        <h3>Data Science pipeline</h3>
        <ol class="muted" style="line-height:2.0">
          <li>Collect / generate anonymised library history</li>
          <li>Validate missing values and data types</li>
          <li>Split numeric and categorical variables</li>
          <li>Standardise numeric features</li>
          <li>One-hot encode categorical features</li>
          <li>Compare four classification algorithms</li>
          <li>Evaluate on unseen test data + 5-fold CV</li>
          <li>Save final Random Forest with joblib</li>
          <li>Serve predictions through FastAPI</li>
        </ol>
      </div>
    </div>
    <div class="two" style="margin-top:16px">
      <div class="card panel">
        <h3>Research interpretation</h3>
        <p class="muted">The final model is selected as Random Forest for its non-linear modelling capability, robustness on mixed features and explainable feature importance. The model is an academic prototype and should not be used for punitive decisions.</p>
      </div>
      <div class="card panel">
        <h3>Dataset limitation</h3>
        <p class="muted">The included 1,600 records are synthetic and reproducible. Replace them with anonymised historical transactions for a real empirical study. Report class balance, leakage checks, cross-validation and fairness analysis.</p>
      </div>
    </div>`;
}

async function actions() {
  setHeader("actions", "Activity", "AUDIT TRAIL");
  const list = await api("/actions");
  $("content").innerHTML = `
    <div class="card panel">
      <h3>Recent system activity</h3>
      ${list.map((x) => `<div class="activity-row"><span class="badge">${x.type}</span><b>${x.book}</b><span class="muted">→ ${x.member} · ${x.date}</span><span class="badge">${x.status}</span></div>`).join("")}
    </div>`;
}
