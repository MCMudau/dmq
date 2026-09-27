/**
 * DMQ Plumbers & Renovations — shared admin navigation.
 *
 * Renders a top bar on larger screens and a slim icon sidebar on small screens
 * (the switch is pure CSS, at 768px). Usage on any admin page:
 *
 *   <div id="adminNav"></div>
 *   <script src="adminNav.js"></script>
 *   <script>AdminNav.init({ active: 'previous-projects' });</script>
 *
 * `active` matches the `data-nav` value of the current page's link, so it can be
 * highlighted (omit it, or pass a value that matches nothing, for no highlight).
 */
(function (window, document) {
  'use strict';

  const NAV_ITEMS = [
    {
      key: 'previous-projects',
      label: 'Previous Projects',
      href: '/adminPreviousProjects.html',
      icon: '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z"/>'
    },
    {
      key: 'services',
      label: 'Services',
      href: '/adminServices.html',
      icon: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>'
},
    {
      key: 'settings',
      label: 'Settings',
      href: '/adminSettings.html',
      icon: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>'
    }
  ];

  const LOGOUT_ITEM = {
    key: 'logout',
    label: 'Logout',
    icon: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>'
  };

  const STYLE_ID = 'dmq-admin-nav-styles';

  function injectStyles() {
    if (document.getElementById(STYLE_ID)) return;
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = `
      :root {
        --dmq-navy: #002f87;
        --dmq-blue: #0a63c9;
        --dmq-yellow: #f9e526;
      }
      body.dmq-nav-padding { margin: 0; }

      /* ---- Top bar (>=768px) ---- */
      .dmq-topbar {
        display: none;
        position: fixed; top: 0; left: 0; right: 0; height: 64px; z-index: 40;
        background: var(--dmq-navy); border-bottom: 4px solid var(--dmq-yellow);
        align-items: center; justify-content: space-between; padding: 0 24px;
        font-family: "Segoe UI", system-ui, -apple-system, Roboto, Arial, sans-serif;
      }
      .dmq-topbar .dmq-brand { display: flex; flex-direction: column; line-height: 1.1; text-decoration: none; }
      .dmq-topbar .dmq-brand b { color: #fff; font-size: 16px; letter-spacing: .5px; }
      .dmq-topbar .dmq-brand span { color: #fff; opacity: .8; font-size: 10.5px; letter-spacing: 1.5px; }
      .dmq-topbar .dmq-links { display: flex; align-items: center; gap: 6px; }
      .dmq-topbar .dmq-link {
        display: flex; align-items: center; gap: 8px; color: #fff; text-decoration: none;
        font-size: 14px; font-weight: 600; padding: 9px 14px; border-radius: 8px; border: 0;
        background: transparent; cursor: pointer; font-family: inherit;
      }
      .dmq-topbar .dmq-link:hover { background: rgba(255,255,255,.12); }
      .dmq-topbar .dmq-link.active { background: rgba(249,229,38,.18); color: var(--dmq-yellow); }
      .dmq-topbar .dmq-link svg { width: 18px; height: 18px; flex: none; }
      .dmq-topbar .dmq-link.dmq-logout { color: #ffd7d2; }
      .dmq-topbar .dmq-link.dmq-logout:hover { background: rgba(198,40,40,.25); }

      @media (min-width: 768px) {
        .dmq-topbar { display: flex; }
        body.dmq-nav-padding { padding-top: 64px; }
      }

      /* ---- Icon sidebar (<768px) ---- */
      .dmq-sidebar {
        display: none;
        position: fixed; top: 0; left: 0; bottom: 0; width: 68px; z-index: 40;
        background: var(--dmq-navy); border-right: 4px solid var(--dmq-yellow);
        flex-direction: column; align-items: center; padding: 14px 0;
        font-family: "Segoe UI", system-ui, -apple-system, Roboto, Arial, sans-serif;
      }
      .dmq-sidebar .dmq-brand {
        width: 40px; height: 40px; border-radius: 10px; background: rgba(255,255,255,.12);
        display: grid; place-items: center; color: var(--dmq-yellow); font-weight: 800; font-size: 14px;
        text-decoration: none; margin-bottom: 18px; flex: none;
      }
      .dmq-sidebar .dmq-links { display: flex; flex-direction: column; gap: 6px; width: 100%; align-items: center; flex: 1; }
      .dmq-sidebar .dmq-link {
        display: flex; flex-direction: column; align-items: center; gap: 4px; width: 56px; padding: 8px 2px;
        color: #fff; text-decoration: none; border-radius: 10px; border: 0; background: transparent;
        cursor: pointer; font-family: inherit;
      }
      .dmq-sidebar .dmq-link span { font-size: 9.5px; font-weight: 600; text-align: center; line-height: 1.1; }
      .dmq-sidebar .dmq-link svg { width: 20px; height: 20px; }
      .dmq-sidebar .dmq-link:hover { background: rgba(255,255,255,.12); }
      .dmq-sidebar .dmq-link.active { background: rgba(249,229,38,.18); color: var(--dmq-yellow); }
      .dmq-sidebar .dmq-link.dmq-logout { color: #ffd7d2; margin-top: auto; }
      .dmq-sidebar .dmq-link.dmq-logout:hover { background: rgba(198,40,40,.25); }

      @media (max-width: 767px) {
        .dmq-sidebar { display: flex; }
        body.dmq-nav-padding { padding-left: 68px; }
      }

      /* ---- Logout confirmation modal ---- */
      .dmq-modal-overlay {
        position: fixed; inset: 0; z-index: 100; display: none; align-items: center; justify-content: center;
        padding: 16px; background: rgba(0, 20, 60, .45);
        -webkit-backdrop-filter: blur(6px); backdrop-filter: blur(6px);
        font-family: "Segoe UI", system-ui, -apple-system, Roboto, Arial, sans-serif;
      }
      .dmq-modal-overlay.open { display: flex; }
      .dmq-modal {
        width: 100%; max-width: 380px; background: #fff; padding: 24px;
        box-shadow: 0 20px 50px rgba(0, 20, 60, .35); animation: dmq-pop .15s ease-out;
      }
      @keyframes dmq-pop { from { transform: scale(.96); opacity: 0; } to { transform: scale(1); opacity: 1; } }
      .dmq-modal h2 { margin: 0 0 8px; font-size: 20px; color: var(--dmq-navy); }
      .dmq-modal p { margin: 0 0 22px; font-size: 14.5px; color: #5b6577; line-height: 1.5; }
      .dmq-modal-actions { display: flex; gap: 10px; justify-content: flex-end; }
      .dmq-modal-actions button {
        font: inherit; font-size: 14.5px; font-weight: 700; padding: 10px 18px; border: 0; cursor: pointer;
      }
      .dmq-modal-cancel { background: #eef3fb; color: var(--dmq-navy); }
      .dmq-modal-cancel:hover { background: #dde5f3; }
      .dmq-modal-confirm { background: #c62828; color: #fff; }
      .dmq-modal-confirm:hover { background: #a61f1f; }
      .dmq-modal-actions button:focus-visible { outline: 3px solid rgba(10, 99, 201, .4); outline-offset: 2px; }
    `;
    document.head.appendChild(style);
  }

  function iconSvg(pathMarkup) {
    return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" '
         + 'stroke-linecap="round" stroke-linejoin="round">' + pathMarkup + '</svg>';
  }

  function linkHtml(item, active, asButton) {
    const cls = 'dmq-link' + (item.key === active ? ' active' : '') + (item.key === 'logout' ? ' dmq-logout' : '');
    const inner = iconSvg(item.icon) + '<span>' + item.label + '</span>';
    if (asButton) {
      return '<button type="button" class="' + cls + '" data-nav="' + item.key + '">' + inner + '</button>';
    }
    return '<a class="' + cls + '" href="' + item.href + '" data-nav="' + item.key + '">' + inner + '</a>';
  }

  function buildBar(className, active, brandHtml) {
    const items = NAV_ITEMS.map((i) => linkHtml(i, active, false)).join('')
                + linkHtml(LOGOUT_ITEM, active, true);
    return '<nav class="' + className + '">' + brandHtml + '<div class="dmq-links">' + items + '</div></nav>';
  }

  function buildHtml(active) {
    const topbarBrand = '<a class="dmq-brand" href="/adminDashboard.html"><b>DMQ PLUMBERS</b><span>And Renovations</span></a>';
    const sidebarBrand = '<a class="dmq-brand" href="/adminDashboard.html" title="DMQ Plumbers &amp; Renovations">DMQ</a>';
    return buildBar('dmq-topbar', active, topbarBrand) + buildBar('dmq-sidebar', active, sidebarBrand);
  }

  /** Shows the logout modal; resolves true on "Log out", false on Cancel / Esc / backdrop click. */
  function confirmLogout() {
    let overlay = document.getElementById('dmqLogoutModal');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'dmqLogoutModal';
      overlay.className = 'dmq-modal-overlay';
      overlay.innerHTML =
          '<div class="dmq-modal" role="dialog" aria-modal="true" aria-labelledby="dmqLogoutTitle" aria-describedby="dmqLogoutMsg">'
        +   '<h2 id="dmqLogoutTitle">Log out?</h2>'
        +   '<p id="dmqLogoutMsg">Are you sure you want to log out of your admin account?</p>'
        +   '<div class="dmq-modal-actions">'
        +     '<button type="button" class="dmq-modal-cancel">Cancel</button>'
        +     '<button type="button" class="dmq-modal-confirm">Log out</button>'
        +   '</div>'
        + '</div>';
      document.body.appendChild(overlay);
    }

    const cancelBtn = overlay.querySelector('.dmq-modal-cancel');
    const confirmBtn = overlay.querySelector('.dmq-modal-confirm');
    const previousFocus = document.activeElement;

    return new Promise((resolve) => {
      function close(result) {
        overlay.classList.remove('open');
        cancelBtn.removeEventListener('click', onCancel);
        confirmBtn.removeEventListener('click', onConfirm);
        overlay.removeEventListener('click', onBackdrop);
        document.removeEventListener('keydown', onKey);
        if (previousFocus && previousFocus.focus) previousFocus.focus();
        resolve(result);
      }
      function onCancel() { close(false); }
      function onConfirm() { close(true); }
      function onBackdrop(e) { if (e.target === overlay) close(false); }
      function onKey(e) {
        if (e.key === 'Escape') close(false);
        if (e.key === 'Tab') {   // keep focus inside the modal
          e.preventDefault();
          (document.activeElement === cancelBtn ? confirmBtn : cancelBtn).focus();
        }
      }

      cancelBtn.addEventListener('click', onCancel);
      confirmBtn.addEventListener('click', onConfirm);
      overlay.addEventListener('click', onBackdrop);
      document.addEventListener('keydown', onKey);
      overlay.classList.add('open');
      cancelBtn.focus();   // safe default: Enter won't log out by accident
    });
  }

  async function handleLogout(target) {
    if (!(await confirmLogout())) return;
    try {
      await fetch('/api/admin/account/logout', { method: 'POST' });
    } catch (_) {
      // Best-effort: even if the request fails, still send them to the login page.
    }
    window.location.href = '/';
  }

  function wire(target) {
    target.querySelectorAll('[data-nav="logout"]').forEach((btn) => {
      btn.addEventListener('click', () => handleLogout(target));
    });
  }

  window.AdminNav = {
    /**
     * @param {Object} [options]
     * @param {string} [options.target='#adminNav'] CSS selector for the placeholder element.
     * @param {string} [options.active] data-nav key of the current page ('previous-projects' | 'settings').
     */
    init(options) {
      options = options || {};
      const target = document.querySelector(options.target || '#adminNav');
      if (!target) {
        console.warn('AdminNav.init: no element matched "' + (options.target || '#adminNav') + '"');
        return;
      }
      injectStyles();
      target.innerHTML = buildHtml(options.active || '');
      document.body.classList.add('dmq-nav-padding');
      wire(target);
    }
  };
})(window, document);
