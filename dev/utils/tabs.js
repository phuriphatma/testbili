class TabButtons extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
      <div class="tab-buttons">
	<button class="tab-button active" onclick="switchTab('ga')">GA Calculator</button>
		<button class="tab-button" onclick="switchTab('average-calculator')">Average Calculator</button>
		<button class="tab-button" onclick="switchTab('settings')">Settings</button>
		<button class="tab-button" onclick="switchTab('bili')">Bili</button>
      </div>
    `;
        this.querySelectorAll('.tab-button').forEach(btn => {
            btn.addEventListener('click', () => {
                this.querySelectorAll('.tab-button').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.dispatchEvent(new CustomEvent('tab-switch', {
                    detail: btn.dataset.tab,
                    bubbles: true
                }));
            });
        });
    }
}
customElements.define('tab-buttons', TabButtons);

// Tab switching functionality - Navigate to unique URLs
function switchTab(tabId) {
    if (tabId === 'ga') {
        window.location.href = '../ga/ga.html';
    } else if (tabId === 'average-calculator') {
        window.location.href = '../avg/avg.html';
    } else if (tabId === 'settings') {
        window.location.href = '../settings/settings.html';
    } else if (tabId === 'bili') {
        window.location.href = '../index.html';
    }

    // Only navigate if not already on the target page
    if (!window.location.pathname.endsWith(page)) {
        window.location.href = base + page;
    }
}