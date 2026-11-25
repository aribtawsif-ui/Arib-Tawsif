(function () {
  const STORAGE_KEY = "porey_website_state_v1";
  const hasBridge = () => typeof window.PoreyBridge !== "undefined" && window.PoreyBridge !== null;

  const IMAGE_MAP = {
    panjabi: "images/img_panjabi_artisan.jpg",
    saree: "images/img_saree_atelier.jpg",
    streetwear: "images/img_streetwear_dhaka.jpg",
    hero: "images/img_hero_dhaka.jpg"
  };

  function createDefaultState() {
    return {
      isDarkMode: false,
      activeRole: "GUEST",
      isGuest: true,
      activeUser: null,
      promoCode: "",
      selectedZoneId: "zone_dhaka_express",
      zones: [
        { id: "zone_dhaka_express", name: "Dhaka Metro Express", feeBdt: 60, window: "Same-Day (2–4 hrs)" },
        { id: "zone_uttara_mirpur", name: "Uttara & Mirpur Hub", feeBdt: 70, window: "Same-Day (3–6 hrs)" },
        { id: "zone_savar_naryanganj", name: "Savar & Narayanganj", feeBdt: 110, window: "Next-Day (18–24 hrs)" }
      ],
      users: [
        { id: "usr_cust_1", name: "Nafisa Rahman", email: "nafisa.dhaka@example.bd", phone: "+880 1711-440219", role: "CUSTOMER", password: "porey123", address: "House 24, Road 11, Banani, Dhaka" },
        { id: "usr_merch_1", name: "Arif Mahmud (Banani POS)", email: "banani.pos@example.bd", phone: "+880 1811-552100", role: "PHYSICAL_MERCHANT", merchantId: "mer_banani_pos", password: "porey123", address: "Road 11, Banani, Dhaka" },
        { id: "usr_admin_1", name: "Tariqul Islam (Admin)", email: "tariqul.admin@porey.bd", phone: "+880 1911-880011", role: "ADMIN", password: "porey123", address: "Porey HQ, Gulshan 2, Dhaka" }
      ],
      merchants: [
        { id: "mer_banani_pos", name: "Aarong Heritage Studio", area: "Banani, Dhaka", storeType: "PHYSICAL_POS", isVerified: true, isSuspended: false, rating: 4.9, tradeLicenseRef: "TRAD/DNCC/2024/1102", ownerUserId: "usr_merch_1" },
        { id: "mer_dhanmondi_handloom", name: "Dhaka Looms & Co.", area: "Dhanmondi, Dhaka", storeType: "PHYSICAL_MANUAL", isVerified: true, isSuspended: false, rating: 4.7, tradeLicenseRef: "TRAD/DSCC/2024/4421", ownerUserId: "usr_merch_2" },
        { id: "mer_porey_online", name: "Porey Atelier Online", area: "Gulshan Hub, Dhaka", storeType: "ONLINE_ONLY", isVerified: true, isSuspended: false, rating: 4.8, tradeLicenseRef: "DBID/MOI/2025/0921", ownerUserId: "usr_merch_3" },
        { id: "mer_uttara_street", name: "Uttara Streetwear Hub", area: "Uttara Sec 7, Dhaka", storeType: "PHYSICAL_MANUAL", isVerified: false, isSuspended: false, rating: 4.5, tradeLicenseRef: "PENDING-KYC-2026-07", ownerUserId: "usr_merch_4" }
      ],
      products: [
        {
          id: "prod_jamdani_panjabi",
          name: "Artisan Jamdani Weave Panjabi",
          brand: "Dhaka Looms",
          category: "Panjabi",
          fabric: "100% Handloom Cotton",
          rating: 4.9,
          imageKey: "panjabi",
          isNewArrival: true,
          offers: [
            { listingId: "lst_panjabi_banani", merchantId: "mer_banani_pos", size: "L", color: "Ivory Cream", priceBdt: 3450, discountPriceBdt: 2990, availableQty: 14 },
            { listingId: "lst_panjabi_dhanmondi", merchantId: "mer_dhanmondi_handloom", size: "L", color: "Ivory Cream", priceBdt: 3250, discountPriceBdt: null, availableQty: 7 }
          ]
        },
        {
          id: "prod_muslin_saree",
          name: "Dhakai Muslin Silk Saree",
          brand: "Aarong Studio",
          category: "Saree",
          fabric: "Pure Half-Silk & Zari",
          rating: 4.9,
          imageKey: "saree",
          isNewArrival: false,
          offers: [
            { listingId: "lst_saree_banani", merchantId: "mer_banani_pos", size: "Free Size", color: "Terracotta Coral", priceBdt: 6800, discountPriceBdt: 5950, availableQty: 6 }
          ]
        },
        {
          id: "prod_dhaka_oversized_tee",
          name: "Dhaka '71 Graphic Oversized Tee",
          brand: "Porey Street",
          category: "Streetwear",
          fabric: "240 GSM Combed Jersey",
          rating: 4.7,
          imageKey: "streetwear",
          isNewArrival: true,
          offers: [
            { listingId: "lst_tee_online", merchantId: "mer_porey_online", size: "M", color: "Forest Slate", priceBdt: 1450, discountPriceBdt: 1190, availableQty: 22 },
            { listingId: "lst_tee_uttara", merchantId: "mer_uttara_street", size: "M", color: "Forest Slate", priceBdt: 1350, discountPriceBdt: null, availableQty: 9 }
          ]
        },
        {
          id: "prod_linen_kurti",
          name: "Sage Botanical Linen Kurti",
          brand: "Porey Atelier",
          category: "Saree",
          fabric: "Breathable Flax Linen",
          rating: 4.8,
          imageKey: "hero",
          isNewArrival: true,
          offers: [
            { listingId: "lst_kurti_online", merchantId: "mer_porey_online", size: "M", color: "Sage Olive", priceBdt: 2650, discountPriceBdt: 2290, availableQty: 11 }
          ]
        },
        {
          id: "prod_khadi_waistcoat",
          name: "Comilla Khadi Heritage Waistcoat",
          brand: "Dhaka Looms",
          category: "Panjabi",
          fabric: "Hand-spun Comilla Khadi",
          rating: 4.8,
          imageKey: "panjabi",
          isNewArrival: false,
          offers: [
            { listingId: "lst_khadi_dhanmondi", merchantId: "mer_dhanmondi_handloom", size: "XL", color: "Charcoal Moss", priceBdt: 2900, discountPriceBdt: null, availableQty: 5 }
          ]
        },
        {
          id: "prod_cargo_jogger",
          name: "Mirpur Utility Cargo Trouser",
          brand: "Porey Street",
          category: "Streetwear",
          fabric: "Stretch Cotton Twill",
          rating: 4.6,
          imageKey: "streetwear",
          isNewArrival: false,
          offers: [
            { listingId: "lst_cargo_uttara", merchantId: "mer_uttara_street", size: "32", color: "Sand Khaki", priceBdt: 2150, discountPriceBdt: 1850, availableQty: 12 }
          ]
        }
      ],
      cart: [],
      orders: []
    };
  }

  function loadLocalState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_) {}
    const fresh = createDefaultState();
    saveLocalState(fresh);
    return fresh;
  }

  function saveLocalState(nextState) {
    if (hasBridge()) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
    } catch (_) {}
  }

  let state = hasBridge() ? createDefaultState() : loadLocalState();
  let activeSection = "home";
  let activeWorkspaceTab = "shop";
  let currentFilter = { query: "", category: "All", sort: "popular", verifiedOnly: false };

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatBdt(amount) {
    return "৳" + Number(amount || 0).toLocaleString("en-US");
  }

  function getImageSrc(imageKey) {
    return IMAGE_MAP[imageKey] || IMAGE_MAP.hero;
  }

  function getMerchant(id) {
    return state.merchants.find((m) => m.id === id) || null;
  }

  function getBestOffer(product) {
    const offers = (product.offers || []).filter((o) => {
      const m = getMerchant(o.merchantId);
      return m && !m.isSuspended && o.availableQty > 0;
    });
    if (!offers.length) return (product.offers || [])[0] || null;
    offers.sort((a, b) => {
      const ma = getMerchant(a.merchantId);
      const mb = getMerchant(b.merchantId);
      const va = ma && ma.isVerified ? 1 : 0;
      const vb = mb && mb.isVerified ? 1 : 0;
      if (va !== vb) return vb - va;
      return (a.discountPriceBdt || a.priceBdt) - (b.discountPriceBdt || b.priceBdt);
    });
    return offers[0];
  }

  let toastTimer = null;
  function showToast(msg) {
    const el = document.getElementById("toast");
    if (!el) return;
    el.textContent = msg;
    el.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add("hidden"), 3200);
  }

  function applyTheme() {
    const theme = state.isDarkMode ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", theme);
    const icon = document.getElementById("theme-icon");
    const label = document.getElementById("theme-label");
    if (icon) icon.textContent = state.isDarkMode ? "☀️" : "🌙";
    if (label) label.textContent = state.isDarkMode ? "Light" : "Dark";
  }

  function switchSection(sectionId) {
    activeSection = sectionId;
    document.querySelectorAll(".section").forEach((el) => {
      el.classList.toggle("active-section", el.id === `sec-${sectionId}`);
    });
    document.querySelectorAll("[data-section-btn]").forEach((btn) => {
      btn.classList.toggle("active", btn.getAttribute("data-section-btn") === sectionId);
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove("hidden");
  }

  function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add("hidden");
  }

  function openAuthModal(roleTab = "customer", showGateNotice = false) {
    const notice = document.getElementById("auth-gate-notice");
    if (notice) notice.classList.toggle("hidden", !showGateNotice);
    selectAuthTab(roleTab);
    openModal("auth-modal");
  }

  function selectAuthTab(role) {
    document.querySelectorAll(".auth-tab").forEach((t) => {
      t.classList.toggle("active", t.getAttribute("data-auth-role") === role);
    });
    document.querySelectorAll(".auth-panel").forEach((p) => {
      p.classList.toggle("active", p.id === `auth-panel-${role}`);
    });
  }

  function renderHeaderAndBanner() {
    const roleChip = document.getElementById("role-chip");
    const statusText = document.getElementById("role-status-text");
    const authLabel = document.getElementById("auth-btn-label");
    const bagCount = document.getElementById("bag-count");

    const cartTotalQty = state.isGuest ? 0 : (state.cart || []).reduce((sum, item) => sum + (item.qty || 1), 0);
    if (bagCount) bagCount.textContent = String(cartTotalQty);

    if (state.isGuest || state.activeRole === "GUEST") {
      if (roleChip) roleChip.textContent = "GUEST";
      if (authLabel) authLabel.textContent = "Guest · Login";
      if (statusText) {
        statusText.textContent =
          "Browsing as Guest. Log in as Customer to buy products, or register as Shop Owner (Admin verification required).";
      }
    } else if (state.activeRole === "ADMIN" || state.activeRole === "SUPER_ADMIN") {
      if (roleChip) roleChip.textContent = "ADMIN";
      if (authLabel) authLabel.textContent = `${state.activeUser?.name || "Platform Admin"}`;
      if (statusText) {
        const pendingCount = state.merchants.filter((m) => !m.isVerified && !m.isSuspended).length;
        statusText.textContent = `Admin Console active · ${pendingCount} shop(s) awaiting KYC verification for Verified sign.`;
      }
    } else if (state.activeRole.includes("MERCHANT") || state.activeRole === "STORE_EMPLOYEE") {
      const ownShop = state.merchants.find((m) => m.ownerUserId === state.activeUser?.id || m.id === state.activeUser?.merchantId);
      const badge = ownShop?.isVerified ? "✔ Verified Shop" : "⏳ Pending Admin Verification";
      if (roleChip) roleChip.textContent = "SHOP OWNER";
      if (authLabel) authLabel.textContent = `${state.activeUser?.name || "Shop Owner"}`;
      if (statusText) statusText.textContent = `${ownShop?.name || "Your Shop"} · Status: ${badge}`;
    } else {
      if (roleChip) roleChip.textContent = "CUSTOMER";
      if (authLabel) authLabel.textContent = `${state.activeUser?.name || "Customer"}`;
      if (statusText) statusText.textContent = `Signed in as Customer (${state.activeUser?.email || ""}) · Ready to buy & checkout.`;
    }
  }

  function renderProductCard(product) {
    const offer = getBestOffer(product);
    const merchant = offer ? getMerchant(offer.merchantId) : null;
    const priceNow = offer ? (offer.discountPriceBdt || offer.priceBdt) : 0;
    const priceOld = offer && offer.discountPriceBdt ? offer.priceBdt : null;
    const totalStock = (product.offers || []).reduce((acc, o) => acc + Math.max(0, o.availableQty || 0), 0);
    const badgeHtml = merchant?.isVerified
      ? `<span class="verified-sign">✔ Verified Shop</span>`
      : `<span class="pending-sign">⏳ Pending KYC</span>`;
    const ctaText = state.isGuest || state.activeRole === "GUEST" ? "Login to Buy" : "Add to Bag";

    return `
      <article class="product-card" data-product-id="${escapeHtml(product.id)}">
        <div class="product-img-wrap" data-open-product="${escapeHtml(product.id)}">
          <img src="${escapeHtml(getImageSrc(product.imageKey))}" alt="${escapeHtml(product.name)}" loading="lazy" />
          ${product.isNewArrival ? `<span class="product-tag">NEW ARRIVAL</span>` : priceOld ? `<span class="product-tag" style="background:var(--coral);color:#fff;">OFFER</span>` : ""}
        </div>
        <div class="product-body">
          <div class="product-meta">
            <span>${escapeHtml(product.brand)} · ${escapeHtml(product.category)}</span>
            <span>★ ${escapeHtml(product.rating)}</span>
          </div>
          <div class="product-title" data-open-product="${escapeHtml(product.id)}">${escapeHtml(product.name)}</div>
          <div class="muted">${escapeHtml(product.fabric)}</div>
          <div class="product-shop-row">
            <span>${escapeHtml(merchant?.name || "Dhaka Studio")}</span>
            ${badgeHtml}
          </div>
          <div class="product-footer">
            <div>
              <span class="price-now">${formatBdt(priceNow)}</span>
              ${priceOld ? `<span class="price-old">${formatBdt(priceOld)}</span>` : ""}
              <div class="muted">${totalStock > 0 ? `${totalStock} in stock` : "Out of stock"}</div>
            </div>
            <button class="btn btn-sm btn-primary" type="button" data-quick-buy="${escapeHtml(offer?.listingId || "")}">
              ${ctaText}
            </button>
          </div>
        </div>
      </article>
    `;
  }

  function renderCatalog() {
    const featuredGrid = document.getElementById("home-featured-grid");
    const catalogGrid = document.getElementById("catalog-grid");
    const verifiedStat = document.getElementById("stat-verified-shops");
    const productStat = document.getElementById("stat-products");

    if (verifiedStat) verifiedStat.textContent = String(state.merchants.filter((m) => m.isVerified && !m.isSuspended).length);
    if (productStat) productStat.textContent = String(state.products.length);

    if (featuredGrid) {
      featuredGrid.innerHTML = state.products.slice(0, 4).map(renderProductCard).join("");
    }

    const q = currentFilter.query.trim().toLowerCase();
    let list = state.products.filter((p) => {
      const offer = getBestOffer(p);
      const merchant = offer ? getMerchant(offer.merchantId) : null;
      const matchesQ =
        !q ||
        p.name.toLowerCase().includes(q) ||
        p.brand.toLowerCase().includes(q) ||
        p.fabric.toLowerCase().includes(q) ||
        p.category.toLowerCase().includes(q) ||
        (merchant && (merchant.name.toLowerCase().includes(q) || merchant.area.toLowerCase().includes(q)));
      const matchesCat = currentFilter.category === "All" || p.category === currentFilter.category;
      const matchesVerified = !currentFilter.verifiedOnly || (merchant && merchant.isVerified);
      return matchesQ && matchesCat && matchesVerified;
    });

    if (currentFilter.sort === "price_asc") {
      list.sort((a, b) => {
        const oa = getBestOffer(a);
        const ob = getBestOffer(b);
        return (oa?.discountPriceBdt || oa?.priceBdt || 0) - (ob?.discountPriceBdt || ob?.priceBdt || 0);
      });
    } else if (currentFilter.sort === "price_desc") {
      list.sort((a, b) => {
        const oa = getBestOffer(a);
        const ob = getBestOffer(b);
        return (ob?.discountPriceBdt || ob?.priceBdt || 0) - (oa?.discountPriceBdt || oa?.priceBdt || 0);
      });
    } else if (currentFilter.sort === "rating") {
      list.sort((a, b) => (b.rating || 0) - (a.rating || 0));
    }

    if (catalogGrid) {
      catalogGrid.innerHTML = list.length
        ? list.map(renderProductCard).join("")
        : `<div class="panel-card"><strong>No matching items found.</strong><p class="muted">Try clearing filters or unchecking Verified Only.</p></div>`;
    }
  }

  function renderShops() {
    const grid = document.getElementById("shops-grid");
    if (!grid) return;
    grid.innerHTML = state.merchants
      .filter((m) => !m.isSuspended)
      .map((m) => {
        const badge = m.isVerified
          ? `<span class="verified-sign">✔ Verified Shop</span>`
          : `<span class="pending-sign">⏳ Pending Admin KYC</span>`;
        return `
          <div class="shop-card">
            <div class="shop-card-head">
              <div>
                <strong>${escapeHtml(m.name)}</strong>
                <div class="muted">${escapeHtml(m.area)} · ${escapeHtml(m.storeType)}</div>
              </div>
              ${badge}
            </div>
            <div class="muted">KYC / Trade License: <code>${escapeHtml(m.tradeLicenseRef || "N/A")}</code></div>
            <div class="muted">Rating: ★ ${escapeHtml(m.rating)} · Status: ${m.isVerified ? "Verified & Active" : "Awaiting Admin Approval"}</div>
          </div>
        `;
      })
      .join("");
  }

  function renderWorkspaces() {
    const shopContainer = document.getElementById("shop-owner-content");
    const adminContainer = document.getElementById("admin-verify-content");

    if (shopContainer) {
      const isShopOwner = state.activeRole.includes("MERCHANT") || state.activeRole === "STORE_EMPLOYEE";
      const myMerchant = state.merchants.find(
        (m) => m.ownerUserId === state.activeUser?.id || m.id === state.activeUser?.merchantId
      );

      if (!isShopOwner || !myMerchant) {
        shopContainer.innerHTML = `
          <div class="panel-card">
            <h3>Shop Owner Portal</h3>
            <p class="muted" style="margin:8px 0 14px;">Sign in or register a new Shop Owner account to manage your store and publish products.</p>
            <button class="btn btn-primary" type="button" id="ws-open-shop-auth">Login / Register as Shop Owner</button>
          </div>
        `;
      } else {
        const statusBanner = myMerchant.isVerified
          ? `<div class="notice-emerald"><strong>✔ Verified Shop Sign Granted!</strong> Your shop is verified by Porey Admin and can publish products across the store and website.</div>`
          : `<div class="notice-amber"><strong>⏳ Pending Admin Verification:</strong> Your Trade License (<code>${escapeHtml(myMerchant.tradeLicenseRef || "Pending")}</code>) is under Admin review. Publishing new products unlocks once an Admin grants your Verified sign.</div>`;

        shopContainer.innerHTML = `
          <div class="panel-card">
            <div class="shop-card-head" style="margin-bottom:10px;">
              <div>
                <h3>${escapeHtml(myMerchant.name)}</h3>
                <div class="muted">${escapeHtml(myMerchant.area)} · ${escapeHtml(myMerchant.storeType)}</div>
              </div>
              ${myMerchant.isVerified ? `<span class="verified-sign">✔ Verified</span>` : `<span class="pending-sign">⏳ Pending Verification</span>`}
            </div>
            ${statusBanner}
            <h4>Publish New Product Listing</h4>
            <form id="ws-publish-form" class="form-stack" style="margin-top:10px;">
              <div class="form-row">
                <label>Product Name <input id="pub-name" type="text" placeholder="e.g., Sylhet Silk Panjabi" required /></label>
                <label>Brand <input id="pub-brand" type="text" value="${escapeHtml(myMerchant.name)}" required /></label>
              </div>
              <div class="form-row">
                <label>Category
                  <select id="pub-cat">
                    <option value="Panjabi">Panjabi</option>
                    <option value="Saree">Saree</option>
                    <option value="Streetwear">Streetwear</option>
                    <option value="Footwear">Footwear</option>
                  </select>
                </label>
                <label>Fabric <input id="pub-fabric" type="text" value="100% Cotton" required /></label>
              </div>
              <div class="form-row">
                <label>Price (BDT ৳) <input id="pub-price" type="number" value="2450" min="100" required /></label>
                <label>Initial Stock <input id="pub-stock" type="number" value="10" min="1" required /></label>
              </div>
              <button class="btn btn-primary" type="submit" ${myMerchant.isVerified ? "" : "disabled"}>
                ${myMerchant.isVerified ? "Publish Product to Porey" : "Locked Until Admin Verifies Shop"}
              </button>
            </form>
          </div>
        `;
      }
    }

    if (adminContainer) {
      const isAdmin = state.activeRole === "ADMIN" || state.activeRole === "SUPER_ADMIN";
      if (!isAdmin) {
        adminContainer.innerHTML = `
          <div class="panel-card">
            <h3>Platform Admin Verification Desk</h3>
            <p class="muted" style="margin:8px 0 14px;">Admin verification is required before newly registered shops receive the <strong>✔ Verified</strong> sign.</p>
            <button class="btn btn-primary" type="button" id="ws-open-admin-auth">Sign In as Platform Admin</button>
          </div>
        `;
      } else {
        const rows = state.merchants
          .map((m) => {
            const btnLabel = m.isVerified ? "Revoke Verified" : "✔ Verify Shop";
            const btnCls = m.isVerified ? "btn btn-sm btn-outline" : "btn btn-sm btn-emerald";
            return `
              <div class="offer-row">
                <div>
                  <strong>${escapeHtml(m.name)}</strong>
                  <div class="muted">${escapeHtml(m.area)} · License: <code>${escapeHtml(m.tradeLicenseRef || "N/A")}</code></div>
                </div>
                <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                  ${m.isVerified ? `<span class="verified-sign">✔ Verified</span>` : `<span class="pending-sign">⏳ Pending KYC</span>`}
                  <button class="${btnCls}" type="button" data-verify-merchant="${escapeHtml(m.id)}">${btnLabel}</button>
                </div>
              </div>
            `;
          })
          .join("");
        adminContainer.innerHTML = `
          <div class="panel-card">
            <h3>Admin Shop Verification Queue</h3>
            <p class="muted" style="margin-bottom:10px;">Approve pending Trade Licenses to grant shops their green Verified badge across the app and website.</p>
            ${rows}
          </div>
        `;
      }
    }
  }

  function openProductDetail(productId) {
    const product = state.products.find((p) => p.id === productId);
    if (!product) return;
    const body = document.getElementById("product-modal-body");
    if (!body) return;

    const offersHtml = (product.offers || [])
      .map((o) => {
        const m = getMerchant(o.merchantId);
        const price = o.discountPriceBdt || o.priceBdt;
        const verified = m?.isVerified
          ? `<span class="verified-sign">✔ Verified</span>`
          : `<span class="pending-sign">⏳ Pending KYC</span>`;
        const btnText = state.isGuest || state.activeRole === "GUEST" ? "Login as Customer to Buy" : "Add to Bag";
        return `
          <div class="offer-row">
            <div>
              <strong>${escapeHtml(m?.name || "Shop")}</strong> ${verified}
              <div class="muted">${escapeHtml(m?.area || "")} · Size ${escapeHtml(o.size)} · ${escapeHtml(o.color)} · ${o.availableQty} left</div>
            </div>
            <div style="text-align:right;">
              <div class="price-now">${formatBdt(price)}</div>
              <button class="btn btn-sm btn-primary" type="button" data-modal-buy="${escapeHtml(o.listingId)}" ${o.availableQty <= 0 ? "disabled" : ""}>
                ${btnText}
              </button>
            </div>
          </div>
        `;
      })
      .join("");

    body.innerHTML = `
      <div class="detail-layout">
        <img class="detail-img" src="${escapeHtml(getImageSrc(product.imageKey))}" alt="${escapeHtml(product.name)}" />
        <div>
          <div class="muted">${escapeHtml(product.brand)} · ${escapeHtml(product.category)} · ★ ${escapeHtml(product.rating)}</div>
          <h2 style="margin:6px 0;">${escapeHtml(product.name)}</h2>
          <p class="muted" style="margin-bottom:12px;">Fabric: ${escapeHtml(product.fabric)}</p>
          <h4>Available Store Branches</h4>
          ${offersHtml}
        </div>
      </div>
    `;
    openModal("product-modal");
  }

  function renderBagModal() {
    const body = document.getElementById("bag-modal-body");
    if (!body) return;

    if (state.isGuest || state.activeRole === "GUEST") {
      body.innerHTML = `
        <div class="notice-amber">
          <strong>Customer Login Required to Buy:</strong> Guests can browse products and verified shops freely, but must sign in or register as a Customer to add items to bag and checkout.
        </div>
        <button class="btn btn-primary" id="bag-login-gate-btn" type="button">Login / Register as Customer</button>
      `;
      return;
    }

    const items = state.cart || [];
    if (!items.length) {
      body.innerHTML = `
        <p class="muted" style="margin-bottom:12px;">Your shopping bag is currently empty.</p>
        <button class="btn btn-outline" data-close-modal="bag-modal" type="button">Continue Browsing</button>
      `;
      return;
    }

    let subtotal = 0;
    const rowsHtml = items
      .map((item) => {
        const lineTotal = (item.unitPriceBdt || 0) * (item.qty || 1);
        subtotal += lineTotal;
        return `
          <div class="bag-item-row">
            <div>
              <strong>${escapeHtml(item.productName)}</strong>
              <div class="muted">${escapeHtml(item.merchantName)} · ${formatBdt(item.unitPriceBdt)} each</div>
            </div>
            <div class="qty-controls">
              <button class="btn btn-sm btn-outline" type="button" data-qty-delta="-1" data-cart-id="${escapeHtml(item.id)}">-</button>
              <strong>${item.qty}</strong>
              <button class="btn btn-sm btn-outline" type="button" data-qty-delta="1" data-cart-id="${escapeHtml(item.id)}">+</button>
              <button class="btn btn-sm btn-ghost" type="button" data-remove-cart="${escapeHtml(item.id)}">Remove</button>
            </div>
          </div>
        `;
      })
      .join("");

    const activeZone = (state.zones || []).find((z) => z.id === state.selectedZoneId) || state.zones[0];
    const deliveryFee = activeZone ? activeZone.feeBdt : 60;
    const promoUpper = (state.promoCode || "").trim().toUpperCase();
    const discount = promoUpper === "POREY10" ? Math.min(600, Math.round(subtotal * 0.1)) : promoUpper === "DHAKA50" ? Math.min(50, subtotal) : 0;
    const grandTotal = Math.max(0, subtotal - discount + deliveryFee);

    const zonesOptions = (state.zones || [])
      .map((z) => `<option value="${escapeHtml(z.id)}" ${z.id === activeZone?.id ? "selected" : ""}>${escapeHtml(z.name)} (${formatBdt(z.feeBdt)} · ${escapeHtml(z.window)})</option>`)
      .join("");

    body.innerHTML = `
      <div>${rowsHtml}</div>
      <form id="checkout-form" class="form-stack" style="margin-top:14px;">
        <label>Delivery Address (Dhaka / BD)
          <input id="checkout-address" type="text" value="${escapeHtml(state.activeUser?.address || "House 24, Road 11, Banani, Dhaka")}" required />
        </label>
        <div class="form-row">
          <label>Delivery Zone
            <select id="checkout-zone">${zonesOptions}</select>
          </label>
          <label>Payment Method
            <select id="checkout-payment">
              <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
              <option value="BKASH_SANDBOX">bKash (Sandbox)</option>
              <option value="NAGAD_SANDBOX">Nagad (Sandbox)</option>
              <option value="SSLCOMMERZ_SANDBOX">Card / SSLCommerz</option>
            </select>
          </label>
        </div>
        <div class="form-row">
          <label>Promo Code (POREY10 or DHAKA50)
            <input id="checkout-promo" type="text" value="${escapeHtml(state.promoCode || "")}" placeholder="POREY10" />
          </label>
          <div style="display:flex;align-items:flex-end;">
            <button id="apply-promo-btn" class="btn btn-outline" type="button" style="width:100%;">Apply Promo</button>
          </div>
        </div>
        <div class="panel-card" style="margin:6px 0 0;">
          <div>Subtotal: <strong>${formatBdt(subtotal)}</strong></div>
          <div>Delivery: <strong>${formatBdt(deliveryFee)}</strong></div>
          ${discount > 0 ? `<div>Discount: <strong>-${formatBdt(discount)}</strong></div>` : ""}
          <div style="font-size:1.1rem;margin-top:4px;">Total Payable: <strong>${formatBdt(grandTotal)}</strong></div>
        </div>
        <button class="btn btn-primary" type="submit">Confirm Order (${formatBdt(grandTotal)})</button>
      </form>
    `;
  }

  function renderAll() {
    applyTheme();
    renderHeaderAndBanner();
    renderCatalog();
    renderShops();
    renderWorkspaces();
    const bagModal = document.getElementById("bag-modal");
    if (bagModal && !bagModal.classList.contains("hidden")) {
      renderBagModal();
    }
  }

  // --- ACTIONS (BRIDGE OR LOCAL) ---
  function handleToggleTheme() {
    if (hasBridge()) {
      window.PoreyBridge.toggleTheme();
    } else {
      state.isDarkMode = !state.isDarkMode;
      saveLocalState(state);
      renderAll();
      showToast(state.isDarkMode ? "Dark Mode enabled" : "Light Mode enabled");
    }
  }

  function handleLogin(identifier, password) {
    if (hasBridge()) {
      window.PoreyBridge.loginWithCredentials(identifier, password);
      closeModal("auth-modal");
      return;
    }
    const clean = identifier.trim().toLowerCase();
    const user = state.users.find(
      (u) => u.email.toLowerCase() === clean || u.phone.toLowerCase() === clean
    );
    if (!user) {
      showToast(`No account found for '${identifier}'`);
      return;
    }
    if (user.password !== password) {
      showToast("Incorrect password (demo accounts use 'porey123')");
      return;
    }
    state.isGuest = false;
    state.activeRole = user.role;
    state.activeUser = user;
    saveLocalState(state);
    closeModal("auth-modal");
    renderAll();
    showToast(`Signed in as ${user.name}`);
  }

  function handleDemoLogin(roleName) {
    if (hasBridge()) {
      window.PoreyBridge.loginDemoRole(roleName);
      closeModal("auth-modal");
      return;
    }
    if (roleName === "GUEST") {
      state.isGuest = true;
      state.activeRole = "GUEST";
      state.activeUser = null;
      saveLocalState(state);
      closeModal("auth-modal");
      renderAll();
      showToast("Browsing as Guest");
      return;
    }
    const user = state.users.find((u) => u.role === roleName);
    if (!user) return;
    state.isGuest = false;
    state.activeRole = user.role;
    state.activeUser = user;
    saveLocalState(state);
    closeModal("auth-modal");
    renderAll();
    showToast(`Signed in as ${user.name}`);
  }

  function handleRegisterCustomer(name, phone, email, password, address) {
    if (hasBridge()) {
      window.PoreyBridge.registerCustomer(name, phone, email, password, address);
      closeModal("auth-modal");
      return;
    }
    if (password.length < 4) {
      showToast("Password must be at least 4 characters.");
      return;
    }
    const user = {
      id: "usr_cust_" + Date.now(),
      name: name.trim(),
      phone: phone.trim(),
      email: email.trim(),
      role: "CUSTOMER",
      password,
      address: address.trim()
    };
    state.users.push(user);
    state.isGuest = false;
    state.activeRole = "CUSTOMER";
    state.activeUser = user;
    saveLocalState(state);
    closeModal("auth-modal");
    renderAll();
    showToast(`Welcome to Porey, ${user.name}!`);
  }

  function handleRegisterShopOwner(owner, shopName, phone, email, area, storeType, addr, license, password) {
    if (hasBridge()) {
      window.PoreyBridge.registerShopOwner(owner, phone, email, password, shopName, area, addr, storeType, license);
      closeModal("auth-modal");
      switchSection("shops");
      return;
    }
    if (!license.trim()) {
      showToast("Trade License / DBID reference is required for Admin verification.");
      return;
    }
    const uid = "usr_merch_" + Date.now();
    const mid = "mer_" + Date.now();
    const role = storeType === "PHYSICAL_POS" ? "PHYSICAL_MERCHANT" : storeType === "ONLINE_ONLY" ? "ONLINE_MERCHANT" : "NO_POS_MERCHANT";
    const user = { id: uid, name: owner.trim(), phone: phone.trim(), email: email.trim(), role, merchantId: mid, password, address: addr.trim() };
    const merchant = {
      id: mid,
      name: shopName.trim(),
      area: area.trim(),
      storeType,
      isVerified: false,
      isSuspended: false,
      rating: 4.5,
      tradeLicenseRef: license.trim(),
      ownerUserId: uid
    };
    state.users.push(user);
    state.merchants.push(merchant);
    state.isGuest = false;
    state.activeRole = role;
    state.activeUser = user;
    saveLocalState(state);
    closeModal("auth-modal");
    renderAll();
    switchSection("workspace");
    showToast(`${shopName} registered! Pending Admin verification for Verified sign.`);
  }

  function handleAddToCart(listingId) {
    if (!listingId) return;
    if (state.isGuest || state.activeRole === "GUEST") {
      closeModal("product-modal");
      openAuthModal("customer", true);
      showToast("Please log in as a Customer to buy products.");
      return;
    }
    if (hasBridge()) {
      window.PoreyBridge.addToCart(listingId, 1);
      closeModal("product-modal");
      return;
    }
    let foundProduct = null;
    let foundOffer = null;
    for (const p of state.products) {
      const o = (p.offers || []).find((x) => x.listingId === listingId);
      if (o) {
        foundProduct = p;
        foundOffer = o;
        break;
      }
    }
    if (!foundOffer || foundOffer.availableQty <= 0) {
      showToast("Out of stock");
      return;
    }
    const merchant = getMerchant(foundOffer.merchantId);
    const existing = state.cart.find((c) => c.listingId === listingId);
    if (existing) {
      if (existing.qty >= foundOffer.availableQty) {
        showToast("Stock limit reached");
        return;
      }
      existing.qty += 1;
    } else {
      state.cart.push({
        id: "cart_" + Date.now(),
        listingId,
        productName: foundProduct.name,
        merchantName: merchant?.name || "Shop",
        unitPriceBdt: foundOffer.discountPriceBdt || foundOffer.priceBdt,
        qty: 1
      });
    }
    saveLocalState(state);
    closeModal("product-modal");
    renderAll();
    showToast(`Added ${foundProduct.name} to Bag`);
  }

  function handleVerifyMerchant(merchantId) {
    if (hasBridge()) {
      window.PoreyBridge.toggleMerchantVerified(merchantId);
      return;
    }
    const m = getMerchant(merchantId);
    if (!m) return;
    m.isVerified = !m.isVerified;
    saveLocalState(state);
    renderAll();
    showToast(m.isVerified ? `${m.name} verified! ✔ Verified sign granted.` : `${m.name} verification paused.`);
  }

  function handlePublishProduct(name, brand, category, fabric, priceBdt, stock) {
    const myMerchant = state.merchants.find(
      (m) => m.ownerUserId === state.activeUser?.id || m.id === state.activeUser?.merchantId
    );
    if (!myMerchant) return;
    if (!myMerchant.isVerified) {
      showToast("Admin must verify your shop first before you can publish products.");
      return;
    }
    if (hasBridge()) {
      const imageKey = category === "Panjabi" ? "panjabi" : category === "Saree" ? "saree" : "streetwear";
      window.PoreyBridge.publishProduct(
        myMerchant.id,
        name,
        brand,
        category,
        fabric,
        "M",
        "Natural",
        "SKU-" + Math.floor(Math.random() * 9000 + 1000),
        Number(priceBdt),
        Number(stock),
        imageKey
      );
      return;
    }
    const imageKey = category === "Panjabi" ? "panjabi" : category === "Saree" ? "saree" : "streetwear";
    state.products.unshift({
      id: "prod_" + Date.now(),
      name,
      brand,
      category,
      fabric,
      rating: 4.8,
      imageKey,
      isNewArrival: true,
      offers: [
        {
          listingId: "lst_" + Date.now(),
          merchantId: myMerchant.id,
          size: "M",
          color: "Classic",
          priceBdt: Number(priceBdt),
          discountPriceBdt: null,
          availableQty: Number(stock)
        }
      ]
    });
    saveLocalState(state);
    renderAll();
    switchSection("catalog");
    showToast(`${name} published to Porey catalog!`);
  }

  // --- EVENT WIRING ---
  document.addEventListener("click", (e) => {
    const target = e.target;
    if (!(target instanceof HTMLElement)) return;

    const secBtn = target.closest("[data-section-btn]");
    if (secBtn) {
      switchSection(secBtn.getAttribute("data-section-btn"));
      return;
    }

    const wsTab = target.closest("[data-ws-tab]");
    if (wsTab) {
      activeWorkspaceTab = wsTab.getAttribute("data-ws-tab");
      document.querySelectorAll("[data-ws-tab]").forEach((b) => b.classList.toggle("active", b === wsTab));
      document.getElementById("ws-shop-panel")?.classList.toggle("active", activeWorkspaceTab === "shop");
      document.getElementById("ws-admin-panel")?.classList.toggle("active", activeWorkspaceTab === "admin");
      return;
    }

    const authRoleBtn = target.closest("[data-auth-role]");
    if (authRoleBtn) {
      selectAuthTab(authRoleBtn.getAttribute("data-auth-role"));
      return;
    }

    const closeBtn = target.closest("[data-close-modal]");
    if (closeBtn) {
      closeModal(closeBtn.getAttribute("data-close-modal"));
      return;
    }

    if (target.classList.contains("modal-backdrop")) {
      target.classList.add("hidden");
      return;
    }

    const openProd = target.closest("[data-open-product]");
    if (openProd) {
      openProductDetail(openProd.getAttribute("data-open-product"));
      return;
    }

    const quickBuy = target.closest("[data-quick-buy]");
    if (quickBuy) {
      handleAddToCart(quickBuy.getAttribute("data-quick-buy"));
      return;
    }

    const modalBuy = target.closest("[data-modal-buy]");
    if (modalBuy) {
      handleAddToCart(modalBuy.getAttribute("data-modal-buy"));
      return;
    }

    const verifyBtn = target.closest("[data-verify-merchant]");
    if (verifyBtn) {
      handleVerifyMerchant(verifyBtn.getAttribute("data-verify-merchant"));
      return;
    }

    const qtyBtn = target.closest("[data-qty-delta]");
    if (qtyBtn) {
      const cartId = qtyBtn.getAttribute("data-cart-id");
      const delta = Number(qtyBtn.getAttribute("data-qty-delta"));
      if (hasBridge()) {
        window.PoreyBridge.updateCartQty(cartId, delta);
      } else {
        const item = state.cart.find((c) => c.id === cartId);
        if (item) {
          item.qty += delta;
          if (item.qty <= 0) state.cart = state.cart.filter((c) => c.id !== cartId);
          saveLocalState(state);
          renderAll();
        }
      }
      return;
    }

    const removeBtn = target.closest("[data-remove-cart]");
    if (removeBtn) {
      const cartId = removeBtn.getAttribute("data-remove-cart");
      if (hasBridge()) {
        window.PoreyBridge.removeCartItem(cartId);
      } else {
        state.cart = state.cart.filter((c) => c.id !== cartId);
        saveLocalState(state);
        renderAll();
      }
      return;
    }

    if (target.id === "ws-open-shop-auth") {
      openAuthModal("shop", false);
      return;
    }
    if (target.id === "ws-open-admin-auth") {
      openAuthModal("admin", false);
      return;
    }
    if (target.id === "bag-login-gate-btn") {
      closeModal("bag-modal");
      openAuthModal("customer", true);
      return;
    }
    if (target.id === "apply-promo-btn") {
      const code = document.getElementById("checkout-promo")?.value || "";
      if (hasBridge()) {
        window.PoreyBridge.updatePromoCode(code);
      } else {
        state.promoCode = code;
        saveLocalState(state);
        renderBagModal();
      }
    }
  });

  document.getElementById("theme-toggle-btn")?.addEventListener("click", handleToggleTheme);
  document.getElementById("auth-open-btn")?.addEventListener("click", () => openAuthModal("customer", false));
  document.getElementById("banner-login-btn")?.addEventListener("click", () => openAuthModal("customer", false));
  document.getElementById("hero-shop-btn")?.addEventListener("click", () => switchSection("catalog"));
  document.getElementById("home-view-all-btn")?.addEventListener("click", () => switchSection("catalog"));
  document.getElementById("hero-register-shop-btn")?.addEventListener("click", () => {
    openAuthModal("shop", false);
    document.getElementById("shop-mode-reg")?.click();
  });
  document.getElementById("shops-register-btn")?.addEventListener("click", () => {
    openAuthModal("shop", false);
    document.getElementById("shop-mode-reg")?.click();
  });
  document.getElementById("hero-guest-btn")?.addEventListener("click", () => handleDemoLogin("GUEST"));
  document.getElementById("bag-open-btn")?.addEventListener("click", () => {
    renderBagModal();
    openModal("bag-modal");
  });

  // Auth mode toggles
  document.getElementById("cust-mode-login")?.addEventListener("click", () => {
    document.getElementById("cust-mode-login")?.classList.add("active");
    document.getElementById("cust-mode-reg")?.classList.remove("active");
    document.getElementById("cust-login-form")?.classList.remove("hidden");
    document.getElementById("cust-reg-form")?.classList.add("hidden");
  });
  document.getElementById("cust-mode-reg")?.addEventListener("click", () => {
    document.getElementById("cust-mode-reg")?.classList.add("active");
    document.getElementById("cust-mode-login")?.classList.remove("active");
    document.getElementById("cust-reg-form")?.classList.remove("hidden");
    document.getElementById("cust-login-form")?.classList.add("hidden");
  });
  document.getElementById("shop-mode-login")?.addEventListener("click", () => {
    document.getElementById("shop-mode-login")?.classList.add("active");
    document.getElementById("shop-mode-reg")?.classList.remove("active");
    document.getElementById("shop-login-form")?.classList.remove("hidden");
    document.getElementById("shop-reg-form")?.classList.add("hidden");
  });
  document.getElementById("shop-mode-reg")?.addEventListener("click", () => {
    document.getElementById("shop-mode-reg")?.classList.add("active");
    document.getElementById("shop-mode-login")?.classList.remove("active");
    document.getElementById("shop-reg-form")?.classList.remove("hidden");
    document.getElementById("shop-login-form")?.classList.add("hidden");
  });

  // Forms
  document.getElementById("cust-login-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    handleLogin(
      document.getElementById("cust-login-id").value,
      document.getElementById("cust-login-pw").value
    );
  });
  document.getElementById("cust-demo-btn")?.addEventListener("click", () => handleDemoLogin("CUSTOMER"));
  document.getElementById("cust-reg-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    handleRegisterCustomer(
      document.getElementById("cust-reg-name").value,
      document.getElementById("cust-reg-phone").value,
      document.getElementById("cust-reg-email").value,
      document.getElementById("cust-reg-pw").value,
      document.getElementById("cust-reg-addr").value
    );
  });

  document.getElementById("shop-login-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    handleLogin(
      document.getElementById("shop-login-id").value,
      document.getElementById("shop-login-pw").value
    );
  });
  document.getElementById("shop-demo-btn")?.addEventListener("click", () => handleDemoLogin("PHYSICAL_MERCHANT"));
  document.getElementById("shop-reg-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    handleRegisterShopOwner(
      document.getElementById("shop-reg-owner").value,
      document.getElementById("shop-reg-name").value,
      document.getElementById("shop-reg-phone").value,
      document.getElementById("shop-reg-email").value,
      document.getElementById("shop-reg-area").value,
      document.getElementById("shop-reg-type").value,
      document.getElementById("shop-reg-addr").value,
      document.getElementById("shop-reg-license").value,
      document.getElementById("shop-reg-pw").value
    );
  });

  document.getElementById("guest-continue-btn")?.addEventListener("click", () => handleDemoLogin("GUEST"));
  document.getElementById("admin-demo-btn")?.addEventListener("click", () => handleDemoLogin("ADMIN"));

  document.addEventListener("submit", (e) => {
    if (e.target && e.target.id === "ws-publish-form") {
      e.preventDefault();
      handlePublishProduct(
        document.getElementById("pub-name").value,
        document.getElementById("pub-brand").value,
        document.getElementById("pub-cat").value,
        document.getElementById("pub-fabric").value,
        document.getElementById("pub-price").value,
        document.getElementById("pub-stock").value
      );
    } else if (e.target && e.target.id === "checkout-form") {
      e.preventDefault();
      const addr = document.getElementById("checkout-address").value;
      const payment = document.getElementById("checkout-payment").value;
      const zoneId = document.getElementById("checkout-zone").value;
      if (hasBridge()) {
        window.PoreyBridge.selectDeliveryZone(zoneId);
        window.PoreyBridge.placeOrder(addr, payment);
        closeModal("bag-modal");
      } else {
        // Atomic local checkout deduction
        for (const item of state.cart) {
          for (const p of state.products) {
            const o = (p.offers || []).find((x) => x.listingId === item.listingId);
            if (o) o.availableQty = Math.max(0, o.availableQty - item.qty);
          }
        }
        const orderCode = "PRY-" + Math.floor(100000 + Math.random() * 899999);
        state.orders.unshift({ id: orderCode, address: addr, payment, createdAt: Date.now() });
        state.cart = [];
        state.promoCode = "";
        saveLocalState(state);
        closeModal("bag-modal");
        renderAll();
        showToast(`Order ${orderCode} placed successfully!`);
      }
    }
  });

  document.addEventListener("change", (e) => {
    if (e.target && e.target.id === "checkout-zone") {
      if (hasBridge()) {
        window.PoreyBridge.selectDeliveryZone(e.target.value);
      } else {
        state.selectedZoneId = e.target.value;
        saveLocalState(state);
        renderBagModal();
      }
    }
  });

  // Search & Filters
  document.getElementById("search-input")?.addEventListener("input", (e) => {
    currentFilter.query = e.target.value;
    renderCatalog();
  });
  document.getElementById("category-select")?.addEventListener("change", (e) => {
    currentFilter.category = e.target.value;
    renderCatalog();
  });
  document.getElementById("sort-select")?.addEventListener("change", (e) => {
    currentFilter.sort = e.target.value;
    renderCatalog();
  });
  document.getElementById("verified-only-check")?.addEventListener("change", (e) => {
    currentFilter.verifiedOnly = e.target.checked;
    renderCatalog();
  });

  // Exposed sync hook for Android WebView
  window.PoreyWebsite = {
    syncState: function (incoming) {
      if (!incoming) return;
      state = Object.assign({}, state, incoming);
      renderAll();
    }
  };

  renderAll();
})();
