/* Main app logic: parses inputs, renders Chart.js curves, overlays values, shows recommendations, PDF export */
(function(){
  const $ = sel => document.querySelector(sel);
  const $$ = sel => Array.from(document.querySelectorAll(sel));

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

  function buildDatasets(){
    const ga = Number($('#ga').value);
    const ageHoursList = parseList($('#ageHours').value);
    const biliList = parseList($('#bili').value);
    const risk = ($('input[name="risk"]:checked')||{}).value || 'no_risk';
    const scale = ($('input[name="scale"]:checked')||{}).value || 'auto';

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

    // Plot user provided points
    if(ageHoursList.length && biliList.length){
      const points = zipToPoints(ageHoursList, biliList);
      datasets.push({ label: 'Patient bilirubin', data: points, showLine: false, borderColor: '#0ea5e9', backgroundColor: '#0ea5e9', pointRadius: 4 });
    }

    const xMax = scale === 'full' ? DemoThresholds.HOURS_MAX : Math.max(72, ...datasets.flatMap(d => (d.data||[])).map(p => p.x||0), ...(ageHoursList||[]));
    const yMax = scale === 'full' ? 24 : Math.max(12, ...datasets.flatMap(d => (d.data||[])).map(p => p.y||0), ...(biliList||[]));

    return { datasets, xMax, yMax, ga, risk, ageHoursList, biliList };
  }

  function drawChart(){
    const {datasets, xMax, yMax} = buildDatasets();
    const data = { datasets };
    const opt = {
      responsive: true,
      animation: false,
      scales: {
        x: { type: 'linear', title: { display: true, text: 'Age (hours)' }, min: 0, max: xMax },
        y: { title: { display: true, text: 'TSB (mg/dL)' }, min: 0, max: yMax }
      },
      plugins: { legend: { position: 'top' }, tooltip: { intersect: false, mode: 'index' } }
    };
    if(chart) chart.destroy();
    chart = new Chart(ctx, { type: 'line', data, options: opt });
  }

  function computeSummary(){
    const { ga, risk, ageHoursList, biliList } = buildDatasets();
    const age = ageHoursList[ageHoursList.length-1];
    const bili = biliList[biliList.length-1];

  let rec = DemoThresholds.recommendation({ age, bili, ga, risk });

    // If any-risk selected, show AAP exchange for that GA as authoritative overlay
    let aapEx = null;
    let aapExactText = '';
    if(risk !== 'no_risk' && window.AAP_AnyRisk_Exchange){
      if(typeof age === 'number' && !isNaN(age)){
        const exact = window.AAP_AnyRisk_Exchange.getExchangeExact(ga, age);
        aapEx = Number(exact.value.toFixed(1));
      }
      // If user entered multiple ages, show the exact table value for each
      if(ageHoursList.length){
        const rows = ageHoursList.map((h, i) => {
          const ex = window.AAP_AnyRisk_Exchange.getExchangeExact(ga, h);
          const tbili = (typeof biliList[i] === 'number' && !isNaN(biliList[i])) ? biliList[i] : null;
          const cmp = tbili!=null ? (tbili >= ex.value ? '≥' : '<') : '';
          const tbiliTxt = tbili!=null ? ` · TSB ${tbili} (${cmp} ${ex.value})` : '';
          return `h${ex.hour}: ${ex.value}${tbiliTxt}`;
        });
        aapExactText = `AAP any-risk exchange values: ${rows.join('; ')}`;
      }
    }

  const parts = [];
  parts.push(`<strong>GA:</strong> ${ga} wks`);
  if(typeof age !== 'undefined') parts.push(`<strong>Age:</strong> ${age} h`);
  if(typeof bili !== 'undefined') parts.push(`<strong>TSB:</strong> ${bili} mg/dL`);
  parts.push(`<strong>Risk:</strong> ${risk.replace('_',' ')}`);

  const lines = [];
  if(aapEx != null) lines.push(`AAP any-risk exchange at this age/GA (exact table hour): <strong>${aapEx} mg/dL</strong>`);
  if(aapExactText) lines.push(aapExactText);
  lines.push(parts.join(' · '));
    // Prefer AAP exchange determination if applicable
    if(aapEx != null && typeof bili === 'number' && !isNaN(bili)){
      if(bili >= aapEx){
        rec = { level: 'Exchange threshold or higher (AAP any risk)', detail: rec.detail, pt: rec.pt, ex: aapEx };
      }
    }
    lines.push(`<strong>Assessment:</strong> ${rec.level}`);
    if(rec.pt) lines.push(`Demo phototherapy threshold ~ ${rec.pt} mg/dL; demo exchange ~ ${rec.ex} mg/dL`);
  // AAP lines already placed at top

    $('#summary').innerHTML = lines.join('<br/>');
  }

  function toPDF(){
    const { jsPDF } = window.jspdf;
    const doc = new jsPDF({ unit: 'pt', format: 'letter' });

    doc.setFontSize(14);
    doc.text('Age & Bilirubin (Demo)', 40, 40);

    // Add summary
    const summary = $('#summary').innerText || '';
    const wrapped = doc.splitTextToSize(summary, 520);
    doc.setFontSize(10);
    doc.text(wrapped, 40, 60);

    // Add chart canvas
    const canvas = ctx;
    const dataUrl = canvas.toDataURL('image/png', 1.0);
    doc.addImage(dataUrl, 'PNG', 40, 120, 520, 300);

    doc.setFontSize(8);
    doc.text('For education only — not for clinical use. Curves are demo; AAP exchange (any risk) overlay when selected.', 40, 440);

    doc.save('age-bilirubin-demo.pdf');
  }

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
    const rows = Array.from({length: 15}, () => null);
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
        const day = rows.findIndex(r => r === null);
        if(day !== -1) rows[day] = nums;
      }
    }
    // Flatten if all rows present
    if(rows.every(r => Array.isArray(r) && r.length === 24)){
      return rows.flat();
    }
    return [];
  }

  function getInternalHourlyForGA(ga){
    const arr = (window.AAP_AnyRisk_Exchange && window.AAP_AnyRisk_Exchange.tables[ga]) || [];
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
    const table = (window.AAP_AnyRisk_Exchange && window.AAP_AnyRisk_Exchange.tables[ga]) || [];
    const obj = table.map(p => ({ hour: p.x, value: p.y }));
    const blob = new Blob([JSON.stringify(obj, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `aap_anyrisk_exchange_ga${ga}.json`;
    a.click();
    setTimeout(()=>URL.revokeObjectURL(url), 5000);
  }

  // Events
  $('#plotBtn').addEventListener('click', () => { drawChart(); computeSummary(); });
  $('#resetBtn').addEventListener('click', () => { setTimeout(()=>{ drawChart(); $('#summary').textContent=''; }, 0); });
  $('#pdfBtn').addEventListener('click', toPDF);
  $('#calcAgeBtn').addEventListener('click', () => { calcAge(); drawChart(); computeSummary(); });
  $$('#controls input, #controls select').forEach(el => el.addEventListener('change', ()=>{ drawChart(); computeSummary(); }));
  $('#verifyBtn').addEventListener('click', verifyAgainstDataset);
  $('#exportBtn').addEventListener('click', exportDatasetJSON);

  // Initial render
  drawChart();
  computeSummary();
})();
