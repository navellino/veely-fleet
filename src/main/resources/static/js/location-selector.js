class LocationSelector extends HTMLElement {
  constructor() {
    super();
    // 1) UI isolata in Shadow DOM
    this.shadow = this.attachShadow({ mode: 'open' });

    // 2) Contenitori per i dati JSON
    this.nations    = [];
    this.regions    = [];
    this.provinces  = [];
    this.comuni     = [];
    this.comuniCap  = [];  // ← array CAP per comune

    // 3) Hidden inputs per Spring MVC bind
    this._hiddenInputs = {};
  }

  async connectedCallback() {
    // ───────────────────────────────────────────────
    // ATTRIBUTI DI BINDING: property‑path Thymeleaf
    // (passati letteralmente in form.html)
    this.nameCountry  = this.getAttribute('country-binding')  || '';
    this.nameRegion   = this.getAttribute('name-region')      || '';
    this.nameProvince = this.getAttribute('name-province')    || '';
    this.nameCity     = this.getAttribute('name-city')        || '';
    this.namePostal   = this.getAttribute('name-postal')      || '';
    this.nameStreet   = this.getAttribute('name-street')      || '';  // ★ NEW ★

    // ───────────────────────────────────────────────
    // ATTRIBUTI DI PREFILL (solo in edit)
    this.initialCountry  = this.getAttribute('data-selected-country')  || 'IT'; // ★ default ITALIA ★
    this.initialRegion   = this.getAttribute('data-selected-region')   || '';
    this.initialProvince = this.getAttribute('data-selected-province') || '';
    this.initialCity     = this.getAttribute('data-selected-city')     || '';
    this.initialPostal   = this.getAttribute('data-selected-postal')   || '';
    this.initialStreet   = this.getAttribute('data-selected-street')   || '';   // ★ NEW ★

    // ───────────────────────────────────────────────
    // FETCH PARALLELO DEI 5 JSON
    let jsonN, jsonR, jsonP, jsonC, jsonCC;
    try {
      [jsonN, jsonR, jsonP, jsonC, jsonCC] = await Promise.all([
        fetch('/js/gi_nazioni.json').then(r => r.json()),
        fetch('/js/gi_regioni.json').then(r => r.json()),
        fetch('/js/gi_province.json').then(r => r.json()),
        fetch('/js/gi_comuni.json').then(r => r.json()),
        fetch('/js/cap.json').then(r => r.json())
      ]);
    } catch (err) {
      console.error('Errore caricamento JSON location-selector:', err);
      return; // interrompi in caso di errore fetch
    }

    // Salvo dataset
    this.nations   = Array.isArray(jsonN) ? jsonN : [];
    this.regions   = Array.isArray(jsonR) ? jsonR : [];
    this.provinces = Array.isArray(jsonP) ? jsonP : [];
    this.comuni    = Array.isArray(jsonC) ? jsonC : [];

    // Normalizza cap.json
    let capsCandidate = jsonCC;
    if (!Array.isArray(capsCandidate)) {
      if (capsCandidate && Array.isArray(capsCandidate.comuniCap)) {
        capsCandidate = capsCandidate.comuniCap;
      } else if (capsCandidate && Array.isArray(capsCandidate.data)) {
        capsCandidate = capsCandidate.data;
      } else if (capsCandidate && typeof capsCandidate === 'object') {
        const vals = Object.values(capsCandidate).flat();
        capsCandidate = Array.isArray(vals) ? vals : [];
      } else {
        console.warn('Formato inesperato per cap.json:', capsCandidate);
        capsCandidate = [];
      }
    }
    this.comuniCap = capsCandidate;

    // Disegno UI + hidden + listeners + prefill
    this.render();
    this._renderHiddenInputs();
    this.attachListeners();
    this._prefill();
  }

  /* ============================================================ */
  /* 1) Disegna UI                                                */
  /* ============================================================ */
  render() {
    // Composizione option Paesi (ordina alfab.)
    const nationOptions = this.nations
      .sort((a,b)=>a.denominazione_nazione.localeCompare(b.denominazione_nazione))
      .map(n => `<option value="${n.sigla_nazione}">${n.denominazione_nazione}</option>`)
      .join('');

    this.shadow.innerHTML = `
      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
      <div class="row gx-3 gy-2 align-items-end">
        <!-- Paese -->
        <div class="col-lg-2">
          <label class="form-label" for="countrySelect">Paese</label>
          <select class="form-select" id="countrySelect">
            <option value="">— seleziona —</option>
            ${nationOptions}
          </select>
        </div>
        <!-- Regione -->
        <div class="col-lg-2">
          <label class="form-label" for="regionSelect">Regione</label>
          <select class="form-select" id="regionSelect" disabled><option value="">— seleziona —</option></select>
        </div>
        <!-- Provincia -->
        <div class="col-lg-2">
          <label class="form-label" for="provinceSelect">Provincia</label>
          <select class="form-select" id="provinceSelect" disabled><option value="">— seleziona —</option></select>
        </div>
        <!-- Comune -->
        <div class="col-lg-3">
          <label class="form-label" for="citySelect">Comune</label>
          <select class="form-select" id="citySelect" disabled><option value="">— seleziona —</option></select>
        </div>
        <!-- CAP -->
        <div class="col-lg-3">
          <label class="form-label" for="postalSelect">CAP</label>
          <select class="form-select" id="postalSelect" disabled><option value="">— seleziona —</option></select>
        </div>
        <!-- Via / Indirizzo (★ NEW ★) -->
        <div class="col-12 pt-1">
          <label class="form-label" for="streetInput">Via / Indirizzo</label>
          <input class="form-control" id="streetInput" type="text" placeholder="Via, Piazza…">
        </div>
      </div>`;
  }

  /* ============================================================ */
  /* 2) Hidden inputs nel Light DOM                               */
  /* ============================================================ */
  _renderHiddenInputs() {
    const map = [
      ['country',  this.nameCountry ],
      ['region',   this.nameRegion  ],
      ['province', this.nameProvince],
      ['city',     this.nameCity    ],
      ['postal',   this.namePostal  ],
      ['street',   this.nameStreet  ]  // ★ NEW ★
    ];
    map.forEach(([key, name]) => {
      if (!name) return;
      const inp = document.createElement('input');
      inp.type  = 'hidden';
      inp.name  = name;
      inp.value = '';
      this._hiddenInputs[key] = inp;
      this.appendChild(inp);
    });
  }

  /* ============================================================ */
  /* 3) Event listener su ogni select / input                     */
  /* ============================================================ */
  attachListeners() {
    const $ = (id) => this.shadow.getElementById(id);

    $("countrySelect").addEventListener('change', () => this.onCountryChange());
    $("regionSelect").addEventListener('change',  () => this.onRegionChange());
    $("provinceSelect").addEventListener('change',()=> this.onProvinceChange());
    $("citySelect").addEventListener('change',    () => this.onCityChange());
    $("postalSelect").addEventListener('change',  () => {
      if (this._hiddenInputs.postal)
        this._hiddenInputs.postal.value = $("postalSelect").value;
    });
    // ★ input via
    $("streetInput").addEventListener('input', (e) => {
      if (this._hiddenInputs.street)
        this._hiddenInputs.street.value = e.target.value;
    });
  }

  /* ============================================================ */
  /* 4) Gestori a cascata (identici all’originale)                 */
  /* ============================================================ */
  onCountryChange() {
    const cc     = this.shadow.getElementById('countrySelect').value;
    const rSel   = this.shadow.getElementById('regionSelect');
    const pSel   = this.shadow.getElementById('provinceSelect');
    const cSel   = this.shadow.getElementById('citySelect');
    const capSel = this.shadow.getElementById('postalSelect');

    [rSel, pSel, cSel, capSel].forEach(sel => {
      sel.innerHTML = '<option value="">— seleziona —</option>';
      sel.disabled  = true;
    });

    if (this._hiddenInputs.country) this._hiddenInputs.country.value = cc;

    if (cc === 'IT') {
      this.regions
        .sort((a,b) => a.denominazione_regione.localeCompare(b.denominazione_regione))
        .forEach(r => rSel.add(new Option(r.denominazione_regione, r.codice_regione)));
      rSel.disabled = false;
    }
  }

  onRegionChange() {
    const code   = this.shadow.getElementById('regionSelect').value;
    const pSel   = this.shadow.getElementById('provinceSelect');
    const cSel   = this.shadow.getElementById('citySelect');
    const capSel = this.shadow.getElementById('postalSelect');

    [pSel, cSel, capSel].forEach(sel => {
      sel.innerHTML = '<option value="">— seleziona —</option>';
      sel.disabled  = true;
    });

    if (this._hiddenInputs.region) this._hiddenInputs.region.value = code;
    if (!code) return;

    this.provinces
      .filter(p => p.codice_regione === code)
      .sort((a,b) => a.denominazione_provincia.localeCompare(b.denominazione_provincia))
      .forEach(p => pSel.add(new Option(p.denominazione_provincia, p.sigla_provincia)));
    pSel.disabled = false;
  }

  onProvinceChange() {
    const sig    = this.shadow.getElementById('provinceSelect').value;
    const cSel   = this.shadow.getElementById('citySelect');
    const capSel = this.shadow.getElementById('postalSelect');

    [cSel, capSel].forEach(sel => {
      sel.innerHTML = '<option value="">— seleziona —</option>';
      sel.disabled  = true;
    });

    if (this._hiddenInputs.province) this._hiddenInputs.province.value = sig;
    if (!sig) return;

    this.comuni
      .filter(c => c.sigla_provincia === sig)
      .sort((a,b) => a.denominazione_ita.localeCompare(b.denominazione_ita))
      .forEach(c => cSel.add(new Option(c.denominazione_ita, c.codice_istat)));
    cSel.disabled = false;
  }

  onCityChange() {
    const istat  = this.shadow.getElementById('citySelect').value;
    const capSel = this.shadow.getElementById('postalSelect');

    const caps = Array.from(
      new Set(this.comuniCap.filter(c => c.codice_istat === istat).map(c => c.cap).filter(Boolean))
    );

    capSel.innerHTML = '<option value="">— seleziona —</option>';
    caps.sort().forEach(cap => capSel.add(new Option(cap, cap)));
    capSel.disabled = caps.length === 0;

    if (caps.length === 1) capSel.value = caps[0];

    if (this._hiddenInputs.city)   this._hiddenInputs.city.value   = istat;
    if (this._hiddenInputs.postal) this._hiddenInputs.postal.value = capSel.value;
  }

  /* ============================================================ */
  /* 5) Prefill in edit + default Italia                          */
  /* ============================================================ */
  _prefill() {
    // Via / indirizzo prima
    if (this.initialStreet) {
      this.shadow.getElementById('streetInput').value = this.initialStreet;
      if (this._hiddenInputs.street) this._hiddenInputs.street.value = this.initialStreet;
    }

    // Paese (default IT se nuovo)
    if (this.initialCountry) {
      this.shadow.getElementById('countrySelect').value = this.initialCountry;
      this.onCountryChange();
    }
    // cascata se edit
    if (this.initialRegion) {
      this.shadow.getElementById('regionSelect').value = this.initialRegion;
      this.onRegionChange();
    }
    if (this.initialProvince) {
      this.shadow.getElementById('provinceSelect').value = this.initialProvince;
      this.onProvinceChange();
    }
    if (this.initialCity) {
      this.shadow.getElementById('citySelect').value = this.initialCity;
      this.onCityChange();
    }
    if (this.initialPostal) {
      this.shadow.getElementById('postalSelect').value = this.initialPostal;
      if (this._hiddenInputs.postal) this._hiddenInputs.postal.value = this.initialPostal;
    }
    if (this._hiddenInputs.country && this.initialCountry) this._hiddenInputs.country.value = this.initialCountry;
  }
}

// Registrazione del WebComponent
customElements.define('location-selector', LocationSelector);