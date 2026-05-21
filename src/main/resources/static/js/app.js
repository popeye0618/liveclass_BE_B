(function () {
  "use strict";

  /* ──────────────────────── Helpers ──────────────────────── */

  const moneyFmt = new Intl.NumberFormat("ko-KR");
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  function formatMoney(n) {
    if (n === null || n === undefined || isNaN(n)) return "-";
    return moneyFmt.format(n) + " 원";
  }

  function formatJson(obj) {
    try {
      return JSON.stringify(obj, null, 2);
    } catch (e) {
      return String(obj);
    }
  }

  function escapeHtml(value) {
    if (value === null || value === undefined) return "";
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function getSection(feature) {
    return document.querySelector(`[data-feature="${feature}"]`);
  }

  function setStatus(section, kind, text) {
    const badge = $('[data-role="status"]', section);
    badge.className = "status-badge";
    if (kind) badge.classList.add("status-badge--" + kind);
    badge.textContent = text;
  }

  function setRequestInfo(section, method, url) {
    const el = $('[data-role="request"]', section);
    el.textContent = `${method} ${url}`;
  }

  function setSummary(section, html) {
    const el = $('[data-role="summary"]', section);
    el.innerHTML = html || "";
  }

  function setJson(section, obj) {
    const pre = $('[data-role="json"]', section);
    pre.textContent = typeof obj === "string" ? obj : formatJson(obj);
  }

  function renderError(section, error) {
    const code = error && error.code ? error.code : "UNKNOWN_ERROR";
    const message = error && error.message ? error.message : "알 수 없는 오류가 발생했습니다.";
    const fieldErrors = (error && error.fieldErrors) || [];
    const fieldHtml = fieldErrors.length
      ? `<ul class="error-block__fields">${fieldErrors
          .map(
            (fe) =>
              `<li><strong>${escapeHtml(fe.field)}</strong>: ${escapeHtml(
                fe.reason
              )} (입력값: ${escapeHtml(fe.rejectedValue)})</li>`
          )
          .join("")}</ul>`
      : "";
    setSummary(
      section,
      `<div class="error-block">
        <span class="error-block__code">${escapeHtml(code)}</span>
        <p class="error-block__msg">${escapeHtml(message)}</p>
        ${fieldHtml}
      </div>`
    );
  }

  /* ──────────────────────── Core fetch wrapper ──────────────────────── */

  async function apiRequest(method, url, body) {
    const opts = { method, headers: {} };
    if (body !== undefined) {
      opts.headers["Content-Type"] = "application/json";
      opts.body = JSON.stringify(body);
    }
    let response;
    try {
      response = await fetch(url, opts);
    } catch (e) {
      return {
        ok: false,
        status: 0,
        payload: null,
        error: { code: "NETWORK_ERROR", message: e.message || "네트워크 오류" },
      };
    }

    let payload = null;
    try {
      payload = await response.json();
    } catch (e) {
      payload = null;
    }

    if (!response.ok || (payload && payload.success === false)) {
      const error =
        (payload && payload.error) || {
          code: "HTTP_" + response.status,
          message: response.statusText || "요청 실패",
        };
      return { ok: false, status: response.status, payload, error };
    }

    return { ok: true, status: response.status, payload, error: null };
  }

  /* ──────────────────────── Common: handle and render ──────────────────────── */

  async function runAndRender(section, method, url, options) {
    options = options || {};
    setStatus(section, "loading", "요청 중…");
    setRequestInfo(section, method, url);

    const result = await apiRequest(method, url, options.body);

    if (result.ok) {
      setStatus(section, "success", `성공 (${result.status})`);
      const data = result.payload && result.payload.data;
      if (typeof options.onSuccess === "function") {
        options.onSuccess(data, result.payload);
      } else {
        setSummary(section, "");
      }
      setJson(section, result.payload);
    } else {
      setStatus(section, "error", `실패 (${result.status || "NET"})`);
      renderError(section, result.error);
      setJson(section, result.payload || result.error);
    }
    return result;
  }

  /* ──────────────────────── Renderers ──────────────────────── */

  function renderMoneyCards(items) {
    return `<div class="summary-cards">${items
      .map(
        (it) =>
          `<div class="summary-card${it.accent ? " summary-card--accent" : ""}">
            <span class="summary-card__label">${escapeHtml(it.label)}</span>
            <span class="summary-card__value">${escapeHtml(it.value)}</span>
          </div>`
      )
      .join("")}</div>`;
  }

  function renderStatusPill(status) {
    const cls = `status-pill status-pill--${status || ""}`;
    return `<span class="${cls}">${escapeHtml(status || "-")}</span>`;
  }

  /* ──────────────────────── ① Monthly Settlement ──────────────────────── */

  async function loadMonthlySettlement() {
    const section = getSection("monthly-settlement");
    const creatorId = $("#m-creatorId").value.trim();
    const date = $("#m-date").value.trim();
    if (!creatorId || !date) {
      setStatus(section, "error", "입력 누락");
      renderError(section, { code: "INPUT_REQUIRED", message: "creatorId, date를 모두 입력하세요." });
      return;
    }
    const url = `/api/v1/settlements/creators/${encodeURIComponent(
      creatorId
    )}/monthly?date=${encodeURIComponent(date)}`;

    await runAndRender(section, "GET", url, {
      onSuccess: (data) => {
        if (!data) {
          setSummary(section, "");
          return;
        }
        setSummary(
          section,
          renderMoneyCards([
            { label: "정산 금액", value: formatMoney(data.settlementAmount), accent: true },
            { label: "총 매출액", value: formatMoney(data.totalSalesAmount) },
            { label: "환불 합계", value: formatMoney(data.totalRefundAmount) },
            { label: "순매출액", value: formatMoney(data.netSalesAmount) },
            { label: "플랫폼 수수료", value: formatMoney(data.platformFeeAmount) },
            { label: "판매 건수", value: (data.salesCount ?? 0) + " 건" },
            { label: "취소 건수", value: (data.cancelCount ?? 0) + " 건" },
          ])
        );
      },
    });
  }

  /* ──────────────────────── ② Total Settlement (Admin) ──────────────────────── */

  async function loadTotalSettlement() {
    const section = getSection("total-settlement");
    const startDate = $("#t-startDate").value.trim();
    const endDate = $("#t-endDate").value.trim();
    if (!startDate || !endDate) {
      setStatus(section, "error", "입력 누락");
      renderError(section, { code: "INPUT_REQUIRED", message: "startDate, endDate를 모두 입력하세요." });
      return;
    }
    const url = `/api/v1/admin/settlements?startDate=${encodeURIComponent(
      startDate
    )}&endDate=${encodeURIComponent(endDate)}`;

    await runAndRender(section, "GET", url, {
      onSuccess: (data) => {
        if (!data) {
          setSummary(section, "");
          return;
        }
        const cards = renderMoneyCards([
          { label: "전체 정산 합계", value: formatMoney(data.totalSettlementAmount), accent: true },
          { label: "조회 시작일", value: data.startDate || "-" },
          { label: "조회 종료일", value: data.endDate || "-" },
          { label: "크리에이터 수", value: (data.creators?.length || 0) + " 명" },
        ]);
        const rows = (data.creators || [])
          .map(
            (c) => `<tr>
              <td>${escapeHtml(c.creatorId)}</td>
              <td>${escapeHtml(c.creatorName)}</td>
              <td class="num">${formatMoney(c.totalSalesAmount)}</td>
              <td class="num">${formatMoney(c.totalRefundAmount)}</td>
              <td class="num">${formatMoney(c.netSalesAmount)}</td>
              <td class="num">${formatMoney(c.platformFeeAmount)}</td>
              <td class="num">${formatMoney(c.settlementAmount)}</td>
              <td class="num">${c.salesCount ?? 0}</td>
              <td class="num">${c.cancelCount ?? 0}</td>
            </tr>`
          )
          .join("");
        const table = (data.creators || []).length
          ? `<div class="data-table-wrap"><table class="data-table">
              <thead><tr>
                <th>creatorId</th><th>name</th>
                <th class="num">매출</th><th class="num">환불</th><th class="num">순매출</th>
                <th class="num">수수료</th><th class="num">정산금액</th>
                <th class="num">판매</th><th class="num">취소</th>
              </tr></thead>
              <tbody>${rows}</tbody>
            </table></div>`
          : `<p style="color:var(--text-muted);font-size:13px;margin:8px 0 0;">조회 기간에 해당하는 크리에이터가 없습니다.</p>`;
        setSummary(section, cards + '<div style="margin-top:14px;">' + table + "</div>");
      },
    });
  }

  /* ──────────────────────── ③ Create Settlement ──────────────────────── */

  async function createSettlement() {
    const section = getSection("create-settlement");
    const creatorId = $("#c-creatorId").value.trim();
    const settlementMonth = $("#c-settlementMonth").value.trim();
    if (!creatorId || !settlementMonth) {
      setStatus(section, "error", "입력 누락");
      renderError(section, {
        code: "INPUT_REQUIRED",
        message: "creatorId, settlementMonth를 모두 입력하세요.",
      });
      return;
    }

    await runAndRender(section, "POST", "/api/v1/admin/settlements/monthly", {
      body: { creatorId, settlementMonth },
      onSuccess: (data) => {
        if (!data) {
          setSummary(section, "");
          return;
        }
        const cards = renderMoneyCards([
          { label: "settlement id", value: data.id || "-", accent: true },
          { label: "상태", value: data.status || "-" },
          { label: "정산 월", value: data.settlementMonth || "-" },
          { label: "정산 금액", value: formatMoney(data.settlementAmount) },
          { label: "순매출", value: formatMoney(data.netSalesAmount) },
          { label: "플랫폼 수수료", value: formatMoney(data.platformFeeAmount) },
        ]);
        const useIdBtn = data.id
          ? `<div class="inline-action">
              생성된 ID: <code>${escapeHtml(data.id)}</code>
              <button class="btn btn--ghost" data-action="copy-id" data-id="${escapeHtml(
                data.id
              )}">④번에 이 ID 사용</button>
            </div>`
          : "";
        setSummary(section, cards + useIdBtn);
      },
    });
  }

  /* ──────────────────────── ④ Change Status ──────────────────────── */

  async function changeStatus(action) {
    const section = getSection("change-status");
    const settlementId = $("#s-settlementId").value.trim();
    if (!settlementId) {
      setStatus(section, "error", "입력 누락");
      renderError(section, { code: "INPUT_REQUIRED", message: "settlementId를 입력하세요." });
      return;
    }
    const path = action === "pay" ? "pay" : "confirm";
    const url = `/api/v1/admin/settlements/${encodeURIComponent(settlementId)}/${path}`;

    await runAndRender(section, "PATCH", url, {
      onSuccess: (data) => {
        if (!data) {
          setSummary(section, "");
          return;
        }
        const statusPill = renderStatusPill(data.status);
        setSummary(
          section,
          `<div class="summary-cards">
            <div class="summary-card summary-card--accent">
              <span class="summary-card__label">변경된 상태</span>
              <span class="summary-card__value">${statusPill}</span>
            </div>
            <div class="summary-card">
              <span class="summary-card__label">settlement id</span>
              <span class="summary-card__value" style="font-size:13px;word-break:break-all;">${escapeHtml(
                data.id || "-"
              )}</span>
            </div>
            <div class="summary-card">
              <span class="summary-card__label">정산 월</span>
              <span class="summary-card__value">${escapeHtml(data.settlementMonth || "-")}</span>
            </div>
            <div class="summary-card">
              <span class="summary-card__label">정산 금액</span>
              <span class="summary-card__value">${formatMoney(data.settlementAmount)}</span>
            </div>
            <div class="summary-card">
              <span class="summary-card__label">확정 시각</span>
              <span class="summary-card__value" style="font-size:12px;">${escapeHtml(
                data.confirmedAt || "-"
              )}</span>
            </div>
            <div class="summary-card">
              <span class="summary-card__label">지급 시각</span>
              <span class="summary-card__value" style="font-size:12px;">${escapeHtml(
                data.paidAt || "-"
              )}</span>
            </div>
          </div>`
        );
      },
    });
  }

  /* ──────────────────────── ⑤ Excel Download ──────────────────────── */

  function parseContentDispositionFilename(header) {
    if (!header) return null;
    const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(header);
    if (utf8Match) {
      try {
        return decodeURIComponent(utf8Match[1].trim());
      } catch (e) {
        return utf8Match[1].trim();
      }
    }
    const plainMatch = /filename="?([^"';]+)"?/i.exec(header);
    if (plainMatch) return plainMatch[1].trim();
    return null;
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  async function downloadExcel() {
    const section = getSection("excel-download");
    const startMonth = $("#e-startMonth").value.trim();
    const endMonth = $("#e-endMonth").value.trim();
    if (!startMonth || !endMonth) {
      setStatus(section, "error", "입력 누락");
      renderError(section, { code: "INPUT_REQUIRED", message: "startMonth, endMonth를 모두 입력하세요." });
      return;
    }
    const url = `/api/v1/admin/settlements/excel?startMonth=${encodeURIComponent(
      startMonth
    )}&endMonth=${encodeURIComponent(endMonth)}`;

    setStatus(section, "loading", "다운로드 중…");
    setRequestInfo(section, "GET", url);

    let response;
    try {
      response = await fetch(url);
    } catch (e) {
      setStatus(section, "error", "네트워크 오류");
      renderError(section, { code: "NETWORK_ERROR", message: e.message });
      setJson(section, { code: "NETWORK_ERROR", message: e.message });
      return;
    }

    if (!response.ok) {
      let errPayload = null;
      try {
        errPayload = await response.json();
      } catch (e) {
        /* ignore */
      }
      const error =
        (errPayload && errPayload.error) || {
          code: "HTTP_" + response.status,
          message: response.statusText || "다운로드 실패",
        };
      setStatus(section, "error", `실패 (${response.status})`);
      renderError(section, error);
      setJson(section, errPayload || error);
      return;
    }

    const blob = await response.blob();
    const cd = response.headers.get("Content-Disposition");
    const fallback = `settlements_${startMonth}_${endMonth}.xlsx`;
    const filename = parseContentDispositionFilename(cd) || fallback;

    downloadBlob(blob, filename);

    setStatus(section, "success", `성공 (${response.status})`);
    setSummary(
      section,
      renderMoneyCards([
        { label: "다운로드 파일명", value: filename, accent: true },
        { label: "Content-Type", value: response.headers.get("Content-Type") || "-" },
        { label: "파일 크기", value: blob.size.toLocaleString() + " bytes" },
      ])
    );
    setJson(section, {
      filename,
      size: blob.size,
      contentType: response.headers.get("Content-Type"),
      contentDisposition: cd,
    });
  }

  /* ──────────────────────── ⑥ Sale Records ──────────────────────── */

  async function loadSaleRecords() {
    const section = getSection("sale-records");
    const creatorId = $("#r-creatorId").value.trim();
    const startDate = $("#r-startDate").value.trim();
    const endDate = $("#r-endDate").value.trim();
    if (!creatorId) {
      setStatus(section, "error", "입력 누락");
      renderError(section, { code: "INPUT_REQUIRED", message: "creatorId를 입력하세요." });
      return;
    }
    const params = [];
    if (startDate) params.push(`startDate=${encodeURIComponent(startDate)}`);
    if (endDate) params.push(`endDate=${encodeURIComponent(endDate)}`);
    const qs = params.length ? "?" + params.join("&") : "";
    const url = `/api/v1/creators/${encodeURIComponent(creatorId)}/sale-records${qs}`;

    await runAndRender(section, "GET", url, {
      onSuccess: (data) => {
        const records = Array.isArray(data) ? data : [];
        const totalAmount = records.reduce((sum, r) => sum + (Number(r.amount) || 0), 0);
        const cards = renderMoneyCards([
          { label: "판매 건수", value: records.length + " 건", accent: true },
          { label: "총 결제금액", value: formatMoney(totalAmount) },
        ]);
        if (records.length === 0) {
          setSummary(
            section,
            cards + `<p style="color:var(--text-muted);font-size:13px;margin:10px 0 0;">판매 내역이 없습니다.</p>`
          );
          return;
        }
        const rows = records
          .map(
            (r) => `<tr>
              <td>${escapeHtml(r.id)}</td>
              <td>${escapeHtml(r.courseTitle)}</td>
              <td>${escapeHtml(r.courseId)}</td>
              <td>${escapeHtml(r.studentId)}</td>
              <td class="num">${formatMoney(r.amount)}</td>
              <td>${escapeHtml(r.paidAt)}</td>
            </tr>`
          )
          .join("");
        const table = `<div class="data-table-wrap"><table class="data-table">
            <thead><tr>
              <th>id</th><th>강의명</th><th>courseId</th><th>studentId</th>
              <th class="num">금액</th><th>결제 시각</th>
            </tr></thead>
            <tbody>${rows}</tbody>
          </table></div>`;
        setSummary(section, cards + '<div style="margin-top:14px;">' + table + "</div>");
      },
    });
  }

  /* ──────────────────────── Event delegation ──────────────────────── */

  const handlers = {
    "monthly-settlement": loadMonthlySettlement,
    "total-settlement": loadTotalSettlement,
    "create-settlement": createSettlement,
    "confirm-settlement": () => changeStatus("confirm"),
    "pay-settlement": () => changeStatus("pay"),
    "excel-download": downloadExcel,
    "sale-records": loadSaleRecords,
  };

  document.addEventListener("click", (e) => {
    const target = e.target.closest("[data-action]");
    if (!target) return;
    const action = target.dataset.action;

    if (action === "copy-id") {
      const id = target.dataset.id;
      if (id) {
        const input = $("#s-settlementId");
        input.value = id;
        input.focus();
        input.scrollIntoView({ behavior: "smooth", block: "center" });
      }
      return;
    }

    const fn = handlers[action];
    if (typeof fn === "function") {
      Promise.resolve()
        .then(fn)
        .catch((err) => {
          console.error("[" + action + "] error:", err);
        });
    }
  });
})();
