// class TabButtons extends HTMLElement {
//     connectedCallback() {
//         this.innerHTML = `
//       <div class="tab-buttons">
// 	<button class="tab-button active" onclick="switchTab('ga')">GA Calculator</button>
// 		<button class="tab-button" onclick="switchTab('average-calculator')">Average Calculator</button>
// 		<button class="tab-button" onclick="switchTab('settings')">Settings</button>
// 		<button class="tab-button" onclick="switchTab('bili')">Bili</button>
//       </div>
//     `;
//         this.querySelectorAll('.tab-button').forEach(btn => {
//             btn.addEventListener('click', () => {
//                 this.querySelectorAll('.tab-button').forEach(b => b.classList.remove('active'));
//                 btn.classList.add('active');
//                 this.dispatchEvent(new CustomEvent('tab-switch', {
//                     detail: btn.dataset.tab,
//                     bubbles: true
//                 }));
//             });
//         });
//     }
// }
// customElements.define('tab-buttons', TabButtons);

// // Tab switching functionality - Navigate to unique URLs
// function switchTab(tabId) {
//     if (tabId === 'ga') {
//         window.location.href = '../ga/ga.html';
//     } else if (tabId === 'average-calculator') {
//         window.location.href = '../avg/avg.html';
//     } else if (tabId === 'settings') {
//         window.location.href = '../settings/settings.html';
//     } else if (tabId === 'bili') {
//         window.location.href = '../bili/index.html';
//     }

//     // Only navigate if not already on the target page
//     if (!window.location.pathname.endsWith(page)) {
//         window.location.href = base + page;
//     }
// }




class TabButtons extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
      <div class="tab-buttons">
        <button class="tab-button" data-tab="ga">GA Calculator</button>
        <button class="tab-button" data-tab="average-calculator">Average Calculator</button>
        <button class="tab-button" data-tab="settings">Settings</button>
        <button class="tab-button" data-tab="bili">Bili</button>
      </div>
    `;

        // Highlight the correct tab based on current page
        const path = window.location.pathname;
        let activeTab = null;

        if (path.includes("/ga/")) {
            activeTab = "ga";
        } else if (path.includes("/avg/")) {
            activeTab = "average-calculator";
        } else if (path.includes("/settings/")) {
            activeTab = "settings";
        } else if (path.includes("/bili/")) {
            activeTab = "bili";
        }

        this.querySelectorAll('.tab-button').forEach(btn => {
            if (btn.dataset.tab === activeTab) {
                btn.classList.add('active');
            }

            btn.addEventListener('click', () => {
                this.querySelectorAll('.tab-button').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                switchTab(btn.dataset.tab);
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
        window.location.href = '../bili/bili.html';
    }
}
