/* Main app logic: parses inputs, renders Chart.js curves, overlays values, shows recommendations, PDF export */
(function(){
  const $ = function(sel){ return document.querySelector(sel); };
  const $$ = function(sel){ return Array.prototype.slice.call(document.querySelectorAll(sel)); };

  const ctx = $('#chart');
  let chart;

  function parseList(str){
    if(!str) return [];
    return str.split(',').map(s => s.trim()).filter(Boolean).map(Number).filter(v => !isNaN(v));
  }

  function zipToPoints(hoursList, biliList){
    const n = Math.min(hoursList.length, biliList.length);
    const pts = [];
    for(let i=0;i<n;i++) pts.push({ x: hoursList[i], y: biliList[i] });
    return pts.sort((a,b)=>a.x-b.x);
  }

  // Data persistence functions
  function saveFormData(){
    try {
      const formData = {
        ga: $('#ga').value,
        ageHours: $('#ageHours').value,
        bili: $('#bili').value,
        risk: ($('input[name="risk"]:checked')||{}).value,
        dob: $('#dob').value,
        dom: $('#dom').value,
        datasetKind: ($('#datasetKind')||{}).value,
        verifyInput: ($('#verifyInput')||{}).value
      };
      localStorage.setItem('pedBiliFormData', JSON.stringify(formData));
    } catch(e) {
      // Ignore localStorage errors (e.g., quota exceeded, private browsing)
    }
  }

  function loadFormData(){
    try {
      const saved = localStorage.getItem('pedBiliFormData');
      if(!saved) return;
      
      const formData = JSON.parse(saved);
      
      // Restore form values
      if(formData.ga) $('#ga').value = formData.ga;
      if(formData.ageHours) $('#ageHours').value = formData.ageHours;
      if(formData.bili) $('#bili').value = formData.bili;
      if(formData.risk) {
        const riskRadio = $(`input[name="risk"][value="${formData.risk}"]`);
        if(riskRadio) riskRadio.checked = true;
      }
      if(formData.dob) $('#dob').value = formData.dob;
      if(formData.dom) $('#dom').value = formData.dom;
      if(formData.datasetKind && $('#datasetKind')) $('#datasetKind').value = formData.datasetKind;
      if(formData.verifyInput && $('#verifyInput')) $('#verifyInput').value = formData.verifyInput;
      
    } catch(e) {
      // Ignore errors in loading/parsing saved data
    }
  }

  function buildDatasets(){
    const ga = Number($('#ga').value);
    const ageHoursList = parseList($('#ageHours').value);
    const biliList = parseList($('#bili').value);
    const risk = ($('input[name="risk"]:checked')||{}).value || 'no_risk';

    const sets = DemoThresholds.getCurves(ga, risk);
    const datasets = [];

    const palette = {
      no_risk: { pt: '#22c55e', ex: '#f59e0b' },
      any_risk: { pt: '#16a34a', ex: '#d97706' }
    };

    if(sets.no_risk){
      datasets.push({ label: 'Phototherapy (no risk, demo)', data: sets.no_risk.phototherapy, borderColor: palette.no_risk.pt, pointRadius: 0, tension: .3 });
      datasets.push({ label: 'Exchange (no risk, demo)', data: sets.no_risk.exchange, borderColor: palette.no_risk.ex, pointRadius: 0, tension: .3 });
    }
    if(sets.any_risk){
      datasets.push({ label: 'Phototherapy (any risk, demo)', data: sets.any_risk.phototherapy, borderColor: palette.any_risk.pt, pointRadius: 0, tension: .3 });
      datasets.push({ label: 'Exchange (any risk, demo)', data: sets.any_risk.exchange, borderColor: palette.any_risk.ex, pointRadius: 0, tension: .3 });
    }

    // AAP exchange table overlay when risk includes any_risk or both
    if(risk !== 'no_risk' && window.AAP_AnyRisk_Exchange){
      const exAAP = window.AAP_AnyRisk_Exchange.tables[ga] || window.AAP_AnyRisk_Exchange.tables[38];
      datasets.push({ label: 'Exchange (AAP any risk)', data: exAAP, borderColor: '#ef4444', pointRadius: 0, tension: 0 });
    }

    // AAP phototherapy overlay (any risk) if dataset present
    if(risk !== 'no_risk' && window.AAP_AnyRisk_Phototherapy){
      const ptAAP = window.AAP_AnyRisk_Phototherapy.tables[ga] || window.AAP_AnyRisk_Phototherapy.tables[38] || [];
      if(ptAAP.length){
        datasets.push({ label: 'Phototherapy (AAP any risk)', data: ptAAP, borderColor: '#22c55e', pointRadius: 0, tension: 0, borderDash: [6,4] });
      }
    }

    // Plot user provided points
    if(ageHoursList.length && biliList.length){
      const points = zipToPoints(ageHoursList, biliList);
      datasets.push({ label: 'Patient bilirubin', data: points, showLine: false, borderColor: '#0ea5e9', backgroundColor: '#0ea5e9', pointRadius: 4 });
    }

  return { datasets, ga, risk, ageHoursList, biliList };
  }

  // Chart removed; keep datasets builder for parsing only.

  function computeSummary(){
    const { ga, risk, ageHoursList, biliList } = buildDatasets();
    const age = ageHoursList[ageHoursList.length-1];
    const bili = biliList[biliList.length-1];

  let rec = DemoThresholds.recommendation({ age, bili, ga, risk });
  const hasBili = (typeof bili === 'number' && isFinite(bili));

  // If any-risk selected, show AAP exchange for that GA as authoritative overlay
  let aapEx = null;
  let aapExactText = 'hi';
    if(risk !== 'no_risk' && window.AAP_AnyRisk_Exchange){
      if(typeof age === 'number' && !isNaN(age)){
        const exact = window.AAP_AnyRisk_Exchange.getExchangeExact(ga, age);
        aapEx = Number(exact.value.toFixed(1));
      }
      // If user entered multiple ages, show the exact table value for each
      if(ageHoursList.length){
        const rows = ageHoursList.map((h, i) => {
          const ex = window.AAP_AnyRisk_Exchange.getExchangeExact(ga, h);
          const pt = (window.AAP_AnyRisk_Phototherapy && window.AAP_AnyRisk_Phototherapy.getPhotoExact) ? window.AAP_AnyRisk_Phototherapy.getPhotoExact(ga, h) : null;
          const ptV = pt && typeof pt.value === 'number' ? Number(pt.value.toFixed(1)) : null;
          const exV = Number(ex.value.toFixed(1));
          const both = (ptV!=null) ? `(${ptV}, ${exV})` : `${exV}`;
          const tbili = (typeof biliList[i] === 'number' && !isNaN(biliList[i])) ? biliList[i] : null;
          const cmp = tbili!=null ? (tbili >= ex.value ? '≥' : '<') : '';
          const tbiliTxt = tbili!=null ? ` · TSB ${tbili} (${cmp} ${exV})` : '';
          return `h${ex.hour}: ${both}${tbiliTxt}`;
        });

      }
    }

    // Phototherapy exact, if table present
  let aapPt = null;
    if(risk !== 'no_risk' && window.AAP_AnyRisk_Phototherapy){
      if(typeof age === 'number' && !isNaN(age)){
        const exactPt = window.AAP_AnyRisk_Phototherapy.getPhotoExact(ga, age);
        if(exactPt && typeof exactPt.value === 'number') aapPt = Number(exactPt.value.toFixed(1));
      }
    }

  // Build primary threshold header
  let headerHtml = '';
  if(aapPt != null && aapEx != null){
    headerHtml = `<div class="primary-threshold">(${aapPt}, ${aapEx}) mg/dL</div>`;
  } else if(aapPt != null){
    headerHtml = `<div class="primary-threshold">${aapPt} mg/dL</div>`;
  } else if(aapEx != null){
    headerHtml = `<div class="primary-threshold">${aapEx} mg/dL</div>`;
  }

  // Meta line under header
  const metaParts = [];
  metaParts.push(`<strong>GA:</strong> ${ga} wks`);
  if(typeof age !== 'undefined') metaParts.push(`<strong>Age:</strong> ${age} h`);
  const riskText = risk.replace('_',' ');
  metaParts.push(`<span class="muted"><strong>Risk:</strong> ${riskText}</span>`);
  const metaHtml = `<div class="secondary-meta">${metaParts.join(' · ')}</div>`;

  const details = [];
  if(hasBili) details.push(`<div><strong>Patient TSB:</strong> ${bili} mg/dL</div>`);
  // Prefer AAP exchange determination if applicable
    if(aapEx != null && hasBili){
      if(bili >= aapEx){
        rec = { level: 'Exchange threshold or higher (AAP any risk)', detail: rec.detail, pt: rec.pt, ex: aapEx };
      }
    }
    if(hasBili){
      details.push(`<div><strong>Assessment:</strong> ${rec.level}</div>`);
      if(rec.pt) details.push(`<div class="small muted">Demo phototherapy ~ ${rec.pt} mg/dL; demo exchange ~ ${rec.ex} mg/dL</div>`);
    }


    $('#summary').innerHTML = [headerHtml, metaHtml, ...details].filter(Boolean).join('');
  }

  // PDF export removed

  function calcAge(){
    const dob = $('#dob').value; // datetime-local
    const dom = $('#dom').value;
    if(!dob || !dom) return;
    const t0 = new Date(dob).getTime();
    const t1 = new Date(dom).getTime();
    if(isNaN(t0) || isNaN(t1) || t1 <= t0) return;
    const hours = Math.round((t1 - t0)/36e5);
    $('#ageHours').value = String(hours);
  }

  // Verification helpers
  function parseTableNumbers(text){
    if(!text) return [];
    // extract all numbers (allow decimals)
    const arr = (text.match(/[-+]?[0-9]*\.?[0-9]+/g) || []).map(Number).filter(n => Number.isFinite(n));
    return arr;
  }

  function isHeader024(nums){
    if(nums.length !== 24) return false;
    for(let i=0;i<24;i++){ if(nums[i] !== i) return false; }
    return true;
  }

  function parseAAPDayRows(text){
    // Parses lines like: "0 13.1 13.3 ... 16.0" (25 numbers) and ignores the leading day index.
    // Also ignores a header line "0 1 2 ... 23".
  const lines = text.split(/\r?\n/);
  const rows = []; for(var i=0;i<15;i++){ rows[i] = null; }
    for(const line of lines){
      const nums = (line.match(/[-+]?[0-9]*\.?[0-9]+/g) || []).map(Number).filter(n => Number.isFinite(n));
      if(!nums.length) continue;
      if(isHeader024(nums)) continue; // skip hour header
      // day-labeled rows: 25 numbers, first is 0..14
      if(nums.length === 25 && Number.isInteger(nums[0]) && nums[0] >= 0 && nums[0] <= 14){
        const day = nums[0];
        const vals = nums.slice(1);
        if(vals.length === 24) rows[day] = vals;
      } else if(nums.length === 24){
        // Some tables omit the day label; fill the first available day slot
        var day = -1; for(var di=0; di<rows.length; di++){ if(rows[di] === null){ day = di; break; } }
        if(day !== -1) rows[day] = nums;
      }
    }
    // Flatten if all rows present
    var ok = true; for(var ri=0; ri<rows.length; ri++){ if(!Array.isArray(rows[ri]) || rows[ri].length !== 24){ ok = false; break; } }
    if(ok){
      var out = [];
      for(var rr=0; rr<rows.length; rr++){
        for(var hh=0; hh<rows[rr].length; hh++) out.push(rows[rr][hh]);
      }
      return out;
    }
    return [];
  }

  function convertToJsRows(){
    const raw = $('#verifyInput').value;
  const lines = raw.split(/\r?\n/);
  const rows = []; for(var i=0;i<15;i++){ rows[i] = null; }
    for(const line of lines){
      const nums = (line.match(/[-+]?[0-9]*\.?[0-9]+/g) || []).map(Number).filter(n => Number.isFinite(n));
      if(!nums.length) continue;
      if(isHeader024(nums)) continue;
      if(nums.length === 25 && Number.isInteger(nums[0]) && nums[0] >= 0 && nums[0] <= 14){
        const day = nums[0];
        const vals = nums.slice(1);
        if(vals.length === 24) rows[day] = vals;
      } else if(nums.length === 24){
  var day = -1; for(var di=0; di<rows.length; di++){ if(rows[di] === null){ day = di; break; } }
        if(day !== -1) rows[day] = nums;
      }
    }
    // Validate provided rows have 24 values
    for(let d=0; d<15; d++){
      if(rows[d] && rows[d].length !== 24){
        $('#convertOutput').value = `Row for day ${d} has ${rows[d].length} values; expected 24.`;
        return;
      }
    }
    // Determine last filled day and plateau value
    let lastFilled = -1;
  for(let d=14; d>=0; d--){ if(rows[d]) { lastFilled = d; break; } }
    if(lastFilled === -1){
      $('#convertOutput').value = 'No rows detected. Paste day-labeled rows or lines of 24 values.';
      return;
    }
    const plateau = Number((rows[lastFilled][23] != null ? rows[lastFilled][23] : rows[lastFilled][rows[lastFilled].length-1]).toFixed(1));
    // Auto-fill missing days with a flat plateau row up to day 14
    for(let d=0; d<15; d++){
      if(!rows[d]) rows[d] = Array(24).fill(plateau);
    }
    // format
    const js = rows.map(r => `[${r.map(v => Number(v.toFixed(1))).join(',')}]`).join(',\n    ');
    $('#convertOutput').value = js;
  }

  async function copyConverted(){
    const txt = $('#convertOutput').value || '';
    if(!txt) return;
    try{
      await navigator.clipboard.writeText(txt);
    }catch(e){
      // ignore
    }
  }

  function getInternalHourlyForGA(ga){
    const kind = ($('#datasetKind') && $('#datasetKind').value) || 'exchange';
    let arr = [];
    if(kind === 'phototherapy'){
      arr = (window.AAP_AnyRisk_Phototherapy && window.AAP_AnyRisk_Phototherapy.tables[ga]) || [];
    } else {
      arr = (window.AAP_AnyRisk_Exchange && window.AAP_AnyRisk_Exchange.tables[ga]) || [];
    }
    return arr.map(p => p.y);
  }

  function verifyAgainstDataset(){
    const ga = Number($('#ga').value);
    const raw = $('#verifyInput').value;
    let nums = parseAAPDayRows(raw);
    let parsedMode = 'day-rows';
    if(nums.length === 0){
      nums = parseTableNumbers(raw);
      parsedMode = 'flat';
    }
    const expected = getInternalHourlyForGA(ga);
    const out = [];
    if(expected.length === 0){
      $('#verifyOutput').innerHTML = 'No dataset found for this GA.';
      return;
    }
    if(nums.length !== expected.length){
      out.push(`Count mismatch: pasted ${nums.length} numbers; dataset has ${expected.length}.`);
    }
    const n = Math.min(nums.length, expected.length);
    let mismatches = 0;
    for(let i=0;i<n;i++){
      const a = Number(nums[i].toFixed(3));
      const b = Number(expected[i].toFixed(3));
      if(a !== b){
        mismatches++;
        const day = Math.floor(i/24), hour = i % 24;
        if(mismatches <= 25){
          out.push(`Mismatch at day ${day}, hour ${hour}: pasted ${a} vs dataset ${b}`);
        }
      }
    }
    if(mismatches === 0 && nums.length === expected.length){
      out.unshift(`All values match the JS dataset for this GA. (Parsed as ${parsedMode})`);
    } else {
      out.unshift(`Found ${mismatches} mismatches.`);
      if(mismatches > 25) out.push(`…and ${mismatches-25} more.`);
    }
    $('#verifyOutput').innerHTML = out.join('<br/>');
  }

  function exportDatasetJSON(){
    const ga = Number($('#ga').value);
    const kind = ($('#datasetKind') && $('#datasetKind').value) || 'exchange';
    const table = kind === 'phototherapy'
      ? ((window.AAP_AnyRisk_Phototherapy && window.AAP_AnyRisk_Phototherapy.tables[ga]) || [])
      : ((window.AAP_AnyRisk_Exchange && window.AAP_AnyRisk_Exchange.tables[ga]) || []);
    const obj = table.map(p => ({ hour: p.x, value: p.y }));
    const blob = new Blob([JSON.stringify(obj, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = (kind === 'phototherapy') ? `aap_anyrisk_phototherapy_ga${ga}.json` : `aap_anyrisk_exchange_ga${ga}.json`;
    a.click();
    setTimeout(()=>URL.revokeObjectURL(url), 5000);
  }

  // Events
  // Live updates on change
  $('#resetBtn').addEventListener('click', () => { 
    setTimeout(()=>{ 
      $('#summary').textContent=''; 
      // Clear saved data when reset is clicked
      try { localStorage.removeItem('pedBiliFormData'); } catch(e) {}
      computeSummary(); 
    }, 0); 
  });
  $('#calcAgeBtn').addEventListener('click', () => { calcAge(); computeSummary(); saveFormData(); });
  
  // Recompute on every keystroke as well as change, and save data
  $$('#controls input').forEach(el => el.addEventListener('input', ()=>{ computeSummary(); saveFormData(); }));
  $$('#controls input, #controls select').forEach(el => el.addEventListener('change', ()=>{ computeSummary(); saveFormData(); }));
  
  // Also save data when age calculator fields change
  if($('#dob')) $('#dob').addEventListener('change', saveFormData);
  if($('#dom')) $('#dom').addEventListener('change', saveFormData);
  if($('#datasetKind')) $('#datasetKind').addEventListener('change', saveFormData);
  if($('#verifyInput')) $('#verifyInput').addEventListener('input', saveFormData);
  
  $('#verifyBtn').addEventListener('click', verifyAgainstDataset);
  $('#exportBtn').addEventListener('click', exportDatasetJSON);
  $('#convertBtn').addEventListener('click', convertToJsRows);
  $('#copyConvertBtn').addEventListener('click', copyConverted);

  // Load saved data and initial render
  loadFormData();
  computeSummary();
})();


